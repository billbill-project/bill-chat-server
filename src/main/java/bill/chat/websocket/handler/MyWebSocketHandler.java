package bill.chat.websocket.handler;

import static bill.chat.model.enums.MessageType.IMAGE;
import static bill.chat.model.enums.MessageType.SYSTEM;
import static bill.chat.model.enums.SystemType.RESERVATION_REQUEST;
import static bill.chat.model.enums.SystemType.USER_LEFT;

import bill.chat.converter.ChatMessageConverter;
import bill.chat.converter.SSEConverter;
import bill.chat.dto.SSEDTO;
import bill.chat.model.ChatMessage;
import bill.chat.dto.ChatDTO;
import bill.chat.model.ChatRoom;
import bill.chat.model.Participant;
import bill.chat.model.enums.SystemType;
import bill.chat.repository.ChatMessageRepository;
import bill.chat.repository.ChatRoomRepository;
import bill.chat.service.ChatService;
import bill.chat.service.SSEManager;
import bill.chat.websocket.payload.dto.WebSocketSuccessDTO;
import bill.chat.websocket.payload.handler.WebSocketResponseHandler;
import bill.chat.websocket.payload.handler.WebSocketSuccessConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class MyWebSocketHandler implements WebSocketHandler {
    private final Map<String, List<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final WebSocketResponseHandler responseHandler;
    private final ObjectMapper objectMapper;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SSEManager sseManager;
    private final ChatService chatService;

    @Override
    public List<String> getSubProtocols() {
        return WebSocketHandler.super.getSubProtocols();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String channelId = getChannelId(session);
        String userId = (String) session.getAttributes().get("userId");

        log.info("WebSocket 연결 시작: userId={}, channelId={}, sessionId={}", userId, channelId, session.getId());

        return Mono.defer(() -> {
                    sessions.computeIfAbsent(channelId, id -> new CopyOnWriteArrayList<>()).add(session);

                    return processRead(channelId, userId)
                            .then(session.receive()
                                    .flatMap(message -> handleMessage(session, channelId, message.getPayloadAsText()))
                                    .then())
                            .doFinally(signalType -> {
                                log.info("WebSocket 연결 종료: {}, 종료 원인: {}", session.getId(), signalType);
                                sessions.getOrDefault(channelId, List.of()).remove(session);
                            });
                })
                .onErrorResume(e -> session.close(new CloseStatus(4999, "UNKNOWN_ERROR")));
    }

    private Mono<Void> processRead(String channelId, String userId) {
        Mono<Long> unreadMessagesCount = chatMessageRepository.findUnreadMessages(channelId, userId)
                .flatMap(chatMessage -> {
                    chatMessage.changeRead();
                    return chatMessageRepository.save(chatMessage);
                })
                .count();

        return unreadMessagesCount.flatMap(count -> {
            if (count > 0) {
                return chatRoomRepository.findByChannelId(channelId)
                        .flatMap(chatRoom -> {
                            chatRoom.resetUnreadCount();
                            return chatRoomRepository.save(chatRoom).then();
                        });
            }
            return Mono.empty();
        });
    }

    private Mono<Void> handleMessage(WebSocketSession session, String channelId, String payload) {
        try {
            ChatDTO chatDTO = parseChatMessage(payload);
            return processChatMessage(channelId, chatDTO);
        } catch (JsonProcessingException e) {
            return session.close(new CloseStatus(4005, "MESSAGE_TYPE_ERROR"));
        }
    }

    private Mono<Void> processChatMessage(String channelId, ChatDTO chatDTO) {
        return chatRoomRepository.findByChannelId(channelId)
                .doOnNext(chatRoom -> log.info("채팅방 조회 성공: {}", channelId))

                .flatMap(chatRoom -> {
                    log.info("메시지 처리 시작...");
                    boolean isRead = true;
                    SystemType systemType = null;
                    LocalDate startedAt = null;
                    LocalDate endedAt = null;
                    Integer price = null;
                    String lastContent = chatDTO.getContent();

                    isRead = updateToUnread(channelId, chatRoom, isRead);
                    if (chatDTO.getMessageType() == IMAGE) {
                        lastContent = "사진";
                    }
                    if (chatDTO.getMessageType() == SYSTEM) {
                        systemType = chatDTO.getSystemType();
                        if (systemType.equals(RESERVATION_REQUEST)) {
                            startedAt = chatDTO.getStartedAt();
                            endedAt = chatDTO.getEndedAt();
                            price = chatDTO.getPrice();
                        }

                        if (systemType.equals(USER_LEFT)) {
                            WebSocketSuccessDTO successDTO = new WebSocketSuccessConverter().toSuccessDTO(
                                    chatDTO,
                                    LocalDateTime.now(),
                                    isRead
                            );
                            return broadcastMessage(channelId, successDTO);
                        }
                    }
                    chatRoom.updateSender(chatDTO.getSenderId());
                    chatRoom.updateLastMessage(lastContent);
                    String senderId = chatDTO.getSenderId();
                    ChatMessage chatMessage = ChatMessageConverter.toChatMessage(channelId, senderId,
                            chatDTO.getContent(), systemType,
                            chatDTO.getMessageType(), isRead, startedAt, endedAt, price);

                    return saveChatRoomProcess(channelId, chatDTO, chatRoom, senderId, isRead, lastContent,
                            chatMessage);
                });
    }

    private boolean updateToUnread(String channelId, ChatRoom chatRoom, boolean isRead) {
        // 두 명 이상이 session 가지면 읽은 걸로 간주
        long count = sessions.get(channelId).stream()
                .filter(WebSocketSession::isOpen)
                .count();
        if (count < 2) {
            isRead = false;
            chatRoom.addUnreadCount();
        }
        return isRead;
    }

    private Mono<Void> saveChatRoomProcess(String channelId, ChatDTO chatDTO, ChatRoom chatRoom, String senderId,
                                           boolean isRead, String finalLastContent, ChatMessage chatMessage) {
        return chatRoomRepository.save(chatRoom)
                .flatMap(updatedChatRoom -> {
                    List<Participant> participants = chatRoom.getParticipants();
                    return Flux.fromIterable(participants)
                            .filter(participant ->
                                    !participant.getUserId().equals(senderId) && !isRead
                            )
                                    .flatMap(participant -> sendSSEAndPush(channelId, updatedChatRoom, participant, senderId,
                                    finalLastContent))
                            .then(chatMessageRepository.save(chatMessage))
                            .flatMap(savedChat -> {
                                WebSocketSuccessDTO successDTO = new WebSocketSuccessConverter().toSuccessDTO(
                                        chatDTO,
                                        savedChat.getCreatedAt(),
                                        savedChat.isRead()
                                );
                                return broadcastMessage(channelId, successDTO);
                            });
                });
    }

    private Mono<Void> sendSSEAndPush(String channelId, ChatRoom updatedChatRoom, Participant participant,
                                      String senderId,
                                      String lastContent) {
        // SSE 전송 (알림 설정 여부 무관)
        if (sseManager.doesSinkExist(participant.getUserId())) {
            log.info("SSE 전송: {}", participant.getUserId());
            SSEDTO ssedto = SSEConverter.toSSEDTO(updatedChatRoom, participant);
            sseManager.getOrManageSink(participant.getUserId()).tryEmitNext(ssedto);
            return Mono.empty();
        }
        // Push 전송 (알림 설정된 경우만)
        else if (participant.isNotification()) {
            return chatService.sendPush(
                    participant.getUserId(),
                    senderId,
                    channelId,
                    lastContent
            ).onErrorResume(e -> {
                log.error("푸시 전송 실패: {}", e.getMessage());
                return Mono.empty();
            });
        }
        return Mono.empty();
    }

    private Mono<Void> broadcastMessage(String chatRoomId, WebSocketSuccessDTO successDTO) {
        log.info("broadcastMessage 진입 (USER_LEFT 처리)");
        List<WebSocketSession> chatRoomSessions = sessions.getOrDefault(chatRoomId, List.of());
        log.info("chatRoomSessions : {}", chatRoomSessions);

        // 각 세션에 성공 메시지 브로드캐스트
        return Mono.when(chatRoomSessions.stream()
                .filter(WebSocketSession::isOpen)
                .map(session -> responseHandler.handleSuccess(session, successDTO)
                        .doOnSuccess(unused -> log.info("메시지 전송 성공: {}", session.getId()))
                        .doOnError(e -> log.error("WebSocket 메시지 전송 실패: {}", e.getMessage(), e))
                )
                .toArray(Mono[]::new));
    }

    private ChatDTO parseChatMessage(String payload) throws JsonProcessingException {
        return objectMapper.readValue(payload, ChatDTO.class);
    }

    private String getChannelId(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();

        return query.substring("channelId=".length());
    }
}
