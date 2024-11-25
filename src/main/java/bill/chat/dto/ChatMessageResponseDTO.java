package bill.chat.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ChatMessageResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class getChatMessage {
        String senderId;
        String content;
        boolean isImage;
        boolean isSystem;
        boolean isRead;
        LocalDateTime createdAt;
    }
}
