package bill.chat.config.interceptor;

import bill.chat.config.jwt.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillChatInterceptor implements ChannelInterceptor {
    private final JWTUtil jwtUtil;

    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER_ = "Bearer ";

    private Authentication createAuthentication(String userId, String userRole) {
        GrantedAuthority authority = new SimpleGrantedAuthority(userRole);
        return new UsernamePasswordAuthenticationToken(userId, null, Collections.singletonList(authority));
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        Optional<String> jwtTokenOptional = Optional.ofNullable(accessor.getFirstNativeHeader(AUTHORIZATION));
        String jwtToken = jwtTokenOptional
                .filter(token -> token.startsWith(BEARER_))
                .map(token -> token.substring(BEARER_.length()))
                .filter(token -> !jwtUtil.isExpired(token))
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        String userId = jwtUtil.putUserMDC(jwtUtil.getClaims(jwtToken));
        String userRole = jwtUtil.getUserRole(jwtToken).name();

        Authentication authentication = createAuthentication(userId, userRole);
        accessor.setUser(authentication);

        return message;
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        System.out.println("Post Send: " + message);
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        System.out.println("After Send Completion: " + message);
        if (ex != null) {
            System.err.println("Exception during sending: " + ex.getMessage());
        }
    }

    @Override
    public boolean preReceive(MessageChannel channel) {
        System.out.println("Pre Receive");
        return true; // false를 반환하면 메시지 수신이 중단됩니다.
    }

    @Override
    public Message<?> postReceive(Message<?> message, MessageChannel channel) {
        System.out.println("Post Receive: " + message);
        return message; // 메시지를 수정 가능
    }

    @Override
    public void afterReceiveCompletion(Message<?> message, MessageChannel channel, Exception ex) {
        System.out.println("After Receive Completion: " + message);
    }
}