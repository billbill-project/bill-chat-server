package bill.chat.controller;

import bill.chat.apiPayload.ApiResponse;
import bill.chat.converter.ChatMessageConverter;
import bill.chat.dto.ChatMessageResponseDTO;
import bill.chat.dto.ChatMessageResponseDTO.getChatMessage;
import bill.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    @PostMapping("/leave")
    public Mono<ApiResponse<Void>> chatRoomLeave(@RequestParam String channelId) {
        String userId = "user2";
        return chatService.processChatRoomLeave(channelId, userId)
                .then(Mono.just(ApiResponse.onSuccess(null)));

    }

//    @GetMapping("/info")
//    public Flux<ChatMessageResponseDTO.getChatMessage> getChatInfo(@RequestParam String channelId) {
//        //jwt에서 뽑아서 userId 가져옴
//        return chatService.getChatRoom(channelId)
//                .map(ChatMessageConverter::toGetChatMessage);
//    }
}
