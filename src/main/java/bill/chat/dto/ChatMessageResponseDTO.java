package bill.chat.dto;

import bill.chat.model.Participant;
import bill.chat.model.enums.SystemType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ChatMessageResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class getChatMessage {
        String senderId;
        String content;
        SystemType systemType;
        boolean isImage;
        boolean isSystem;
        boolean isRead;
        LocalDateTime createdAt;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class getChatRoomInfo {
        String channelId;
        String myRole; // sender 인지 receiver 인지 (거래진행 버튼)
        String opponentId; // 눌렀을 때 프로필
        boolean opponentIsLeft;
        String opponentNickname;
        String opponentProfileImageUrl;
        boolean opponentIsDeleted; //탈퇴했는지
        List<Participant> participants; // 닉네임, 프로필 url
        int unreadCount;
        boolean isClosed;
        boolean isDeleted;
        LocalDateTime deletedAt;
        String title;
        String itemImage;
        int itemPrice;
        String itemState;
    }
}
