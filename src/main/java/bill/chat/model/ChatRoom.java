package bill.chat.model;

import bill.chat.model.common.BaseEntity;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Document(collection = "chat_rooms")
public class ChatRoom extends BaseEntity {
    @Id
    private String id;

    private String channelId;
    private List<Participant> participants;
    private List<ChatMessage> chat;
    private int unreadCount;
    private boolean isClosed;
    private boolean isDeleted;
    private LocalDateTime deletedAt; // 채팅방 삭제 시간

    public void addUnreadCount() {
        unreadCount += 1;
    }

    public void addChatMessage(ChatMessage chatMessage) {
        chat.add(chatMessage);
    }
}
