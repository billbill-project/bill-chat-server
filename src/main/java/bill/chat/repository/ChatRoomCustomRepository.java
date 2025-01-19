package bill.chat.repository;

import bill.chat.model.ChatMessage;
import bill.chat.model.ChatRoom;
import java.time.LocalDateTime;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatRoomCustomRepository {
    Flux<ChatRoom> findChatRoomsByChatRoomIdsAndBeforeTimestamp(List<String> chatRoomIds, LocalDateTime beforeTimestamp, int size);
    Mono<Integer> calculateSumOfUnreadCountByUserIdAndChannelIds(String userId, List<String> channelIds);
}
