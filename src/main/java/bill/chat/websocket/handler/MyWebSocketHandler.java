package bill.chat.websocket.handler;

import static bill.chat.model.enums.MessageType.IMAGE;
import static bill.chat.model.enums.MessageType.SYSTEM;
import static bill.chat.websocket.payload.code.WebSocketErrorStatus.INVALID_MESSAGE_FORMAT;
import static bill.chat.websocket.payload.code.WebSocketErrorStatus.UNKNOWN_CHANNEL;
import static bill.chat.websocket.payload.code.WebSocketErrorStatus.UNKNOWN_USER;

import bill.chat.converter.ChatMessageConverter;
import bill.chat.model.ChatMessage;
import bill.chat.model.dto.ChatDTO;
import bill.chat.repository.ChatRoomRepository;
import bill.chat.websocket.payload.dto.WebSocketSuccessDTO;
import bill.chat.websocket.payload.exception.WebSocketException;
import bill.chat.websocket.payload.handler.WebSocketResponseHandler;
import bill.chat.websocket.payload.handler.WebSocketSuccessConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class MyWebSocketHandler implements WebSocketHandler {
    private final Map<String, List<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final WebSocketResponseHandler responseHandler;
    private final ObjectMapper objectMapper;
    private final ChatRoomRepository chatRoomRepository;

    @Autowired
    public MyWebSocketHandler(ChatRoomRepository chatRoomRepository, ObjectMapper objectMapper) {
        this.chatRoomRepository = chatRoomRepository;
        this.objectMapper = objectMapper;
        this.responseHandler = new WebSocketResponseHandler(objectMapper);
    }

    @Override
    public List<String> getSubProtocols() {
        return WebSocketHandler.super.getSubProtocols();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String channelId = getChannelId(session);
        String userId = getUserId(session);

        return validChannelAndUser(channelId, userId)
                .then(Mono.defer(() -> {
                    sessions.computeIfAbsent(channelId, id -> new CopyOnWriteArrayList<>()).add(session);

                    return session.receive()
                            .flatMap(message -> handleMessage(session, channelId, message.getPayloadAsText()))
                            .then()
                            .doFinally(signalType -> {
                                log.info("WebSocket 연결 종료: {}, 종료 원인: {}", session.getId(), signalType);
                                sessions.getOrDefault(channelId, List.of()).remove(session);
                            });
                }))
                .onErrorResume(e -> {
                    // validUser에서 발생한 예외 처리
                    if (e instanceof WebSocketException) {
                        return responseHandler.handleError(session, (WebSocketException) e);
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> validChannelAndUser(String channelId, String userId) {
        return chatRoomRepository.findByChannelId(channelId)
                .switchIfEmpty(Mono.error(new WebSocketException(UNKNOWN_CHANNEL)))
                .flatMap(chatRoom -> chatRoomRepository.findParticipantByChannelIdAndUserId(channelId, userId)
                                .switchIfEmpty(Mono.error(new WebSocketException(UNKNOWN_USER))))
                                .then();
    }

    private Mono<Void> handleMessage(WebSocketSession session, String channelId, String payload) {
        try {
            ChatDTO chatDTO = parseChatMessage(payload); // 여기서 JSON 파싱
            return processChatMessage(channelId, chatDTO, session);
        } catch (WebSocketException e) {
            log.error("WebSocketException 발생: {}", e.getMessage());
            return responseHandler.handleError(session, e);
        } catch (Exception e) {
            log.error("예기치 않은 예외 발생: {}", e.getMessage());
            return Mono.empty();
        }
    }

    private Mono<Void> processChatMessage(String channelId, ChatDTO chatDTO, WebSocketSession senderSession) {
        return chatRoomRepository.findById(channelId)
                .doOnNext(chatRoom -> log.info("채팅방 조회 성공: {}", channelId))

                .flatMap(chatRoom -> chatRoomRepository.findLastChatMessageByChannelId(channelId)
                        .flatMap(lastChatRoom -> {
                            log.info("메시지 처리 시작...");
                            // 메시지 생성
                            boolean isImage = false;
                            boolean isSystem = false;
                            boolean isRead = true;

                            // 안읽은 메세지 있는지 확인하고 처리하는거 구현 예정
                            //
                            //

                            // 두 명 이상이 session 가지면 읽은 걸로 간주
                            if (sessions.get(channelId).size() < 2) {
                                isRead = false;
                                chatRoom.addUnreadCount();

                            }
                            if (chatDTO.getMessageType() == IMAGE) {
                                isImage = true;
                            }
                            if (chatDTO.getMessageType() == SYSTEM) {
                                isSystem = true;
                            }
                            int seq = 0;
                            if (!lastChatRoom.getChat().isEmpty()) {
                                seq = lastChatRoom.getChat().get(0).getSeq() + 1;
                            }
                            log.info("next seq : {}", seq);
                            String senderId = chatDTO.getSenderId();
                            String content = chatDTO.getContent();
                            ChatMessage chatMessage = ChatMessageConverter.toChatMessage(seq, senderId, content,
                                    isImage, isSystem, isRead);

                            chatRoom.addChatMessage(chatMessage);

                            // 저장 후 브로드캐스트
                            return chatRoomRepository.save(chatRoom)
                                    .then(broadcastMessage(channelId, chatDTO));
                        }));
    }

    private Mono<Void> broadcastMessage(String chatRoomId, ChatDTO chatDTO) {
        log.info("broadcastMessage 진입");
        List<WebSocketSession> chatRoomSessions = sessions.getOrDefault(chatRoomId, List.of());
        log.info("chatRoomSessions : {}", chatRoomSessions);
        // 각 세션의 상태를 로깅
        chatRoomSessions.stream()
                .forEach(session -> {
                    boolean isOpen = session.isOpen();
                    log.info("세션 상태: {} - {}", session.getId(), isOpen ? "Open" : "Closed");
                });
        // WebSocketSuccessDTO 변환
        WebSocketSuccessDTO successDTO = new WebSocketSuccessConverter().toSuccessDTO(
                chatDTO,
                LocalDateTime.now()
        );
        log.info("WebSocketSuccessDTO 생성 : {}", successDTO.getChannelId());
        // 각 세션에 성공 메시지 브로드캐스트
        return Mono.when(chatRoomSessions.stream()
                        .filter(WebSocketSession::isOpen)
                        .map(session -> responseHandler.handleSuccess(session, successDTO)
                                .doOnSuccess(unused -> log.info("메시지 전송 성공: {}", session.getId()))
                                .doOnError(e -> log.error("WebSocket 메시지 전송 실패: {}", e.getMessage(), e))
                        )
                        .toArray(Mono[]::new))
                .doOnSuccess(unused -> log.info("모든 메시지 전송 완료"))
                .doOnError(e -> log.error("메시지 브로드캐스트 중 오류 발생: {}", e.getMessage(), e));
    }

    private ChatDTO parseChatMessage(String payload) {
        try {
            return objectMapper.readValue(payload, ChatDTO.class);
        } catch (JsonProcessingException e) {
            throw new WebSocketException(INVALID_MESSAGE_FORMAT);
        }
    }

    //임시로 쿼리 스트링으로 userId, channelId 받아오기 (userId는 jwt 로직 작성 후 쿼리 스트링 제거)
    private Map<String, String> getQueryParams(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("쿼리 스트링이 비어있음");
        }

        Map<String, String> params = new HashMap<>();
        for (String param : query.split("&")) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }

    private String getChannelId(WebSocketSession session) {
        Map<String, String> queryParams = getQueryParams(session);
        String channelId = queryParams.get("channelId");
        if (channelId == null) {
            throw new IllegalArgumentException("channelId가 쿼리 스트링에 존재 x");
        }
        return channelId;
    }

    private String getUserId(WebSocketSession session) {
        Map<String, String> queryParams = getQueryParams(session);
        String userId = queryParams.get("userId");
        if (userId == null) {
            throw new IllegalArgumentException("userId가 쿼리 스트링에 존재 x");
        }
        return userId;
    }
}
