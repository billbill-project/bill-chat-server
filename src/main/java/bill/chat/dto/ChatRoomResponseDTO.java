package bill.chat.dto;

import bill.chat.model.ChatMessage;
import bill.chat.model.Participant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ChatRoomResponseDTO {
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetChatListDTO {
        String channelId;
        List<Participant> participants;
        List<ChatMessage> chatMessageList;
        boolean isClosed;
        boolean isDeleted;
    }
}
