package bill.chat.websocket.payload.code;

import bill.chat.websocket.payload.dto.WebSocketFailureDTO;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WebSocketErrorStatus {
    INVALID_MESSAGE_FORMAT("ws4001", "메세지 형식이 잘못되었습니다."),
    INVALID_MESSAGE_TYPE("ws4002", "지원하지 않는 메세지 타입입니다."),

    UNKNOWN_ERROR("ws5001", "알 수 없는 오류가 발생했습니다.");

    private final String code;
    private final String message;

    public WebSocketFailureDTO toErrorReason() {
        return WebSocketFailureDTO.builder()
                .errorCode(code)
                .errorMessage(message)
                .timestamp(OffsetDateTime.now())
                .build();
    }
}
