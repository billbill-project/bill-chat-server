package bill.chat.service;

import bill.chat.dto.SSEDTO;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

@Component
@Slf4j
public class SSEManager {

    private final Map<String, Sinks.Many<SSEDTO>> sinks = new ConcurrentHashMap<>();

    public Sinks.Many<SSEDTO> getOrManageSink(String userId) {
        return sinks.compute(userId, (key, existingSink) -> {
            if (existingSink != null && existingSink.currentSubscriberCount() > 0) {
                throw new IllegalStateException("이미 구독 중인 사용자입니다.");
            }

            Sinks.Many<SSEDTO> sink = Sinks.many()
                    .unicast()
                    .onBackpressureBuffer(new ArrayBlockingQueue<>(100));

            SSEDTO initialMessage = SSEDTO.builder()
                    .channelId("system")
                    .senderId("server")
                    .content("success")
                    .unreadCount(0)
                    .notification(false)
                    .updatedAt(LocalDateTime.now())
                    .build();

            sink.tryEmitNext(initialMessage);

            return sink;
        });
    }

    public boolean doesSinkExist(String userId) {
        Many<SSEDTO> foundSink = sinks.get(userId);
        return foundSink != null && foundSink.currentSubscriberCount() > 0;
    }

    public void removeSink(String userId) {
        sinks.get(userId).tryEmitComplete();
        sinks.remove(userId);
    }
}
