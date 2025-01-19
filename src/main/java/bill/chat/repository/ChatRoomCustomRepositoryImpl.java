package bill.chat.repository;

import bill.chat.model.ChatRoom;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
@Slf4j
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

    @Override
    public Mono<Integer> calculateSumOfUnreadCountByUserIdAndChannelIds(String userId, List<String> chatRoomIds) {
        log.info(chatRoomIds.toString());
        Criteria criteria = Criteria.where("channelId").in(chatRoomIds)
                .and("lastSender").ne(userId);

        Query query = Query.query(criteria);

        query.fields().include("unreadCount");

        return reactiveMongoTemplate.find(query, ChatRoom.class)
                .map(ChatRoom::getUnreadCount)
                .reduce(0, Integer::sum);
    }

}
