package bill.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SSEDTO {
    String channelId;
    String senderId;
    String content;
    int unreadCount;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime updatedAt;

    public SSEDTO(String channelId, String senderId, String content, int unreadCount, LocalDateTime updatedAt) {
        this.channelId = channelId;
        this.senderId = senderId;
        this.content = content;
        this.unreadCount = unreadCount;
        this.updatedAt = updatedAt;
    }
}
