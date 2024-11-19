package bill.chat.websocket.payload.handler;

import bill.chat.websocket.payload.dto.WebSocketSuccessDTO;
import java.time.OffsetDateTime;

public class WebSocketSuccessConverter {
    public WebSocketSuccessDTO toSuccessDTO(String messageType, int chatRoomId, int senderId, String content, OffsetDateTime time) {
        return WebSocketSuccessDTO.builder()
                .messageType(messageType)
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(content)
                .createdAt(time)
                .build();
    }
}
