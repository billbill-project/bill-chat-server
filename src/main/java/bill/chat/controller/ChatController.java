package bill.chat.controller;

import bill.chat.apiPayload.ApiResponse;
import bill.chat.apiPayload.code.status.ErrorStatus;
import bill.chat.apiPayload.exception.GeneralException;
import bill.chat.converter.ChatMessageConverter;
import bill.chat.dto.ChatMessageResponseDTO.getChatMessage;
import bill.chat.dto.SSEDTO;
import bill.chat.service.ChatService;
import bill.chat.service.SSEManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
@Slf4j
public class ChatController {
    private final ChatService chatService;
    private final SSEManager sseManager;

    @GetMapping("/messages")
    public Mono<ApiResponse<List<getChatMessage>>> getChatMessages(@RequestParam String channelId,
                                                                   @RequestParam(required = false) String beforeTimestamp) {
        return Mono.deferContextual(ctx -> {
            String userId = ctx.get("userId");
            return chatService.getChatMessages(channelId, beforeTimestamp, userId)
                    .map(ChatMessageConverter::toGetChatMessage)
                    .collectList()
                    .map(ApiResponse::onSuccess);
        });
    }

    @GetMapping(value = "/list/SSE", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<SSEDTO> subscribeSSE() {
        return Flux.deferContextual(ctx -> {
            String userId = ctx.get("userId");
            try {
                return sseManager.getOrManageSink(userId).asFlux()
                        .doFinally(signal -> sseManager.removeSink(userId));
            } catch (IllegalStateException e) {
                log.warn("중복 구독 시도: {}", userId);
                return Flux.error(new GeneralException(ErrorStatus.DUPLICATION_SUBSCRIBE));
            }
        });
    }

    @PatchMapping("/{channelId}/notification")
    public Mono<ApiResponse<String>> changeNotificationStatus(@PathVariable String channelId) {
        return Mono.deferContextual(ctx -> {
            String userId = ctx.get("userId");
            return chatService.changeNotificationStatus(channelId, userId)
                    .thenReturn(ApiResponse.onSuccess("success"));
        });
    }
}
