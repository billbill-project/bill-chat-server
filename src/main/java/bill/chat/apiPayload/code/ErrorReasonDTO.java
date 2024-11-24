package bill.chat.apiPayload.code;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
public class ErrorReasonDTO {

    private HttpStatus httpStatus;

    private final boolean success;
    private final String code;
    private final String message;

    public boolean getIsSuccess(){return success;}
}