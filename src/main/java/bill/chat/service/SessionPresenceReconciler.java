package bill.chat.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionPresenceReconciler {

    private final DistributedSessionManager distributedSessionManager;

    @Scheduled(
            initialDelayString = "${chat.session-reconcile.initial-delay-ms:60000}",
            fixedDelayString = "${chat.session-reconcile.fixed-delay-ms:30000}")
    public void reconcilePresence() {
        try {
            distributedSessionManager.reconcilePresenceFromSessionIndex()
                    .block(Duration.ofSeconds(20));
        } catch (Exception e) {
            log.warn("세션 Presence 재조정 실패", e);
        }
    }
}
