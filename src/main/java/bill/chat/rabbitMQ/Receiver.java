package bill.chat.rabbitMQ;

import bill.chat.dto.SSEDTO;
import bill.chat.service.DistributedSSEManager;
import bill.chat.websocket.handler.MyWebSocketHandler;
import bill.chat.websocket.payload.dto.WebSocketSuccessDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class Receiver {
    private final MyWebSocketHandler myWebSocketHandler;
    private final DistributedSSEManager distributedSSEManager;

    @RabbitListener(queues = "#{chatQueue.name}")
    public void consumeAndBroadcastMessage(WebSocketSuccessDTO successDTO) {
        myWebSocketHandler.broadcastToLocalSessions(successDTO.getChannelId(), successDTO)
                .subscribe(null, e -> log.error("로컬 세션 브로드캐스트 중 오류 발생", e));
    }

    @RabbitListener(queues = "#{chatErrorQueue.name}")
    public void consumeAndBroadcastErrorMessage(bill.chat.websocket.payload.dto.WebSocketErrorDTO errorDTO) {
        myWebSocketHandler.sendErrorToLocalSession(errorDTO.getChannelId(), errorDTO.getSenderId(), errorDTO)
                .subscribe(null, e -> log.error("로컬 에러 세션 브로드캐스트 중 오류 발생", e));
    }

    @RabbitListener(queues = "#{sseQueue.name}")
    public void consumeSSEMessage(SSEDTO ssedto) {
        log.info("SSE 큐에서 메시지 수신 성공 Target: {}", ssedto.getTargetUserId());
        try {
            boolean isPresent = distributedSSEManager.hasLocalSSEConnection(ssedto.getTargetUserId());
            log.info("로컬 SSE 연결 확인: Target={}, isPresent={}", ssedto.getTargetUserId(), isPresent);

            if (isPresent) {
                log.info("로컬 SSE 연결 존재. 메시지 전송 시도...");
                boolean success = distributedSSEManager.sendToLocalSSE(ssedto);
                if (!success) {
                    log.warn("로컬 SSE 전송 실패: targetUserId={}", ssedto.getTargetUserId());
                } else {
                    log.info("로컬 SSE 메시지 전송 성공");
                }
            } else {
                log.info("이 서버에 해당 사용자의 로컬 SSE 연결이 없음. Target User: {}", ssedto.getTargetUserId());
            }
        } catch (Exception e) {
            log.error("SSE 메시지 수신 처리 중 예외 발생", e);
        }
    }
}