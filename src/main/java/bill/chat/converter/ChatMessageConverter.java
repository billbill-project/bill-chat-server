package bill.chat.converter;

import bill.chat.model.ChatMessage;

public class ChatMessageConverter {
    public static ChatMessage toChatMessage(String channelId, String senderId, String content, boolean isImage, boolean isSystem,
                                            boolean isRead) {
        return ChatMessage.builder()
                .channelId(channelId)
                .senderId(senderId)
                .content(content)
                .isImage(isImage)
                .isSystem(isSystem)
                .isRead(isRead)
                .build();
    }
}
