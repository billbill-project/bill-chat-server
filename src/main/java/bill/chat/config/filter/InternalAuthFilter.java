package bill.chat.config.filter;

import bill.chat.apiPayload.code.status.ErrorStatus;
import bill.chat.apiPayload.exception.GeneralException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class InternalAuthFilter implements WebFilter {

    @Value("${internal.chat-secret}")
    private String expectedSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (!path.startsWith("/internal/v1")) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst("secretKey");

        if (!expectedSecret.equals(header)) {
            return Mono.error(new GeneralException(ErrorStatus._FORBIDDEN));
        }

        return chain.filter(exchange);
    }
}

