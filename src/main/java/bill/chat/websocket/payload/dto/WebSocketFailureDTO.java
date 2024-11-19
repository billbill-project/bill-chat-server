package bill.chat.websocket.payload.dto;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebSocketFailureDTO {
    private final String messageType = "ERROR";
    private final String errorCode;
    private final String errorMessage;
    private final OffsetDateTime timestamp;
}
