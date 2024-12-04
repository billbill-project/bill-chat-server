package bill.chat.config.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtDto {
    private String accessToken;
    private String refreshToken;
    private String grantType;
    private Long expiresIn;
    private String role;
}
