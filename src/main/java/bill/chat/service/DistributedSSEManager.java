package bill.chat.service;

import bill.chat.dto.SSEDTO;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Sinks.Many;

@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedSSEManager {

    private final Map<String, Sinks.Many<SSEDTO>> localSinks = new ConcurrentHashMap<>();
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String SSE_PREFIX = "chat:sse:";
    private static final Duration SSE_TTL = Duration.ofMinutes(90);

    public Mono<Many<SSEDTO>> createAndRegisterSink(String userId) {
        if (localSinks.containsKey(userId) && localSinks.get(userId).currentSubscriberCount() > 0) {
            return Mono.error(new IllegalStateException("이미 구독 중인 사용자입니다."));
        }

        String serverInstance = getServerInstanceId();
        String sseKey = SSE_PREFIX + userId;
        return redisTemplate.opsForValue().set(sseKey, serverInstance, SSE_TTL)
                .doOnSuccess(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        log.info("Redis에 SSE 연결 정보 저장 성공: {}", userId);
                    } else {
                        log.warn("Redis에 SSE 연결 정보 저장 실패: {}", userId);
                    }
                })
                .then(Mono.fromCallable(() -> {
                    Sinks.Many<SSEDTO> sink = Sinks.many()
                            .unicast()
                            .onBackpressureBuffer(new ArrayBlockingQueue<>(100));

                    localSinks.put(userId, sink);

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
                }));
    }

    public boolean hasLocalSSEConnection(String userId) {
        Sinks.Many<SSEDTO> foundSink = localSinks.get(userId);
        return foundSink != null && foundSink.currentSubscriberCount() > 0;
    }

    public reactor.core.publisher.Mono<Boolean> hasAnySSEConnection(String userId) {
        String sseKey = SSE_PREFIX + userId;
        return redisTemplate.hasKey(sseKey);
    }

    public void removeSink(String userId) {
        Sinks.Many<SSEDTO> sink = localSinks.get(userId);
        if (sink != null) {
            sink.tryEmitComplete();
            localSinks.remove(userId);
        }

        // Redis에서도 제거
        unregisterSSEConnection(userId);
    }

    public boolean sendToLocalSSE(SSEDTO message) {
        Sinks.Many<SSEDTO> sink = localSinks.get(message.getTargetUserId());
        if (sink != null && sink.currentSubscriberCount() > 0) {
            sink.tryEmitNext(message);
            return true;
        }
        return false;
    }

    private void unregisterSSEConnection(String userId) {
        String sseKey = SSE_PREFIX + userId;
        redisTemplate.delete(sseKey).subscribe();
    }

    private String getServerInstanceId() {
        return System.getProperty("server.instance.id", "server-" + System.currentTimeMillis());
    }
}