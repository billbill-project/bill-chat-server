package bill.chat.controller;

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
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;

    @GetMapping("/messages")
    public Flux<ChatMessageResponseDTO.getChatMessage> getChatMessages(@RequestParam String channelId,
                                                                       @RequestParam(required = false) String beforeTimestamp) {
        return chatService.getChatMessages(channelId, beforeTimestamp)
                .map(ChatMessageConverter::toGetChatMessage);
    }
}
