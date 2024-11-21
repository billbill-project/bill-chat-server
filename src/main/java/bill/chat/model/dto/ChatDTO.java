package bill.chat.model.dto;

import bill.chat.model.enums.MessageType;
import bill.chat.model.enums.SystemType;
import lombok.Getter;

@Getter
public class ChatDTO {
    private MessageType messageType;
    private String channelId;
    private SystemType systemType;
    private String senderId;
    private String content; //이미지일때는 s3 주소
}
