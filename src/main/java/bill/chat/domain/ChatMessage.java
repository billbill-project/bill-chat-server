package bill.chat.domain;

import java.time.OffsetDateTime;

public class ChatMessage {
    private final OffsetDateTime createdAt = OffsetDateTime.now();

    private int seq;
    private String senderId;
    private String content;
    private boolean isImage;
    private boolean isSystem;
    private boolean isRead;
}
