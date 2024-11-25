package bill.chat.repository;

import bill.chat.model.ChatMessage;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
@RequiredArgsConstructor
public class ChatMessageCustomRepositoryImpl implements ChatMessageCustomRepository {

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    @Override
    public Flux<ChatMessage> findMessagesByChannelIdBeforeTimestamp(String channelId, LocalDateTime beforeTimestamp,
                                                                    int size) {
        Criteria criteria = Criteria.where("channelId").is(channelId);
        if (beforeTimestamp != null) {
            criteria = criteria.and("createdAt").lt(beforeTimestamp);
        }

        Query query = Query.query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .limit(size);

        return reactiveMongoTemplate.find(query, ChatMessage.class);
    }
}
