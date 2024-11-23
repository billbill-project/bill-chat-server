package bill.chat.websocket.payload.code;

import bill.chat.websocket.payload.dto.WebSocketFailureDTO;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WebSocketErrorStatus {
    INVALID_MESSAGE_FORMAT("ws4001", "메세지 형식이 잘못되었습니다."),
    UNKNOWN_CHANNEL("ws4002", "존재하지 않는 채팅방입니다."),
    UNKNOWN_USER("ws4003", "채팅방에 참여할 수 없는 유저입니다."),
    DELETED_CHANNEL("ws4004", "채팅방이 삭제되었습니다."),
    CLOSED_CHANNEL("ws4005", "채팅방이 닫혀 더 이상 대화할 수 없습니다."),

    UNKNOWN_ERROR("ws5001", "알 수 없는 오류가 발생했습니다.");

    private final String code;
    private final String message;

    public WebSocketFailureDTO toErrorReason() {
        return WebSocketFailureDTO.builder()
                .errorCode(code)
                .errorMessage(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
