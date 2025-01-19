package bill.chat.converter;

import bill.chat.dto.SSEDTO;
import bill.chat.model.ChatRoom;
import bill.chat.model.Participant;

public class SSEConverter {
    public static SSEDTO toSSEDTO(ChatRoom chatRoom, Participant participant) {
        return SSEDTO.builder()
                .content(chatRoom.getLastChat())
                .senderId(chatRoom.getLastSender())
                .channelId(chatRoom.getChannelId())
                .unreadCount(chatRoom.getUnreadCount())
                .updatedAt(chatRoom.getUpdatedAt())
                .notification(participant.isNotification())
                .build();
    }
}
