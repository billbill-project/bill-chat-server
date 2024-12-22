package bill.chat.dto;

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
    LocalDateTime updatedAt;

    public SSEDTO(String channelId, String senderId, String content, int unreadCount, LocalDateTime updatedAt) {
        this.channelId = channelId;
        this.senderId = senderId;
        this.content = content;
        this.unreadCount = unreadCount;
        this.updatedAt = updatedAt;
    }
}
