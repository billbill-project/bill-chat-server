package bill.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.test.StepVerifier;

@SpringBootTest
public class GhostSessionTest {

    @Autowired
    private DistributedSessionManager sessionManager;

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Test
    @DisplayName("고스트 세션 버그 재현: 같은 유저가 끊기고 재접속하면 세션이 1개여야함")
    void reproduceGhostSessionBug() {
        // given
        String channelId = "test-channel-1";
        String userId = "user-100";
        String session1 = "session-uuid-1"; // 첫 번째 세션 (비정상 종료될 예정)
        String session2 = "session-uuid-2"; // 재접속한 세션

        // when
        // 1. 유저가 접속함 (세션 1)
        StepVerifier.create(sessionManager.addSessionToChannel(channelId, session1, userId))
                .verifyComplete();

        // 2. 유저가 '비정상 종료' 함 = removeSessionFromChannel()이 호출되지 않음
        // (Redis에는 여전히 session1이 살아있음)

        // 3. 유저가 재접속함 (세션 2)
        StepVerifier.create(sessionManager.addSessionToChannel(channelId, session2, userId))
                .verifyComplete();

        // then
        // 4. 활성 세션 수 조회
        StepVerifier.create(sessionManager.getActiveUserCount(channelId))
                .expectNextMatches(count -> {
                    System.out.println("========================================");
                    System.out.println("조회된 유저 수: " + count);
                    System.out.println("기대하는 유저 수: 1");
                    System.out.println("결과: " + (count == 1 ? "성공! (Ghost Session이 있어도 유저 수는 1)" : "실패"));
                    System.out.println("========================================");
                    return count == 1;
                })
                .verifyComplete();
    }
}
