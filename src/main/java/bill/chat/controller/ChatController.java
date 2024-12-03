package bill.chat.controller;

import bill.chat.apiPayload.ApiResponse;
import bill.chat.converter.ChatMessageConverter;
import bill.chat.dto.ChatMessageResponseDTO;
import bill.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {
    private final ChatService chatService;

    @GetMapping("/messages")
    public Flux<ApiResponse<ChatMessageResponseDTO.getChatMessage>> getChatMessages(@RequestParam String channelId,
                                                                                    @RequestParam(required = false) String beforeTimestamp) {
        return chatService.getChatMessages(channelId, beforeTimestamp)
                .map(ChatMessageConverter::toGetChatMessage)
                .map(ApiResponse::onSuccess);
    }

    //TODO: api 서버에서 유효한 채팅방 list 받은다음에 여기서 페이징 후 전달?
}
