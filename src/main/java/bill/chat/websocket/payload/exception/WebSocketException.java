package bill.chat.websocket.payload.exception;

import bill.chat.websocket.payload.code.WebSocketErrorStatus;
import bill.chat.websocket.payload.dto.WebSocketFailureDTO;

public class WebSocketException extends RuntimeException {
    private final WebSocketErrorStatus status;

    public WebSocketException(WebSocketErrorStatus status) {
        this.status = status;
    }

    public WebSocketFailureDTO getFailureDTO() {
        return this.status.toErrorReason();
    }
}
