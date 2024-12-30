package bill.chat.converter;

import bill.chat.dto.ChatMessageResponseDTO;
import bill.chat.model.ChatMessage;
import bill.chat.model.enums.MessageType;
import bill.chat.model.enums.SystemType;
import java.time.LocalDate;

public class ChatMessageConverter {
    public static ChatMessage toChatMessage(String channelId, String senderId, String content, SystemType systemType,
                                            MessageType messageType,
                                            boolean isRead, LocalDate startedAt, LocalDate endedAt, Integer price) {
        return ChatMessage.builder()
                .channelId(channelId)
                .senderId(senderId)
                .content(content)
                .systemType(systemType)
                .startedAt(startedAt)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .price(price)
                .endedAt(endedAt)
                .price(price)
                .messageType(messageType)
                .isRead(isRead)
                .build();
    }

    public static ChatMessageResponseDTO.getChatMessage toGetChatMessage(ChatMessage chatMessage) {
        return ChatMessageResponseDTO.getChatMessage.builder()
                .systemType(chatMessage.getSystemType())
                .startedAt(chatMessage.getStartedAt())
                .endedAt(chatMessage.getEndedAt())
                .price(chatMessage.getPrice())
                .senderId(chatMessage.getSenderId())
                .content(chatMessage.getContent())
                .messageType(chatMessage.getMessageType())
                .isRead(chatMessage.isRead())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }
}
