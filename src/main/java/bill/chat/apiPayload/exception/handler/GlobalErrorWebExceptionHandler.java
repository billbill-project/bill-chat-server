package bill.chat.apiPayload.exception.handler;

import bill.chat.apiPayload.ApiResponse;
import bill.chat.apiPayload.code.status.ErrorStatus;
import bill.chat.apiPayload.exception.GeneralException;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalErrorWebExceptionHandler(ErrorAttributes errorAttributes,
                                          ApplicationContext applicationContext,
                                          ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, new WebProperties.Resources(), applicationContext);
        this.setMessageWriters(serverCodecConfigurer.getWriters());
        this.setMessageReaders(serverCodecConfigurer.getReaders());
    }


    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);

        ApiResponse<Object> response;
        HttpStatus status;

        if (error instanceof GeneralException generalException) {
            response = ApiResponse.onFailure(
                    generalException.getErrorReason().getCode(),
                    generalException.getErrorReason().getMessage(),
                    null
            );
            status = generalException.getErrorReason().getHttpStatus();
        } else {
            response = ApiResponse.onFailure(
                    ErrorStatus._INTERNAL_SERVER_ERROR.getCode(),
                    ErrorStatus._INTERNAL_SERVER_ERROR.getMessage(),
                    null
            );
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response);
    }

}


