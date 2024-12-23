package bill.chat;

import bill.chat.config.jwt.JWTUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Slf4j
public class AuthenticatedWebSocketHandler implements WebSocketHandler {

    private final WebSocketHandler delegate;
    private final JWTUtil jwtUtil;

    public AuthenticatedWebSocketHandler(WebSocketHandler delegate, JWTUtil jwtUtil) {
        this.delegate = delegate;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String protocolHeader = session.getHandshakeInfo().getHeaders().getFirst("Sec-WebSocket-Protocol");
        if (protocolHeader != null && protocolHeader.startsWith("Bearer-")) {
            String token = protocolHeader.substring(7);
            if (jwtUtil.isValidAccessToken(token)) {
                String userId = jwtUtil.getClaims(token).getSubject();
                session.getAttributes().put("userId", userId);
                log.info("handshake userId={}", userId);

                return delegate.handle(session);
            } else {
                log.error("Invalid WebSocket token");
                return session.close();
            }
        }
        return session.close();
    }
}
