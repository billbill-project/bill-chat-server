package bill.chat.websocket.payload.dto;

import bill.chat.model.enums.MessageType;
import bill.chat.model.enums.SystemType;
import io.netty.channel.ChannelId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebSocketSuccessDTO {
    private String channelId;
    private MessageType messageType;
    private SystemType systemType;
    private LocalDate startedAt;
    private LocalDate endedAt;
    private Integer price;
    private String senderId;
    private String content;
    private boolean read;
    private LocalDateTime createdAt;
}
