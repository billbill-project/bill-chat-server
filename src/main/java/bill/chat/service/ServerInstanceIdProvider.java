package bill.chat.service;

import java.net.InetAddress;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Slf4j
public class ServerInstanceIdProvider {

    private final String serverInstanceId;

    public ServerInstanceIdProvider(
            @Value("${server.instance.id:}") String configuredServerInstanceId,
            @Value("${spring.application.name:chat-server}") String appName,
            @Value("${server.port:0}") String port) {

        if (configuredServerInstanceId != null && !configuredServerInstanceId.isBlank()) {
            this.serverInstanceId = configuredServerInstanceId;
            log.info("Using configured server instance id: {}", this.serverInstanceId);
            return;
        }

        String hostname = resolveHostname();
        if (hostname == null || hostname.isBlank()) {
            hostname = "unknown-host";
        }

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        this.serverInstanceId = appName + "-" + hostname + "-" + port + "-" + suffix;
        log.info("Generated server instance id: {}", this.serverInstanceId);
    }

    private String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return System.getenv("HOSTNAME");
        }
    }
}
