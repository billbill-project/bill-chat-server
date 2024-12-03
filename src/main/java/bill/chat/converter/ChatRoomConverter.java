package bill.chat.converter;

import bill.chat.dto.WebhookPayload;
import bill.chat.model.ChatRoom;
import bill.chat.model.Participant;
import java.util.Arrays;
import java.util.List;

public class ChatRoomConverter {
    public static ChatRoom toChatRoom(WebhookPayload.CreateChatRoom payload) {
        Participant owner = Participant.builder()
                .userId(payload.getOwnerId())
                .role("owner")
                .build();

        Participant contact = Participant.builder()
                .userId(payload.getContactId())
                .role("contact")
                .build();

        List<Participant> participants = Arrays.asList(owner, contact);

        return ChatRoom.builder()
                .channelId(payload.getChannelId())
                .participants(participants)
                .build();
    }
}
