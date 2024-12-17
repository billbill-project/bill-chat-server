package bill.chat.config.jwt;

import bill.chat.model.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Slf4j
@Component
public class JWTUtil {
    @Value("${jwt.secret-key}")
    private String SECRET_KEY;

    public static String MDC_USER_ID = "userId";
    public static String MDC_USER_ROLE = "role";

//    public JWTUtil(@Value("${jwt.secret-key}") String secretKey) {
//        this.SECRET_KEY = secretKey;
//    }

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

    public String putUserMDC(Claims claims) {
        String userId = claims.getSubject();
        String role = claims.get("role", String.class);

        MDC.put(MDC_USER_ID, userId);
        MDC.put(MDC_USER_ROLE, role);

        return userId;
    }

    public UserRole getUserRole(String token) {
        Claims claims = getClaims(token);
        String role = claims.get("role", String.class);
        return UserRole.valueOf(role);
    }

    public boolean isExpired(String token) {
        try {
            Claims claims = getClaims(token);
            if ("RT".equals(claims.get("type"))) {  // 문자열 비교는 .equals() 사용
                return false;
            }
        } catch (ExpiredJwtException e) {
            log.error("Bill Refresh 토큰 시간이 만료되었습니다. {}", e.getMessage());
            return true;
        }
        return false;
    }
}
