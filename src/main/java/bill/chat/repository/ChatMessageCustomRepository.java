package bill.chat.repository;

import bill.chat.model.ChatMessage;
import java.time.LocalDateTime;
import reactor.core.publisher.Flux;

public interface ChatMessageCustomRepository {
    Flux<ChatMessage> findMessagesByChannelIdBeforeTimestamp(String channelId, LocalDateTime beforeTimestamp, int size);
}
