package bill.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedSessionManager {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ServerInstanceIdProvider serverInstanceIdProvider;
    private static final String CHANNEL_USERS_PREFIX = "chat:channel:users:"; // Hash Key
    private static final String CHANNEL_SERVERS_PREFIX = "chat:channel:servers:"; // Hash Key
    private static final String SESSION_PREFIX = "chat:session:"; // String Key (Reverse Index)

    private static final Duration SESSION_TTL = Duration.ofSeconds(75);

    /**
     * 입장 처리
     */
    public Mono<Void> addSessionToChannel(String channelId, String sessionId, String userId) {
        String serverId = serverInstanceIdProvider.getServerInstanceId();
        String sessionKey = SESSION_PREFIX + sessionId;
        String channelUsersKey = CHANNEL_USERS_PREFIX + channelId;
        String channelServersKey = CHANNEL_SERVERS_PREFIX + channelId;

        // 1. 세션 -> 유저 매핑 (역인덱스)
        Mono<Boolean> saveSession = redisTemplate.opsForValue()
                .set(sessionKey, channelId + "|" + userId + "|" + serverId, SESSION_TTL);

        // 2. 채널별 유저 접속 수 증가 (Reference Counting)
        Mono<Long> incrementUserCount = redisTemplate.opsForHash().increment(channelUsersKey, userId, 1);

        // 3. 채널별 서버 접속 수 증가
        Mono<Long> incrementServerCount = redisTemplate.opsForHash().increment(channelServersKey, serverId, 1);

        return Mono.when(saveSession, incrementUserCount, incrementServerCount)
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
        String serverId = serverInstanceIdProvider.getServerInstanceId();
        String sessionKey = SESSION_PREFIX + sessionId;
        String channelUsersKey = CHANNEL_USERS_PREFIX + channelId;
        String channelServersKey = CHANNEL_SERVERS_PREFIX + channelId;

        return redisTemplate.delete(sessionKey)

                .then(redisTemplate.opsForHash().increment(channelUsersKey, userId, -1)
                        .flatMap(count -> {
                            if (count <= 0) {
                                // 카운트가 0이 되면 해시에서 필드 제거 (진짜 퇴장)
                                return redisTemplate.opsForHash().remove(channelUsersKey, (Object) userId);
                            }
                            return Mono.just(count);
                        }))
                .then(redisTemplate.opsForHash().increment(channelServersKey, serverId, -1)
                        .flatMap(count -> {
                            if (count <= 0) {
                                return redisTemplate.opsForHash().remove(channelServersKey, (Object) serverId);
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
        return redisTemplate.opsForValue().get(sessionKey)
                .flatMap(sessionValue -> {
                    Mono<Boolean> refreshSession = redisTemplate.expire(sessionKey, SESSION_TTL);
                    return refreshSession.then();
                })
                .switchIfEmpty(redisTemplate.expire(sessionKey, SESSION_TTL).then());
    }

    public Mono<Set<String>> getActiveServerIds(String channelId) {
        String channelServersKey = CHANNEL_SERVERS_PREFIX + channelId;
        return redisTemplate.opsForHash().keys(channelServersKey)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    public Mono<Void> reconcilePresenceFromSessionIndex() {
        Mono<List<String>> sessionValuesMono = redisTemplate.keys(SESSION_PREFIX + "*")
                .flatMap(sessionKey -> redisTemplate.opsForValue().get(sessionKey))
                .collectList();

        return sessionValuesMono.flatMap(sessionValues -> {
            Map<String, Map<Object, Object>> channelUserCounts = new HashMap<>();
            Map<String, Map<Object, Object>> channelServerCounts = new HashMap<>();

            for (String sessionValue : sessionValues) {
                SessionMeta meta = parseSessionMeta(sessionValue);
                if (meta == null) {
                    continue;
                }

                String channelId = meta.channelId();
                String userId = meta.userId();
                String serverId = meta.serverId();

                channelUserCounts.computeIfAbsent(channelId, k -> new HashMap<>());
                channelServerCounts.computeIfAbsent(channelId, k -> new HashMap<>());

                Map<Object, Object> userMap = channelUserCounts.get(channelId);
                Map<Object, Object> serverMap = channelServerCounts.get(channelId);

                long userCount = ((Number) userMap.getOrDefault(userId, 0L)).longValue() + 1L;
                long serverCount = ((Number) serverMap.getOrDefault(serverId, 0L)).longValue() + 1L;

                userMap.put(userId, userCount);
                serverMap.put(serverId, serverCount);
            }

            Mono<List<String>> existingUserKeysMono = redisTemplate.keys(CHANNEL_USERS_PREFIX + "*").collectList();
            Mono<List<String>> existingServerKeysMono = redisTemplate.keys(CHANNEL_SERVERS_PREFIX + "*").collectList();

            return Mono.zip(existingUserKeysMono, existingServerKeysMono)
                    .flatMap(existingKeys -> {
                        List<Mono<Void>> tasks = new ArrayList<>();

                        List<String> existingUserKeys = existingKeys.getT1();
                        List<String> existingServerKeys = existingKeys.getT2();

                        Set<String> targetUserKeys = channelUserCounts.keySet().stream()
                                .map(channelId -> CHANNEL_USERS_PREFIX + channelId)
                                .collect(Collectors.toSet());
                        Set<String> targetServerKeys = channelServerCounts.keySet().stream()
                                .map(channelId -> CHANNEL_SERVERS_PREFIX + channelId)
                                .collect(Collectors.toSet());

                        for (String key : existingUserKeys) {
                            if (!targetUserKeys.contains(key)) {
                                tasks.add(redisTemplate.delete(key).then());
                            }
                        }
                        for (String key : existingServerKeys) {
                            if (!targetServerKeys.contains(key)) {
                                tasks.add(redisTemplate.delete(key).then());
                            }
                        }

                        for (Map.Entry<String, Map<Object, Object>> entry : channelUserCounts.entrySet()) {
                            String channelId = entry.getKey();
                            String key = CHANNEL_USERS_PREFIX + channelId;
                            Map<Object, Object> hash = entry.getValue();
                            tasks.add(rewriteHash(key, hash));
                        }

                        for (Map.Entry<String, Map<Object, Object>> entry : channelServerCounts.entrySet()) {
                            String channelId = entry.getKey();
                            String key = CHANNEL_SERVERS_PREFIX + channelId;
                            Map<Object, Object> hash = entry.getValue();
                            tasks.add(rewriteHash(key, hash));
                        }

                        if (tasks.isEmpty()) {
                            return Mono.empty();
                        }
                        return Mono.when(tasks).then();
                    });
        });
    }

    private Mono<Void> rewriteHash(String key, Map<Object, Object> values) {
        if (values.isEmpty()) {
            return redisTemplate.delete(key).then();
        }
        return redisTemplate.delete(key)
                .then(redisTemplate.opsForHash().putAll(key, values))
                .then();
    }

    private SessionMeta parseSessionMeta(String sessionValue) {
        if (sessionValue == null || sessionValue.isBlank()) {
            return null;
        }
        String[] parts = sessionValue.split("\\|");
        if (parts.length != 3) {
            return null;
        }
        return new SessionMeta(parts[0], parts[1], parts[2]);
    }

    private record SessionMeta(String channelId, String userId, String serverId) {
    }
}
