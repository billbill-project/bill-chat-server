package bill.chat.controller;

import bill.chat.dto.WebhookPayload;
import bill.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook")
public class webhookController {
    private final ChatService chatService;
    //TODO: api 서버에서 채팅방 생성 시 여기서 chatRoom 생성
    @PostMapping("")
    public Mono<ResponseEntity<String>> createChatRoom(@RequestBody WebhookPayload.CreateChatRoom payload) {
        return chatService.createChatRoom(payload)
                .then(Mono.just(ResponseEntity.ok("success")));
    }
}
