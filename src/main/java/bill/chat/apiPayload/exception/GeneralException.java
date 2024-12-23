package bill.chat.apiPayload.exception;

import bill.chat.apiPayload.code.BaseErrorCode;
import bill.chat.apiPayload.code.ErrorReasonDTO;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {
    private BaseErrorCode code;

    public GeneralException(BaseErrorCode errorCode) {
        super(errorCode.getReason().getMessage()); // 메시지를 설정
        this.code = errorCode;
    }

    public ErrorReasonDTO getErrorReason() {
        return this.code.getReason();
    }
}
