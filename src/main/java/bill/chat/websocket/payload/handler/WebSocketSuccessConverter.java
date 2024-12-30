package bill.chat.websocket.payload.handler;

import bill.chat.dto.ChatDTO;
import bill.chat.websocket.payload.dto.WebSocketSuccessDTO;
import java.time.LocalDateTime;

public class WebSocketSuccessConverter {
    public WebSocketSuccessDTO toSuccessDTO(ChatDTO chatDTO, LocalDateTime time, boolean read) {
        return WebSocketSuccessDTO.builder()
                .messageType(chatDTO.getMessageType())
                .systemType(chatDTO.getSystemType())
                .startedAt(chatDTO.getStartedAt())
                .endedAt(chatDTO.getEndedAt())
                .price(chatDTO.getPrice())
                .senderId(chatDTO.getSenderId())
                .content(chatDTO.getContent())
                .read(read)
                .createdAt(time)
                .build();
    }
}
