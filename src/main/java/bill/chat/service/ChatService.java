package bill.chat.service;

import bill.chat.converter.ChatRoomConverter;
import bill.chat.dto.WebhookPayload.CreateChatRoom;
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

    public Flux<ChatMessage> getChatMessages(String channelId, String beforeTimestampStr) {
        int size = 50;
        LocalDateTime beforeTimestamp = null;
        if (beforeTimestampStr != null && !beforeTimestampStr.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            beforeTimestamp = LocalDateTime.parse(beforeTimestampStr, formatter);
        }

        return chatMessageRepository.findMessagesByChannelIdBeforeTimestamp(channelId, beforeTimestamp, size);
    }

    @Transactional
    public Mono<Void> createChatRoom(CreateChatRoom payload) {
        ChatRoom chatRoom = ChatRoomConverter.toChatRoom(payload);
        return chatRoomRepository.save(chatRoom).then();
    }
}
