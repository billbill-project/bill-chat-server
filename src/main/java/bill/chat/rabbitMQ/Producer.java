package bill.chat.rabbitMQ;

import bill.chat.dto.ChatDTO;
import bill.chat.dto.PushDTO;
import bill.chat.dto.SSEDTO;
import bill.chat.websocket.payload.dto.WebSocketSuccessDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class Producer {
    private final RabbitTemplate rabbitTemplate;

    public Mono<Void> sendChatMessage(ChatDTO chatDTO) {
        return Mono.fromRunnable(() -> {
            try {
                rabbitTemplate.convertAndSend("chat.processing.exchange", "chat.process", chatDTO);
                log.info("채팅 메시지 MQ 전송 성공: channelId={}", chatDTO.getChannelId());
            } catch (Exception e) {
                log.error("채팅 메시지 MQ 전송 실패: channelId={}, error={}", chatDTO.getChannelId(), e.getMessage());
                throw new RuntimeException("MQ 전송 실패", e);
            }
        })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    public Mono<Void> sendWebSocketMessageToServer(String serverId, WebSocketSuccessDTO successDTO) {
        return Mono.fromRunnable(() -> {
            try {
                rabbitTemplate.convertAndSend("chat.ws.exchange", "ws.server." + serverId, successDTO);
                log.info("WS 메시지 서버 라우팅 성공: channelId={}, serverId={}", successDTO.getChannelId(), serverId);
            } catch (Exception e) {
                throw new RuntimeException("WS 메시지 라우팅 실패", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<Void> sendSSEMessageToServer(String serverId, SSEDTO ssedto) {
        return Mono.fromRunnable(() -> {
            try {
                rabbitTemplate.convertAndSend("chat.sse.exchange", "sse.server." + serverId, ssedto);
                log.info("SSE 메시지 서버 라우팅 성공: targetUserId={}, serverId={}", ssedto.getTargetUserId(), serverId);
            } catch (Exception e) {
                log.error("SSE 메시지 MQ 전송 실패: targetUserId={}, error={}", ssedto.getTargetUserId(), e.getMessage());
                throw new RuntimeException("MQ 전송 실패", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<Void> sendPushMessage(PushDTO pushdto) {
        return Mono.fromRunnable(() -> {
            try {
                rabbitTemplate.convertAndSend("push.exchange", "push", pushdto);
                log.info("Push 메시지 MQ 전송 성공: targetUserId={}", pushdto.getUserId());
            } catch (Exception e) {
                log.error("Push 메시지 MQ 전송 실패: targetUserId={}, error={}", pushdto.getUserId(), e.getMessage());
                throw new RuntimeException("MQ 전송 실패", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
