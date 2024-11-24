package bill.chat.websocket.payload.handler;

import bill.chat.dto.ChatDTO;
import bill.chat.websocket.payload.dto.WebSocketSuccessDTO;
import java.time.LocalDateTime;

public class WebSocketSuccessConverter {
    public WebSocketSuccessDTO toSuccessDTO(ChatDTO chatDTO, LocalDateTime time) {
        return WebSocketSuccessDTO.builder()
                .messageType(chatDTO.getMessageType())
                .channelId(chatDTO.getChannelId())
                .systemType(chatDTO.getSystemType())
                .senderId(chatDTO.getSenderId())
                .content(chatDTO.getContent())
                .createdAt(time)
                .build();
    }
}
