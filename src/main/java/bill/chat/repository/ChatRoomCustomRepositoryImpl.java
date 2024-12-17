package bill.chat.repository;

import bill.chat.model.ChatRoom;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
@RequiredArgsConstructor
public class ChatRoomCustomRepositoryImpl implements ChatRoomCustomRepository {

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    @Override
    public Flux<ChatRoom> findChatRoomsByChatRoomIdsAndBeforeTimestamp(List<String> chatRoomIds,
                                                                       LocalDateTime beforeTimestamp, int size) {
        Criteria criteria = Criteria.where("channelId").in(chatRoomIds);

        if (beforeTimestamp != null) {
            criteria = criteria.and("updatedAt").lt(beforeTimestamp);
        }

        Query query = Query.query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "updatedAt"))
                .limit(size);

        return reactiveMongoTemplate.find(query, ChatRoom.class);
    }
}
