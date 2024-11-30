package bill.chat.service;

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

    public Mono<ChatRoom> getChatRoom(String channelId) {
        return chatRoomRepository.findByChannelId(channelId);
    }

    public Mono<Void> processChatRoomLeave(String channelId, String userId) {
        return chatRoomRepository.findByChannelId(channelId)
                .flatMap(chatRoom -> {
                    chatRoom.processLeftUser(userId);
                    chatRoom.checkAndUpdateDelete();
                    return chatRoomRepository.save(chatRoom);
                })
                .then();
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
