package bill.chat.converter;

import bill.chat.dto.SSEDTO;
import bill.chat.model.ChatRoom;

public class SSEConverter {
    public static SSEDTO toSSEDTO(ChatRoom chatRoom) {
        return SSEDTO.builder()
                .content(chatRoom.getLastChat())
                .senderId(chatRoom.getLastSender())
                .channelId(chatRoom.getChannelId())
                .unreadCount(chatRoom.getUnreadCount())
                .updatedAt(chatRoom.getUpdatedAt())
                .build();
    }
}
