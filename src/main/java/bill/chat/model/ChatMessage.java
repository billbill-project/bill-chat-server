package bill.chat.model;

import bill.chat.model.common.BaseEntity;
import lombok.Builder;
import lombok.Getter;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Builder
@Document(collection = "chat_messages")
public class ChatMessage extends BaseEntity {
    @Id
    private String id;

    private String channelId;
    private String senderId;
    private String content;
    private boolean isImage;
    private boolean isSystem;
    private boolean isRead;

    public void changeRead() {
        isRead = true;
    }
}
