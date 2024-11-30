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
    private int unreadCount;
    private boolean isClosed;
    private boolean isDeleted;
    private LocalDateTime deletedAt; // 채팅방 삭제 시간

    public boolean getIsClosed() {
        return isClosed;
    }

    public boolean getIsDeleted() {
        return isDeleted;
    }

    public void addUnreadCount() {
        unreadCount += 1;
    }

//    public void addChatMessage(ChatMessage chatMessage) {
//        chat.add(chatMessage);
//    }

    public void resetUnreadCount() {
        unreadCount = 0;
    }

    public void processLeftUser(String senderId) {
        isClosed = true;
        participants.forEach(participant -> {
            if (participant.getUserId().equals(senderId)) {
                participant.updateLeft();
            }
        });
    }

    public void checkAndUpdateDelete() {
        boolean allLeft = true;

        for (Participant participant : participants) {
            if (!participant.isLeft()) {
                allLeft = false;
                break;
            }
        }

        if (allLeft) {
            isDeleted = true;
        }
    }
}
