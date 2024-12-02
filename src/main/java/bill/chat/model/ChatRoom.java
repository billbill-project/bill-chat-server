package bill.chat.model;

import bill.chat.model.common.BaseEntity;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder
@Getter
@Document(collection = "chat_rooms")
public class ChatRoom extends BaseEntity {
    @Id
    private String id;

    private String channelId;
    private List<Participant> participants;
    private int unreadCount;
    @Builder.Default
    private String lastChat = "";
    @Builder.Default
    private String lastSender = "";

    public void addUnreadCount() {
        unreadCount += 1;
    }

    public void resetUnreadCount() {
        unreadCount = 0;
    }

    public void updateSender(String senderId) {
        lastSender = senderId;
    }

    public void updateLastMessage(String lastContent) {
        lastChat = lastContent;
    }
}
