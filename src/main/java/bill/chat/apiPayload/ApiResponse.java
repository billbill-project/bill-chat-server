package bill.chat.apiPayload;

import bill.chat.apiPayload.code.status.SuccessStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
@JsonPropertyOrder({"success", "code", "message", "responseAt", "data"})
public class ApiResponse <T> {
    @JsonProperty("isSuccess")
    private final Boolean success;
    private final String code;
    private final String message;
    @JsonProperty("responseAt")
    private final LocalDateTime responseAt;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    public static <T> ApiResponse<T> onSuccess(T data) {
        return new ApiResponse<>(true, SuccessStatus._OK.getCode(), SuccessStatus._OK.getMessage(), LocalDateTime.now(), data);
    }

    public static <T> ApiResponse<T> onFailure(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, LocalDateTime.now(), data);
    }
}
