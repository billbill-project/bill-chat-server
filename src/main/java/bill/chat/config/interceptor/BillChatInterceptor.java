package bill.chat.config.interceptor;

import bill.chat.apiPayload.code.status.ErrorStatus;
import bill.chat.apiPayload.exception.handler.MemberHandler;
import bill.chat.config.jwt.JWTUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillChatInterceptor implements WebFilter {

    private final JWTUtil jwtUtil;
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/ws/",
            "/webhook/",
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
                    throw new MemberHandler(ErrorStatus.INVALID_TOKEN);
                }

                String subString = header.substring(7);
                if (!StringUtils.hasText(subString)) {
                    throw new MemberHandler(ErrorStatus.INVALID_TOKEN);
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

        String token = resolveToken(exchange);

        if (token != null && jwtUtil.isValidAccessToken(token)) {
            String userId = jwtUtil.putUserMDC(jwtUtil.getClaims(token));
            log.info("UserRole : {}", jwtUtil.getUserRole(token));
            log.info("REQUEST [{}][{}] : auth by user {}", uuid, requestURI, userId);

            return chain.filter(exchange);
        }

        log.info("REQUEST [{}][{}] : no auth by user", uuid, requestURI);

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}

