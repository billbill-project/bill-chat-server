package bill.chat.websocket.handler;

import bill.chat.config.jwt.JWTUtil;
import bill.chat.repository.ChatRoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Slf4j
public class AuthenticatedWebSocketHandler implements WebSocketHandler {

    private final WebSocketHandler delegate;
    private final JWTUtil jwtUtil;
    private final ChatRoomRepository chatRoomRepository;

    public AuthenticatedWebSocketHandler(WebSocketHandler delegate, JWTUtil jwtUtil,
                                         ChatRoomRepository chatRoomRepository) {
        this.delegate = delegate;
        this.jwtUtil = jwtUtil;
        this.chatRoomRepository = chatRoomRepository;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        if (query == null || query.isEmpty()) {
            return session.close(new CloseStatus(4002, "WRONG_QUERY"));
        }
        if (!query.startsWith("channelId=")) {
            return session.close(new CloseStatus(4002, "WRONG_QUERY"));
        }
        String channelId = query.substring("channelId=".length());
        if (channelId.isEmpty()) {
            return session.close(new CloseStatus(4002, "WRONG_QUERY"));
        }
        return chatRoomRepository.findByChannelId(channelId)
                .hasElement()
                .flatMap(exists -> {
                    if (!exists) {
                        return session.close(new CloseStatus(4003, "NOT_EXIST_CHANNEL"));
                    }
                    String protocolHeader = session.getHandshakeInfo().getHeaders().getFirst("Sec-WebSocket-Protocol");
                    if (protocolHeader != null && protocolHeader.startsWith("Bearer-")) {
                        String token = protocolHeader.substring(7);

                        return jwtUtil.isValidAccessTokenReactive(token)
                                .flatMap(isValid -> {
                                    if (!isValid) {
                                        return session.close(new CloseStatus(4001, "INVALID_JWT"));
                                    }
                                    return jwtUtil.getClaimsReactive(token)
                                            .flatMap(claims -> {
                                                String userId = claims.getSubject();
                                                return chatRoomRepository.findParticipantByChannelIdAndUserId(channelId,
                                                                userId)
                                                        .hasElement()
                                                        .flatMap(ex -> {
                                                            if (!ex) {
                                                                return session.close(new CloseStatus(4004, "INVALID_PARTICIPANT"));
                                                            }
                                                            session.getAttributes().put("userId", userId);
                                                            return delegate.handle(session);
                                                        });
                                            });
                                });
                    }
                    return session.close();
                });
    }
}
