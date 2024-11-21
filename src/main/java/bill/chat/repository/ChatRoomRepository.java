package bill.chat.repository;

import bill.chat.model.ChatRoom;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ChatRoomRepository extends ReactiveMongoRepository<ChatRoom, String> {
    @Query(value = "{'channelId': ?0}", fields = "{'chat': {$slice: -1}}")
    Mono<ChatRoom> findLastChatMessageByChannelId(String chatRoomId);
}
