package bill.chat.service;

import bill.chat.apiPayload.code.status.ErrorStatus;
import bill.chat.apiPayload.exception.GeneralException;
import bill.chat.converter.ChatRoomConverter;
import bill.chat.dto.WebhookPayload.CreateChatRoomPayload;
import bill.chat.dto.WebhookPayload.GetChatListPayload;
import bill.chat.model.ChatMessage;
import bill.chat.model.ChatRoom;
import bill.chat.repository.ChatMessageRepository;
import bill.chat.repository.ChatRoomRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;

    public Flux<ChatMessage> getChatMessages(String channelId, String beforeTimestampStr, String userId) {
        int size = 30;

        Mono<LocalDateTime> beforeTimestampMono = Mono.fromSupplier(() -> {
            if (beforeTimestampStr != null && !beforeTimestampStr.isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                return LocalDateTime.parse(beforeTimestampStr, formatter);
            }
            return null;
        });

        return chatRoomRepository.findById(channelId)
                .switchIfEmpty(Mono.error(new GeneralException(ErrorStatus.NOT_FOUND_CHANNEL)))
                .flatMapMany(chatRoom -> {
                    boolean isParticipant = chatRoom.getParticipants().stream()
                            .anyMatch(participant -> participant.getUserId().equals(userId));
                    if (!isParticipant) {
                        return Flux.error(new GeneralException(ErrorStatus.NOT_PARTICIPANT));
                    }

                    return beforeTimestampMono.flatMapMany(beforeTimestamp ->
                            chatMessageRepository.findMessagesByChannelIdBeforeTimestamp(channelId, beforeTimestamp, size)
                    );
                });
    }

    @Transactional
    public Mono<Void> createChatRoom(CreateChatRoomPayload payload) {
        ChatRoom chatRoom = ChatRoomConverter.toChatRoom(payload);
        return chatRoomRepository.save(chatRoom).then();
    }

    @Transactional
    public Flux<ChatRoom> getChatList(GetChatListPayload payload) {
        int size = 15;
        LocalDateTime beforeTimestamp = null;
        if (payload.getBeforeTimestamp() != null && !payload.getBeforeTimestamp().isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            beforeTimestamp = LocalDateTime.parse(payload.getBeforeTimestamp(), formatter);
        }
        return chatRoomRepository.findChatRoomsByChatRoomIdsAndBeforeTimestamp(payload.getChatRoomIds(), beforeTimestamp, size);
    }
}
