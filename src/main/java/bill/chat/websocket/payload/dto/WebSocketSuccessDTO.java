package bill.chat.websocket.payload.dto;

import bill.chat.model.enums.MessageType;
import bill.chat.model.enums.SystemType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebSocketSuccessDTO {
    private final MessageType messageType;
    private final String channelId;
    private SystemType systemType;
    private final String senderId;
    private final String content;
    private final LocalDateTime createdAt;
}
