package bill.chat.controller;

import bill.chat.converter.ChatRoomConverter;
import bill.chat.dto.ChatRoomResponseDTO;
import bill.chat.dto.WebhookPayload.CreateChatRoomPayload;
import bill.chat.dto.WebhookPayload.GetChatListPayload;
import bill.chat.dto.WebhookPayload.GetUnreadCountPayload;
import bill.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1")
public class webhookController {
    private final ChatService chatService;

    @PostMapping("/channel")
    public Mono<ResponseEntity<String>> createChatRoom(@RequestBody CreateChatRoomPayload payload) {
        return chatService.createChatRoom(payload)
                .then(Mono.just(ResponseEntity.ok("success")));
    }

    @PostMapping("/chat/list")
    public Mono<ChatRoomResponseDTO.getChatInfoList> getChatList(@RequestBody GetChatListPayload payload) {
        return chatService.getChatList(payload)
                .collectList()
                .map(ChatRoomConverter::toGetChatInfoList);
    }

    @PostMapping("/chat/unreadCount")
    public Mono<Integer> getUnreadCount(@RequestBody GetUnreadCountPayload payload) {
        return chatService.getUnreadChatCount(payload);
    }
}
