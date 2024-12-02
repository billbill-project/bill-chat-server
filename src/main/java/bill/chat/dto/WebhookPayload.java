package bill.chat.dto;

import lombok.Getter;

public class WebhookPayload {
    @Getter
    public static class CreateChatRoom {
        String channelId;
        String contactId;
        String ownerId;
    }
}
