package bill.chat.service;

import bill.chat.model.ChatMessage;
import bill.chat.repository.ChatMessageRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;

    public Flux<ChatMessage> getChatMessages(String channelId, String beforeTimestampStr) {
        int size = 50;
        LocalDateTime beforeTimestamp = null;
        if (beforeTimestampStr != null && !beforeTimestampStr.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            beforeTimestamp = LocalDateTime.parse(beforeTimestampStr, formatter);
        }

        return chatMessageRepository.findMessagesByChannelIdBeforeTimestamp(channelId, beforeTimestamp, size);
    }

//    public Mono<List<Object>> getChatMessageAndInfo(String channelId) {
//        return chatRoomRepository.findByChannelId(channelId)
//                .map(chatRoom -> {
//                    List<Object> chatRoomInfo = new ArrayList<>();
//                    chatRoomInfo.add(chatRoom.getChat());
//                    chatRoomInfo.add(chatRoom.getChannelId());
//                    chatRoomInfo.add(chatRoom.getParticipants());
//                    chatRoomInfo.add(chatRoom.getIsClosed());
//                    chatRoomInfo.add(chatRoom.getIsDeleted());
//                    return chatRoomInfo;
//                });
//    }
}
