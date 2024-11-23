package bill.chat.websocket.payload.handler;

import bill.chat.websocket.payload.dto.WebSocketFailureDTO;
import bill.chat.websocket.payload.dto.WebSocketSuccessDTO;
import bill.chat.websocket.payload.exception.WebSocketException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class WebSocketResponseHandler {
    private final ObjectMapper objectMapper;

    @Autowired
    public WebSocketResponseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> handleSuccess(WebSocketSession session, WebSocketSuccessDTO successDTO) {
        return sendResponse(session, successDTO)
                .doOnError(e -> log.error("성공 메시지 전송 중 오류 발생: {}", e.getMessage(), e));
    }

    public Mono<Void> handleError(WebSocketSession session, WebSocketException exception) {
        WebSocketFailureDTO failureDTO = exception.getFailureDTO();
        return sendResponse(session, failureDTO)
                .doOnError(e -> log.error("오류 메시지 전송 중 오류 발생: {}", e.getMessage(), e));
    }

    private <T> Mono<Void> sendResponse(WebSocketSession session, T dto) {
        try {
            String payload = objectMapper.writeValueAsString(dto);//json으로 변환
            return session.send(Mono.just(session.textMessage(payload)));
        } catch (Exception e) {
            log.error("웹소켓 응답 처리 실패 : {}", e.getMessage());
            return Mono.empty();
        }
    }
}
