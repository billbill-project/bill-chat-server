package bill.chat.apiPayload.code.status;

import bill.chat.apiPayload.code.BaseErrorCode;
import bill.chat.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    //일반적인 에러
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "MEMBER4001", "JWT 토큰이 유효하지 않습니다."),

    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON400","잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"COMMON401","인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),

    NOT_PARTICIPANT(HttpStatus.BAD_REQUEST, "CHANNEL4001", "해당 채팅방의 참여자가 아닙니다."),
    NOT_FOUND_CHANNEL(HttpStatus.BAD_REQUEST, "CHANNEL4002", "존재하지 않는 채팅방입니다."),

    DUPLICATION_SUBSCRIBE(HttpStatus.BAD_REQUEST, "CHANNEL4003", "SSE 중복 구독 요청입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .success(false)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .success(false)
                .httpStatus(httpStatus)
                .build();
    }
}