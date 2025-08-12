package bill.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PushDTO {
    private String userId;
    private String senderId;
    private String channelId;
    private String lastContent;
}