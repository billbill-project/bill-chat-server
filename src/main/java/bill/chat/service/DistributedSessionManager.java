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
    private static final String CHANNEL_USERS_PREFIX = "chat:channel:users:"; // Hash Key
    private static final String SESSION_PREFIX = "chat:session:"; // String Key (Reverse Index)

    private static final Duration SESSION_TTL = Duration.ofMinutes(5);

    /**
     * 입장 처리
     */
    public Mono<Void> addSessionToChannel(String channelId, String sessionId, String userId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        String channelUsersKey = CHANNEL_USERS_PREFIX + channelId;

        // 1. 세션 -> 유저 매핑 (역인덱스)
        Mono<Boolean> saveSession = redisTemplate.opsForValue().set(sessionKey, userId, SESSION_TTL);

        // 2. 채널별 유저 접속 수 증가 (Reference Counting)
        Mono<Long> incrementUserCount = redisTemplate.opsForHash().increment(channelUsersKey, userId, 1);

        // 3. TTL 갱신 (채널 키 자체의 TTL)
        Mono<Boolean> expireChannel = redisTemplate.expire(channelUsersKey, SESSION_TTL);

        return Mono.when(saveSession, incrementUserCount, expireChannel)
                .doOnSuccess(unused -> log.info("세션 등록 완료: channelId={}, userId={}, sessionId={}", channelId, userId,
                        sessionId))
                .then();
    }

    /**
     * 채널 활성 유저 수 조회
     */
    public Mono<Long> getActiveUserCount(String channelId) {
        String channelUsersKey = CHANNEL_USERS_PREFIX + channelId;
        return redisTemplate.opsForHash().size(channelUsersKey);
    }

    /**
     * 세션 종료 및 채널 퇴장 처리
     */
    public Mono<Void> removeSessionFromChannel(String channelId, String sessionId, String userId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        String channelUsersKey = CHANNEL_USERS_PREFIX + channelId;

        return redisTemplate.delete(sessionKey)

                .then(redisTemplate.opsForHash().increment(channelUsersKey, userId, -1)
                        .flatMap(count -> {
                            if (count <= 0) {
                                // 카운트가 0이 되면 해시에서 필드 제거 (진짜 퇴장)
                                return redisTemplate.opsForHash().remove(channelUsersKey, (Object) userId);
                            }
                            return Mono.just(count);
                        }))
                .doOnSuccess(unused -> log.info("세션 제거 완료: channelId={}, userId={}, sessionId={}", channelId, userId,
                        sessionId))
                .then();
    }

    /**
     * 특정 유저가 채널에 접속 중인지 확인
     */
    public Mono<Boolean> isUserConnectedToChannel(String userId, String channelId) {
        String channelUsersKey = CHANNEL_USERS_PREFIX + channelId;
        return redisTemplate.opsForHash().hasKey(channelUsersKey, userId);
    }

    public Mono<Void> refreshSession(String sessionId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        return redisTemplate.expire(sessionKey, SESSION_TTL).then();
    }
}