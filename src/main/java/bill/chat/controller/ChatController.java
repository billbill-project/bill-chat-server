package bill.chat.controller;

import bill.chat.apiPayload.ApiResponse;
import bill.chat.config.jwt.JWTUtil;
import bill.chat.converter.ChatMessageConverter;
import bill.chat.dto.ChatMessageResponseDTO;
import bill.chat.dto.SSEDTO;
import bill.chat.service.ChatService;
import bill.chat.service.SSEManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
@Slf4j
public class ChatController {
    private final ChatService chatService;
    private final SSEManager sseManager;

    @GetMapping("/messages")
    public Flux<ApiResponse<ChatMessageResponseDTO.getChatMessage>> getChatMessages(@RequestParam String channelId,
                                                                                    @RequestParam(required = false) String beforeTimestamp) {
        String userId = MDC.get(JWTUtil.MDC_USER_ID).toString();
        return chatService.getChatMessages(channelId, beforeTimestamp, userId)
                .map(ChatMessageConverter::toGetChatMessage)
                .map(ApiResponse::onSuccess);
    }

    @GetMapping(value = "/list/SSE", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<SSEDTO> subscribeSSE() {
        String userId = MDC.get(JWTUtil.MDC_USER_ID).toString();
        if (sseManager.doesSinkExist(userId)) {
            log.warn("이미 구독 중인 사용자: {}", userId);
            return Flux.error(new IllegalStateException("이미 구독 중인 사용자입니다."));
        }

        return sseManager.getOrManageSink(userId)
                .asFlux();
    }

}
