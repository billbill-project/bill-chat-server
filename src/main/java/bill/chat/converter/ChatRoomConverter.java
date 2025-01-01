package bill.chat.converter;

import bill.chat.dto.ChatRoomResponseDTO;
import bill.chat.dto.ChatRoomResponseDTO.getChatInfo;
import bill.chat.dto.WebhookPayload.CreateChatRoomPayload;
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
                .build();

        Participant contact = Participant.builder()
                .userId(payload.getContactId())
                .role("contact")
                .build();

        List<Participant> participants = Arrays.asList(owner, contact);

        return ChatRoom.builder()
                .channelId(payload.getChannelId())
                .participants(participants)
                .build();
    }

    public static ChatRoomResponseDTO.getChatInfoList toGetChatInfoList(List<ChatRoom> chatRooms) {
        List<getChatInfo> getChatInfoList = chatRooms.stream().map(ChatRoomConverter::toGetChatInfo)
                .collect(Collectors.toList());

        return ChatRoomResponseDTO.getChatInfoList.builder()
                .chatInfoList(getChatInfoList)
                .build();
    }

    public static ChatRoomResponseDTO.getChatInfo toGetChatInfo(ChatRoom chatRoom) {
        return ChatRoomResponseDTO.getChatInfo.builder()
                .channelId(chatRoom.getChannelId())
                .unreadCount(chatRoom.getUnreadCount())
                .lastChat(chatRoom.getLastChat())
                .lastSender(chatRoom.getLastSender())
                .updatedAt(chatRoom.getUpdatedAt())
                .build();
    }
}
