package bill.chat.repository;

import bill.chat.model.ChatMessage;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface ChatMessageRepository extends ReactiveMongoRepository<ChatMessage, String> , ChatMessageCustomRepository {
    @Query("{ 'channelId': ?0, 'isRead': false, 'senderId': { $ne: ?1 } }")
    Flux<ChatMessage> findUnreadMessages(String channelId, String userId);
}
