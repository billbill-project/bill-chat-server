package bill.chat.websocket.handler;

import bill.chat.dto.ChatDTO;
import bill.chat.rabbitMQ.Producer;
import bill.chat.service.ChatService;
import bill.chat.service.DistributedSessionManager;
import bill.chat.websocket.payload.dto.WebSocketSuccessDTO;
import bill.chat.websocket.payload.handler.WebSocketResponseHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
public class MyWebSocketHandler implements WebSocketHandler {
    private final Map<String, List<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final WebSocketResponseHandler responseHandler;
    private final ObjectMapper objectMapper;
    private final DistributedSessionManager distributedSessionManager;
    private final Producer producer;
    private final ChatService chatService;

    private static final Duration PING_INTERVAL = Duration.ofSeconds(20);
    private static final Duration PONG_TIMEOUT = Duration.ofSeconds(45);

    @Override
    public List<String> getSubProtocols() {
        return WebSocketHandler.super.getSubProtocols();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String channelId = getChannelId(session);
        String userId = (String) session.getAttributes().get("userId");
        String sessionId = session.getId();
        AtomicLong lastSeenAt = new AtomicLong(System.currentTimeMillis());

        log.info("WebSocket 연결 시작: userId={}, channelId={}, sessionId={}", userId, channelId, sessionId);

        sessions.computeIfAbsent(channelId, id -> new CopyOnWriteArrayList<>()).add(session);

        Disposable heartbeatDisposable = Flux.interval(PING_INTERVAL)
                .flatMap(tick -> session.send(Mono.just(session.textMessage("ping"))))
                .doOnNext(unused -> {
                    long idleMillis = System.currentTimeMillis() - lastSeenAt.get();
                    if (idleMillis > PONG_TIMEOUT.toMillis() && session.isOpen()) {
                        log.warn("PONG/메시지 미수신으로 세션 종료: sessionId={}, idleMillis={}", sessionId, idleMillis);
                        session.close(new CloseStatus(4008, "HEARTBEAT_TIMEOUT")).subscribe();
                    }
                })
                .doOnError(e -> log.error("하트비트 PING 전송 실패", e))
                .subscribeOn(Schedulers.single())
                .subscribe();

        return distributedSessionManager.addSessionToChannel(channelId, sessionId, userId)
                .then(chatService.markMessagesAsRead(channelId, userId))
                .then(session.receive()
                        .flatMap(message -> handleIncomingMessage(session, channelId, sessionId, lastSeenAt, message))
                        .then())
                .doFinally(signalType -> {
                    log.info("WebSocket 연결 종료: {}, 종료 원인: {}", sessionId, signalType);
                    sessions.getOrDefault(channelId, List.of()).remove(session);
                    distributedSessionManager.removeSessionFromChannel(channelId, sessionId, userId).subscribe();
                    heartbeatDisposable.dispose();
                })
                .onErrorResume(e -> session.close(new CloseStatus(4999, "UNKNOWN_ERROR")));
    }

    private Mono<Void> handleIncomingMessage(WebSocketSession session, String channelId, String sessionId,
            AtomicLong lastSeenAt, WebSocketMessage message) {
        if (message.getType() != WebSocketMessage.Type.TEXT) {
            lastSeenAt.set(System.currentTimeMillis());
            return distributedSessionManager.refreshSession(sessionId).then();
        }

        String payload = message.getPayloadAsText();
        if (payload == null || payload.isBlank()) {
            return Mono.empty();
        }

        if ("pong".equals(payload)) {
            lastSeenAt.set(System.currentTimeMillis());
            return distributedSessionManager.refreshSession(sessionId).then();
        }

        if ("ping".equals(payload)) {
            lastSeenAt.set(System.currentTimeMillis());
            return distributedSessionManager.refreshSession(sessionId)
                    .then(session.send(Mono.just(session.textMessage("pong"))))
                    .then();
        }

        lastSeenAt.set(System.currentTimeMillis());
        return distributedSessionManager.refreshSession(sessionId)
                .then(Mono.defer(() -> {
                    try {
                        ChatDTO chatDTO = parseChatMessage(payload);
                        chatDTO.setChannelId(channelId);
                        return producer.sendChatMessage(chatDTO);
                    } catch (JsonProcessingException e) {
                        log.error("메시지 파싱 오류: {}", payload, e);
                        return session.close(new CloseStatus(4005, "MESSAGE_TYPE_ERROR"));
                    }
                }));
    }

    public Mono<Void> broadcastToLocalSessions(String channelId, WebSocketSuccessDTO successDTO) {
        log.info("로컬 브로드캐스트: channelId={}, 세션 수={}", channelId, sessions.getOrDefault(channelId, List.of()).size());

        List<WebSocketSession> localChannelSessions = sessions.getOrDefault(channelId, List.of());
        return Mono.when(localChannelSessions.stream()
                .filter(WebSocketSession::isOpen)
                .map(session -> responseHandler.handleSuccess(session, successDTO)
                        .doOnSuccess(unused -> log.info("메시지 전송 성공: {}", session.getId()))
                        .doOnError(e -> log.error("WebSocket 메시지 전송 실패: {}", e.getMessage(), e)))
                .toArray(Mono[]::new));
    }

    public Mono<Void> sendErrorToLocalSession(String channelId, String senderId,
            bill.chat.websocket.payload.dto.WebSocketErrorDTO errorDTO) {
        log.info("로컬 에러 전송 시도: channelId={}, senderId={}", channelId, senderId);
        List<WebSocketSession> localChannelSessions = sessions.getOrDefault(channelId, List.of());
        return Mono.when(localChannelSessions.stream()
                .filter(WebSocketSession::isOpen)
                .filter(session -> senderId.equals(session.getAttributes().get("userId")))
                .map(session -> responseHandler.handleError(session, errorDTO)
                        .doOnSuccess(unused -> log.info("에러 메시지 전송 성공: {}", session.getId()))
                        .doOnError(e -> log.error("WebSocket 에러 메시지 전송 실패: {}", e.getMessage(), e)))
                .toArray(Mono[]::new));
    }

    private ChatDTO parseChatMessage(String payload) throws JsonProcessingException {
        return objectMapper.readValue(payload, ChatDTO.class);
    }

    private String getChannelId(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();

        return query.substring("channelId=".length());
    }
}
