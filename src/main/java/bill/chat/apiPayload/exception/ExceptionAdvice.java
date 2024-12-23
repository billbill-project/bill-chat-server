package bill.chat.apiPayload.exception;

import bill.chat.apiPayload.ApiResponse;
import bill.chat.apiPayload.code.ErrorReasonDTO;
import bill.chat.apiPayload.code.status.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleGeneralException(Exception e) {
        log.error("Handling GeneralException: {}", e.getMessage());
        ApiResponse<Object> response = ApiResponse.onFailure(
                ErrorStatus._INTERNAL_SERVER_ERROR.getCode(),
                ErrorStatus._INTERNAL_SERVER_ERROR.getMessage(),
                null
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
    }

    @ExceptionHandler(GeneralException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleCustomException(GeneralException e) {
        log.error("Handling Exception: {}", e.getMessage());
        ErrorReasonDTO reason = e.getErrorReason();
        ApiResponse<Object> response = ApiResponse.onFailure(
                reason.getCode(),
                reason.getMessage(),
                null
        );

        return Mono.just(ResponseEntity.status(reason.getHttpStatus()).body(response));
    }
}