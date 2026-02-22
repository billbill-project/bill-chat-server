package bill.chat.websocket.payload.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebSocketErrorDTO {
    private String type; // "ERROR"
    private String channelId;
    private String senderId;
    private String content;
    private String errorMessage;
}
