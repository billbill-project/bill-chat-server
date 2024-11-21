package bill.chat.websocket.payload.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebSocketFailureDTO {
    private final String messageType = "ERROR";
    private final String errorCode;
    private final String errorMessage;
    private final LocalDateTime timestamp;
}
