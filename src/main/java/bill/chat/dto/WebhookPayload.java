package bill.chat.dto;

import java.util.List;
import lombok.Getter;

public class WebhookPayload {
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
    }
}
