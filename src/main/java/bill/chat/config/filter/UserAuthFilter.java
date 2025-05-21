package bill.chat.config.filter;

import bill.chat.apiPayload.code.status.ErrorStatus;
import bill.chat.apiPayload.exception.GeneralException;
import bill.chat.config.jwt.JWTUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;
import reactor.util.context.Context;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAuthFilter implements WebFilter {

    private final JWTUtil jwtUtil;
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/ws/",
            "/internal/v1/",
            "/swagger-ui/",
            "/v3/api-docs/",
            "/swagger-ui.html",
            "/docs/"
    );

    private String resolveToken(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (header != null) {
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                // Check if the header format is correct
                if (header.length() < 8) {
                    throw new GeneralException(ErrorStatus.INVALID_TOKEN);
                }

                String subString = header.substring(7);
                if (!StringUtils.hasText(subString)) {
                    throw new GeneralException(ErrorStatus.INVALID_TOKEN);
                }

                return subString;
            }
        }
        return null;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestURI = exchange.getRequest().getURI().getPath();
        String uuid = UUID.randomUUID().toString();

        for (String excludedPath : EXCLUDED_PATHS) {
            if (requestURI.startsWith(excludedPath)) {
                log.info("REQUEST [{}][{}]: bypassed by BillChatInterceptor", uuid, requestURI);
                return chain.filter(exchange);
            }
        }

        exchange.getAttributes().put("LOG_ID", uuid);

        if (HttpMethod.OPTIONS.matches(String.valueOf(exchange.getRequest().getMethod()))) {
            log.info("Pass OPTIONS method");
            return chain.filter(exchange);
        }

        return Mono.justOrEmpty(resolveToken(exchange))
                .flatMap(token -> jwtUtil.isValidAccessTokenReactive(token)
                        .flatMap(isValid -> {
                            if (!isValid) {
                                return Mono.error(new GeneralException(ErrorStatus.INVALID_TOKEN));
                            }
                            return jwtUtil.getClaimsReactive(token)
                                    .flatMap(claims -> {
                                        String userId = claims.getSubject();
                                        String role = claims.get("role", String.class);
                                        log.info("UserRole : {}", role);
                                        log.info("REQUEST [{}][{}] : auth by user {}", uuid, requestURI, userId);
                                        return chain.filter(exchange)
                                                .contextWrite(Context.of("userId", userId, "role", role));
                                    });
                        }))
                .onErrorResume(e -> {
                    if (e instanceof GeneralException) {
                        return Mono.error(e);
                    }
                    log.info("REQUEST [{}][{}]: no auth by user", uuid, requestURI);
                    return Mono.error(new GeneralException(ErrorStatus.INVALID_TOKEN));
                });
    }
}

