package bill.chat.service;

import bill.chat.dto.SSEDTO;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Sinks.Many;

@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedSSEManager {

    private final Map<String, LocalSSEConnection> localSinks = new ConcurrentHashMap<>();
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ServerInstanceIdProvider serverInstanceIdProvider;
    private static final String SSE_PREFIX = "chat:sse:";
    private static final Duration SSE_TTL = Duration.ofSeconds(60);

    public Mono<SSEConnectionRegistration> createAndRegisterSink(String userId) {
        String connectionId = UUID.randomUUID().toString();
        String serverInstance = serverInstanceIdProvider.getServerInstanceId();
        String redisValue = serverInstance + ":" + connectionId;

        // 동일 유저 재구독 시 최신 연결로 takeover
        LocalSSEConnection previous = localSinks.get(userId);
        if (previous != null) {
            previous.sink().tryEmitComplete();
        }

        Sinks.Many<SSEDTO> sink = Sinks.many()
                .unicast()
                .onBackpressureBuffer(new ArrayBlockingQueue<>(100));
        localSinks.put(userId, new LocalSSEConnection(connectionId, sink));

        String sseKey = SSE_PREFIX + userId;
        return redisTemplate.opsForValue().set(sseKey, redisValue, SSE_TTL)
                .doOnSuccess(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        log.info("Redis에 SSE 연결 정보 저장 성공: userId={}, connId={}", userId, connectionId);
                    } else {
                        log.warn("Redis에 SSE 연결 정보 저장 실패: {}", userId);
                    }
                })
                .then(Mono.fromCallable(() -> {
                    SSEDTO initialMessage = SSEDTO.builder()
                            .channelId("system")
                            .senderId("server")
                            .content("success")
                            .unreadCount(0)
                            .notification(false)
                            .updatedAt(LocalDateTime.now())
                            .build();

                    sink.tryEmitNext(initialMessage);

                    return new SSEConnectionRegistration(connectionId, sink);
                }));
    }

    public boolean hasLocalSSEConnection(String userId) {
        LocalSSEConnection foundSink = localSinks.get(userId);
        return foundSink != null && foundSink.sink().currentSubscriberCount() > 0;
    }

    public Mono<String> findConnectedServerId(String userId) {
        String sseKey = SSE_PREFIX + userId;
        return redisTemplate.opsForValue().get(sseKey)
                .map(this::extractServerId);
    }

    public Mono<Void> refreshConnection(String userId, String connectionId) {
        LocalSSEConnection local = localSinks.get(userId);
        if (local == null || !local.connectionId().equals(connectionId)) {
            return Mono.empty();
        }
        String value = serverInstanceIdProvider.getServerInstanceId() + ":" + connectionId;
        String sseKey = SSE_PREFIX + userId;
        return redisTemplate.opsForValue().set(sseKey, value, SSE_TTL).then();
    }

    public void removeSink(String userId, String connectionId) {
        LocalSSEConnection sink = localSinks.get(userId);
        if (sink == null || !sink.connectionId().equals(connectionId)) {
            return;
        }

        sink.sink().tryEmitComplete();
        localSinks.remove(userId);
        unregisterSSEConnectionIfMatched(userId, connectionId);
    }

    public boolean sendToLocalSSE(SSEDTO message) {
        LocalSSEConnection sink = localSinks.get(message.getTargetUserId());
        if (sink != null && sink.sink().currentSubscriberCount() > 0) {
            sink.sink().tryEmitNext(message);
            return true;
        }
        return false;
    }

    private void unregisterSSEConnectionIfMatched(String userId, String connectionId) {
        String sseKey = SSE_PREFIX + userId;
        String expectedValue = serverInstanceIdProvider.getServerInstanceId() + ":" + connectionId;
        redisTemplate.opsForValue().get(sseKey)
                .flatMap(currentValue -> {
                    if (expectedValue.equals(currentValue)) {
                        return redisTemplate.delete(sseKey);
                    }
                    return Mono.just(false);
                })
                .subscribe();
    }

    private String extractServerId(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        int delimiter = value.indexOf(':');
        if (delimiter <= 0) {
            return value;
        }
        return value.substring(0, delimiter);
    }

    private record LocalSSEConnection(String connectionId, Sinks.Many<SSEDTO> sink) {
    }

    public record SSEConnectionRegistration(String connectionId, Sinks.Many<SSEDTO> sink) {
    }
}
