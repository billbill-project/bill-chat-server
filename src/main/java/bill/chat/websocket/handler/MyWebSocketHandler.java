package bill.chat.websocket.handler;

import static bill.chat.model.enums.MessageType.IMAGE;
import static bill.chat.model.enums.MessageType.SYSTEM;
import static bill.chat.model.enums.SystemType.RESERVATION_REQUEST;
import static bill.chat.model.enums.SystemType.USER_LEFT;
import static bill.chat.websocket.payload.code.WebSocketErrorStatus.INVALID_MESSAGE_FORMAT;
import static bill.chat.websocket.payload.code.WebSocketErrorStatus.UNKNOWN_CHANNEL;
import static bill.chat.websocket.payload.code.WebSocketErrorStatus.UNKNOWN_USER;

import bill.chat.converter.ChatMessageConverter;
import bill.chat.converter.SSEConverter;
import bill.chat.dto.SSEDTO;
import bill.chat.model.ChatMessage;
import bill.chat.dto.ChatDTO;
import bill.chat.model.Participant;
import bill.chat.model.enums.SystemType;
import bill.chat.repository.ChatMessageRepository;
import bill.chat.repository.ChatRoomRepository;
import bill.chat.service.SSEManager;
import bill.chat.websocket.payload.dto.WebSocketSuccessDTO;
import bill.chat.websocket.payload.exception.WebSocketException;
import bill.chat.websocket.payload.handler.WebSocketResponseHandler;
import bill.chat.websocket.payload.handler.WebSocketSuccessConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class MyWebSocketHandler implements WebSocketHandler {
    private final Map<String, List<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final WebSocketResponseHandler responseHandler;
    private final ObjectMapper objectMapper;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SSEManager sseManager;

    @Override
    public List<String> getSubProtocols() {
        return WebSocketHandler.super.getSubProtocols();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String channelId = getChannelId(session);
        String userId = (String) session.getAttributes().get("userId");

        log.info("WebSocket 연결 시작: userId={}, channelId={}, sessionId={}", userId, channelId, session.getId());

        return validUser(channelId, userId)
                .doOnError(error -> log.error("Valid user check failed: {}", error.getMessage()))
                .then(Mono.defer(() -> {
                    sessions.computeIfAbsent(channelId, id -> new CopyOnWriteArrayList<>()).add(session);

                    return processRead(channelId, userId)
                            .then(session.receive()
                                    .flatMap(message -> handleMessage(session, channelId, message.getPayloadAsText()))
                                    .then())
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

    private Mono<Void> validUser(String channelId, String userId) {
        return chatRoomRepository.findByChannelId(channelId)
                .switchIfEmpty(Mono.error(new WebSocketException(UNKNOWN_CHANNEL)))
                .flatMap(chatRoom -> chatRoomRepository.findParticipantByChannelIdAndUserId(channelId, userId)
                        .switchIfEmpty(Mono.error(new WebSocketException(UNKNOWN_USER))))
                .then();
    }

    private Mono<Void> processRead(String channelId, String userId) {
        Mono<Long> unreadMessagesCount = chatMessageRepository.findUnreadMessages(channelId, userId)
                .flatMap(chatMessage -> {
                    chatMessage.changeRead();
                    return chatMessageRepository.save(chatMessage);
                })
                .count();

        return unreadMessagesCount.flatMap(count -> {
            if (count > 0) {
                return chatRoomRepository.findByChannelId(channelId)
                        .flatMap(chatRoom -> {
                            chatRoom.resetUnreadCount();
                            return chatRoomRepository.save(chatRoom).then();
                        });
            }
            return Mono.empty();
        });
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
        return chatRoomRepository.findByChannelId(channelId)
                .doOnNext(chatRoom -> log.info("채팅방 조회 성공: {}", channelId))

                .flatMap(chatRoom -> {
                    log.info("메시지 처리 시작...");
                    // 메시지 생성
                    boolean isRead = true;
                    SystemType systemType = null;
                    LocalDate startedAt = null;
                    LocalDate endedAt = null;
                    Integer price = null;
                    String lastContent = chatDTO.getContent();

                    // 두 명 이상이 session 가지면 읽은 걸로 간주
                    if (sessions.get(channelId).size() < 2) {
                        isRead = false;
                        chatRoom.addUnreadCount();

                    }
                    if (chatDTO.getMessageType() == IMAGE) {
                        lastContent = "사진";
                    }
                    if (chatDTO.getMessageType() == SYSTEM) {
                        systemType = chatDTO.getSystemType();
                        if (systemType.equals(RESERVATION_REQUEST)) {
                            startedAt = chatDTO.getStartedAt();
                            endedAt = chatDTO.getEndedAt();
                            price = chatDTO.getPrice();
                        }

                        if (systemType.equals(USER_LEFT)) {
                            WebSocketSuccessDTO successDTO = new WebSocketSuccessConverter().toSuccessDTO(
                                    chatDTO,
                                    LocalDateTime.now(),
                                    isRead
                            );
                            return broadcastMessage(channelId, successDTO);
                        }
                    }
                    chatRoom.updateSender(chatDTO.getSenderId());
                    chatRoom.updateLastMessage(lastContent);
                    String senderId = chatDTO.getSenderId();
                    ChatMessage chatMessage = ChatMessageConverter.toChatMessage(channelId, senderId, lastContent, systemType,
                            chatDTO.getMessageType(), isRead, startedAt, endedAt, price);

                    return chatRoomRepository.save(chatRoom)
                            .doOnSuccess(updatedChatRoom -> {
                                List<Participant> participants = chatRoom.getParticipants();
                                for (Participant participant : participants) {
                                    // 메세지 보낸 사람 아닌 상대방의 sink 존재 확인
                                    if (!participant.getUserId().equals(senderId) && sseManager.doesSinkExist(participant.getUserId())) {
                                        SSEDTO ssedto = SSEConverter.toSSEDTO(chatRoom);
                                        sseManager.getOrManageSink(participant.getUserId()).tryEmitNext(ssedto);
                                    }
                                }
                            })
                            .then(chatMessageRepository.save(chatMessage))
                            .flatMap(savedChat -> broadcastMessage(channelId, chatDTO, savedChat));
                });
    }

    private Mono<Void> broadcastMessage(String chatRoomId, WebSocketSuccessDTO successDTO) {
        log.info("broadcastMessage 진입 (USER_LEFT 처리)");
        List<WebSocketSession> chatRoomSessions = sessions.getOrDefault(chatRoomId, List.of());
        log.info("chatRoomSessions : {}", chatRoomSessions);

        // 각 세션에 성공 메시지 브로드캐스트
        return Mono.when(chatRoomSessions.stream()
                        .filter(WebSocketSession::isOpen)
                        .map(session -> responseHandler.handleSuccess(session, successDTO)
                                .doOnSuccess(unused -> log.info("메시지 전송 성공: {}", session.getId()))
                                .doOnError(e -> log.error("WebSocket 메시지 전송 실패: {}", e.getMessage(), e))
                        )
                        .toArray(Mono[]::new))
                .doOnSuccess(unused -> log.info("모든 메시지 전송 완료 (USER_LEFT 메시지)"))
                .doOnError(e -> log.error("메시지 브로드캐스트 중 오류 발생 (USER_LEFT): {}", e.getMessage(), e));
    }


    private Mono<Void> broadcastMessage(String chatRoomId, ChatDTO chatDTO, ChatMessage savedChat) {
        log.info("broadcastMessage 진입");
        List<WebSocketSession> chatRoomSessions = sessions.getOrDefault(chatRoomId, List.of());
        log.info("chatRoomSessions : {}", chatRoomSessions);

        // WebSocketSuccessDTO 변환
        WebSocketSuccessDTO successDTO = new WebSocketSuccessConverter().toSuccessDTO(
                chatDTO,
                savedChat.getCreatedAt(),
                savedChat.isRead()
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

    private String getChannelId(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("쿼리 파라미터 안 들어옴");
        }

        // 쿼리 파라미터 파싱
        Map<String, String> queryParams = Arrays.stream(query.split("&"))
                .map(param -> param.split("="))
                .filter(param -> param.length == 2)
                .collect(Collectors.toMap(param -> param[0], param -> param[1]));

        String channelId = queryParams.get("channelId");
        if (channelId == null) {
            throw new IllegalArgumentException("쿼리 파라미터 안 들어옴");
        }

        return channelId;
    }
}
