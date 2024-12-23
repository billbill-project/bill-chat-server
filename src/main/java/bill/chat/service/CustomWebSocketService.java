package bill.chat.service;

import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.function.Supplier;

public class CustomWebSocketService implements WebSocketService {

    private final RequestUpgradeStrategy upgradeStrategy;

    public CustomWebSocketService(RequestUpgradeStrategy upgradeStrategy) {
        this.upgradeStrategy = upgradeStrategy;
    }

    @Override
    public Mono<Void> handleRequest(ServerWebExchange exchange, WebSocketHandler webSocketHandler) {
        String subprotocol = exchange.getRequest().getHeaders().getFirst("Sec-WebSocket-Protocol");
        if (subprotocol != null) {
            exchange.getResponse().getHeaders().set("Sec-WebSocket-Protocol", subprotocol);
        }

        Supplier<HandshakeInfo> handshakeInfoFactory = () -> {
            Mono<Principal> principal = exchange.getPrincipal()
                    .switchIfEmpty(Mono.just(() -> "anonymous"));

            return new HandshakeInfo(
                    exchange.getRequest().getURI(),
                    exchange.getRequest().getHeaders(),
                    principal,
                    subprotocol
            );
        };

        return upgradeStrategy.upgrade(exchange, webSocketHandler, subprotocol, handshakeInfoFactory);
    }
}


