package bill.chat.rabbitMQ;

import static bill.chat.model.enums.MessageType.IMAGE;
import static bill.chat.model.enums.SystemType.USER_LEFT;

import bill.chat.converter.ChatMessageConverter;
import bill.chat.dto.ChatDTO;
import bill.chat.dto.PushDTO;
import bill.chat.dto.SSEDTO;
import bill.chat.model.ChatMessage;
import bill.chat.model.ChatRoom;
import bill.chat.repository.ChatMessageRepository;
import bill.chat.repository.ChatRoomRepository;
import bill.chat.service.DistributedSSEManager;
import bill.chat.service.DistributedSessionManager;
import bill.chat.websocket.payload.dto.WebSocketSuccessDTO;
import bill.chat.websocket.payload.handler.WebSocketSuccessConverter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatProcessor {

    private final Producer mqProducer; // Producer를 주입받습니다.
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final DistributedSessionManager distributedSessionManager;
    private final WebSocketSuccessConverter webSocketSuccessConverter;
    private final DistributedSSEManager distributedSSEManager;

    @RabbitListener(queues = "chat.processing.queue")
    public void processAndSaveChatMessage(ChatDTO chatDTO) {
        String channelId = chatDTO.getChannelId();
        try {
            chatRoomRepository.findByChannelId(channelId)
                    .flatMap(chatRoom -> distributedSessionManager.getActiveUserCount(channelId)
                            .flatMap(activeUserCount -> {
                                boolean isRead = activeUserCount >= 2;
                                if (!isRead) {
                                    chatRoom.addUnreadCount();
                                }

                                String lastContent = Optional.ofNullable(chatDTO.getContent()).orElse("");
                                if (chatDTO.getMessageType() == IMAGE) {
                                    lastContent = "사진";
                                }

                                if (chatDTO.getSystemType() == USER_LEFT) {
                                    chatRoom.updateLastMessage("상대방이 채팅방을 나갔습니다.");
                                } else {
                                    chatRoom.updateSender(chatDTO.getSenderId());
                                    chatRoom.updateLastMessage(lastContent);
                                }

                                ChatMessage chatMessage = ChatMessageConverter.toChatMessage(
                                        channelId,
                                        chatDTO.getSenderId(),
                                        chatDTO.getContent(),
                                        chatDTO.getSystemType(),
                                        chatDTO.getMessageType(),
                                        isRead,
                                        chatDTO.getStartedAt(),
                                        chatDTO.getEndedAt(),
                                        chatDTO.getPrice());
                                // DB 저장 로직
                                return chatRoomRepository.save(chatRoom)
                                        .then(chatMessageRepository.save(chatMessage))
                                        .flatMap(savedMessage -> {
                                            WebSocketSuccessDTO successDTO = webSocketSuccessConverter.toSuccessDTO(
                                                    chatDTO,
                                                    savedMessage.getCreatedAt(),
                                                    savedMessage.isRead());

                                            return dispatchWebSocketMessage(chatRoom.getChannelId(), successDTO)
                                                    .then(sendNotificationsToOtherUsers(chatRoom, chatDTO,
                                                            savedMessage));
                                        });
                            }))
                    .block(java.time.Duration.ofSeconds(5)); // 5초 타임아웃: DB hang 시 무한 대기 방지 → TimeoutException → NACK →
                                                             // 재시도
        } catch (Exception e) {
            log.error("메시지 처리/저장 중 오류 발생 - NACK 처리됨: channelId={}", channelId, e);
            throw new RuntimeException("채팅 메시지 처리 실패", e); // RabbitMQ NACK → 재시도
        }
    }

    private Mono<Void> sendNotificationsToOtherUsers(ChatRoom chatRoom, ChatDTO chatDTO, ChatMessage savedMessage) {
        String senderId = chatDTO.getSenderId();
        String channelId = chatRoom.getChannelId();

        return Flux.fromIterable(chatRoom.getParticipants())
                .filter(participant -> !participant.getUserId().equals(senderId))
                .flatMap(participant -> {
                    String targetUserId = participant.getUserId();

                    return distributedSessionManager.isUserConnectedToChannel(targetUserId, channelId)
                            .flatMap(isConnectedToChannel -> {
                                if (isConnectedToChannel) {
                                    log.info("Target user {} is in channel {}. Skipping notifications.", targetUserId,
                                            channelId);
                                    return Mono.empty();
                                }

                                return distributedSSEManager.findConnectedServerId(targetUserId)
                                        .flatMap(serverId -> {
                                            log.info("Target user {} is online via SSE. Sending SSE message to server {}.",
                                                    targetUserId, serverId);
                                            return mqProducer.sendSSEMessageToServer(serverId, SSEDTO.builder()
                                                    .targetUserId(targetUserId)
                                                    .channelId(chatRoom.getChannelId())
                                                    .senderId(chatDTO.getSenderId())
                                                    .content(savedMessage.getContent())
                                                    .unreadCount(chatRoom.getUnreadCount())
                                                    .notification(participant.isNotification())
                                                    .updatedAt(chatRoom.getUpdatedAt())
                                                    .build());
                                        })
                                        .switchIfEmpty(Mono.defer(() -> {
                                            if (participant.isNotification()) {
                                                log.info("Target user {} is offline. Sending Push message.",
                                                        targetUserId);
                                                return mqProducer.sendPushMessage(PushDTO.builder()
                                                        .userId(targetUserId)
                                                        .senderId(chatDTO.getSenderId())
                                                        .channelId(chatRoom.getChannelId())
                                                        .lastContent(savedMessage.getContent())
                                                        .build());
                                            }
                                            return Mono.empty();
                                        }));
                            });
                }).then();
    }

    private Mono<Void> dispatchWebSocketMessage(String channelId, WebSocketSuccessDTO successDTO) {
        return distributedSessionManager.getActiveServerIds(channelId)
                .flatMapMany(Flux::fromIterable)
                .flatMap(serverId -> mqProducer.sendWebSocketMessageToServer(serverId, successDTO))
                .then();
    }
}
