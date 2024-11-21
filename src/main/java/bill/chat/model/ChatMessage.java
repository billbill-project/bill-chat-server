package bill.chat.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessage {
    @Builder.Default
    private final LocalDateTime createdAt = LocalDateTime.now();

    private int seq;
    private String senderId;
    private String content;
    private boolean isImage;
    private boolean isSystem;
    private boolean isRead;
}
