package bill.chat.repository;

import bill.chat.model.ChatRoom;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ChatRoomRepository extends ReactiveMongoRepository<ChatRoom, String> {

    @Query("{ 'channelId': ?0, 'participants.userId': ?1 }")
    Mono<ChatRoom> findParticipantByChannelIdAndUserId(String channelId, String userId);

    Mono<ChatRoom> findByChannelId(String channelId);
}
