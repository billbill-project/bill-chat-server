package bill.chat.domain;

import bill.chat.domain.common.BaseEntity;
import java.time.OffsetDateTime;
import java.util.List;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
    private OffsetDateTime deletedAt; // 채팅방 삭제 시간
}
