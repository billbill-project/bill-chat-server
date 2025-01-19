package bill.chat.dto;

import java.util.List;
import lombok.Getter;

public class ChatServerPayload {
    @Getter
    public static class CreateChatRoomPayload {
        String channelId;
        String contactId;
        String ownerId;
    }

    @Getter
    public static class GetChatListPayload {
        String beforeTimestamp;
        List<String> chatRoomIds;
        String userId;
    }

    @Getter
    public static class GetUnreadCountPayload {
        String userId;
        List<String> chatRoomIds;
    }
}
