package bill.chat.converter;

import bill.chat.dto.ChatRoomResponseDTO;
import bill.chat.dto.ChatRoomResponseDTO.getChatInfo;
import bill.chat.dto.ChatServerPayload.CreateChatRoomPayload;
import bill.chat.model.ChatRoom;
import bill.chat.model.Participant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChatRoomConverter {
    public static ChatRoom toChatRoom(CreateChatRoomPayload payload) {
        Participant owner = Participant.builder()
                .userId(payload.getOwnerId())
                .role("owner")
                .notification(true)
                .build();

        Participant contact = Participant.builder()
                .userId(payload.getContactId())
                .role("contact")
                .notification(true)
                .build();

        List<Participant> participants = Arrays.asList(owner, contact);

        return ChatRoom.builder()
                .channelId(payload.getChannelId())
                .participants(participants)
                .build();
    }

    public static ChatRoomResponseDTO.getChatInfoList toGetChatInfoList(List<ChatRoom> chatRooms, String userId) {
        List<getChatInfo> getChatInfoList = chatRooms.stream().map(c -> toGetChatInfo(c, userId))
                .collect(Collectors.toList());

        return ChatRoomResponseDTO.getChatInfoList.builder()
                .chatInfoList(getChatInfoList)
                .build();
    }

    public static ChatRoomResponseDTO.getChatInfo toGetChatInfo(ChatRoom chatRoom, String userId) {
        Participant participant = null;
        for (Participant p : chatRoom.getParticipants()) {
            if (p.getUserId().equals(userId)) {
                participant = p;
                break;
            }
        }

        return ChatRoomResponseDTO.getChatInfo.builder()
                .notification(participant.isNotification())
                .channelId(chatRoom.getChannelId())
                .unreadCount(chatRoom.getUnreadCount())
                .lastChat(chatRoom.getLastChat())
                .lastSender(chatRoom.getLastSender())
                .updatedAt(chatRoom.getUpdatedAt())
                .build();
    }
}
