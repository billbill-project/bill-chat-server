package bill.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedSessionManager {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String CHANNEL_SESSIONS_PREFIX = "chat:channel:";
    private static final String SESSION_PREFIX = "chat:session:";
    private static final String USER_SESSIONS_PREFIX = "user:sessions:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(5);

    public Mono<Void> addSessionToChannel(String channelId, String sessionId, String userId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        String channelSessionsKey = CHANNEL_SESSIONS_PREFIX + channelId;
        String userSessionsKey = USER_SESSIONS_PREFIX + userId;

        return redisTemplate.opsForValue().set(sessionKey, userId, SESSION_TTL)
                .then(redisTemplate.opsForSet().add(channelSessionsKey, sessionId))
                .then(redisTemplate.expire(channelSessionsKey, SESSION_TTL))
                .then(redisTemplate.opsForSet().add(userSessionsKey, sessionId))
                .doOnSuccess(unused -> log.info("세션 및 역인덱스 추가: channelId={}, sessionId={}, userId={}",
                        channelId, sessionId, userId))
                .then();
    }

    public Mono<Long> getActiveSessionCount(String channelId) {
        String channelSessionsKey = CHANNEL_SESSIONS_PREFIX + channelId;

        return redisTemplate.opsForSet()
                .members(channelSessionsKey)
                .flatMap(sessionId -> {
                    String sessionKey = SESSION_PREFIX + sessionId;
                    return redisTemplate.hasKey(sessionKey)
                            .filter(exists -> exists)
                            .map(exists -> 1L)
                            .defaultIfEmpty(0L)
                            .doOnNext(exists -> {
                                if (exists == 0L) {
                                    redisTemplate.opsForSet().remove(channelSessionsKey, sessionId).subscribe();
                                }
                            });
                })
                .reduce(0L, Long::sum);

    }

    public Mono<Void> removeSessionFromChannel(String channelId, String sessionId, String userId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        String channelSessionsKey = CHANNEL_SESSIONS_PREFIX + channelId;
        String userSessionsKey = USER_SESSIONS_PREFIX + userId;

        return redisTemplate.delete(sessionKey)
                .then(redisTemplate.opsForSet().remove(channelSessionsKey, sessionId))
                .then(redisTemplate.opsForSet().remove(userSessionsKey, sessionId))
                .doOnSuccess(unused -> log.info("세션 및 역인덱스 제거: channelId={}, sessionId={}, userId={}",
                        channelId, sessionId, userId))
                .then();
    }

    public Mono<Boolean> isUserConnectedToChannel(String userId, String channelId) {
        String userSessionsKey = USER_SESSIONS_PREFIX + userId;
        String channelSessionsKey = CHANNEL_SESSIONS_PREFIX + channelId;

        return redisTemplate.opsForSet().intersect(userSessionsKey, channelSessionsKey)
                .hasElements();
    }

    public Mono<Void> refreshSession(String sessionId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        return redisTemplate.expire(sessionKey, SESSION_TTL).then();
    }
}