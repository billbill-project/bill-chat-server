package bill.chat.config.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
public class JWTUtil {
    @Value("${jwt.secret-key}")
    private String SECRET_KEY;

    public Mono<Boolean> isValidAccessTokenReactive(String token) {
        return Mono.fromCallable(() -> isValidAccessToken(token))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Claims> getClaimsReactive(String token) {
        return Mono.fromCallable(() -> getClaims(token))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public boolean isValidAccessToken(String token) {
        try {
            if (getClaims(token).get("type").equals("AT")) return true;
        } catch (ExpiredJwtException e) {
            log.error("Bill Access 토큰 시간이 만료 되었습니다. {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("Bill Access 토큰 헤더값이 유효하지 않습니다. {}", e.getMessage());
            return false;
        }

        return false;
    }

    public Claims getClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
