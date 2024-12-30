package bill.chat.dto;

import bill.chat.model.enums.MessageType;
import bill.chat.model.enums.SystemType;
import java.time.LocalDate;
import lombok.Getter;

@Getter
public class ChatDTO {
    private MessageType messageType;
    private String channelId;
    private SystemType systemType;
    private String senderId;
    private String content; //이미지일때는 s3 주소
    private LocalDate startedAt;
    private LocalDate endedAt;
    private Integer price;

}
