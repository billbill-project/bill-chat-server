package bill.chat.rabbitMQ;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bill.chat.dto.ChatDTO;
import bill.chat.model.ChatRoom;
import bill.chat.model.Participant;
import bill.chat.repository.ChatMessageRepository;
import bill.chat.repository.ChatRoomRepository;
import bill.chat.service.DistributedSSEManager;
import bill.chat.service.DistributedSessionManager;
import bill.chat.websocket.payload.handler.WebSocketSuccessConverter;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@Disabled("Regression test for pre-fix behavior (dc94436)")
class ChatProcessorLossScenarioTest {

    @Mock
    private Producer producer;
    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private DistributedSessionManager distributedSessionManager;
    @Mock
    private WebSocketSuccessConverter webSocketSuccessConverter;
    @Mock
    private DistributedSSEManager distributedSSEManager;

    @InjectMocks
    private ChatProcessor chatProcessor;

    @Test
    void listenerReturnsOnDbSaveFailure() {
        ChatDTO chatDTO = new ChatDTO();
        chatDTO.setChannelId("channel-1");
        chatDTO.setSenderId("user-1");
        chatDTO.setContent("hello");

        ChatRoom chatRoom = ChatRoom.builder()
                .channelId("channel-1")
                .participants(List.of(
                        Participant.builder().userId("user-1").notification(true).build(),
                        Participant.builder().userId("user-2").notification(true).build()))
                .build();

        when(chatRoomRepository.findByChannelId("channel-1")).thenReturn(Mono.just(chatRoom));
        when(distributedSessionManager.getActiveUserCount("channel-1")).thenReturn(Mono.just(1L));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(Mono.just(chatRoom));
        when(chatMessageRepository.save(any())).thenReturn(Mono.error(new RuntimeException("db down")));

        assertDoesNotThrow(() -> chatProcessor.processAndSaveChatMessage(chatDTO));

        verify(chatMessageRepository).save(any());
        verify(producer, never()).broadcastProcessedMessage(any());
    }
}
