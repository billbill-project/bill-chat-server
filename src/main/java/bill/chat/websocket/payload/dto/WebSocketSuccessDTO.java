package bill.chat.websocket.payload.dto;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebSocketSuccessDTO {
    private final String messageType;
    private final int chatRoomId;
    private final int senderId;
    private final String content;
    private final OffsetDateTime createdAt;
}
