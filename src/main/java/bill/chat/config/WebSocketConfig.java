package bill.chat.config;

import bill.chat.service.CustomWebSocketService;
import bill.chat.websocket.handler.AuthenticatedWebSocketHandler;
import bill.chat.config.jwt.JWTUtil;
import bill.chat.repository.ChatRoomRepository;
import bill.chat.websocket.handler.MyWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    private final ChatRoomRepository chatRoomRepository;
    private final MyWebSocketHandler myWebSocketHandler;
    private final JWTUtil jwtUtil;

    @Bean
    public HandlerMapping handlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/greeting", new AuthenticatedWebSocketHandler(myWebSocketHandler, jwtUtil, chatRoomRepository));
        int order = -1;

        log.info("Registering WebSocket handler for /ws/greeting");
        return new SimpleUrlHandlerMapping(map, order);
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        WebSocketService webSocketService = new CustomWebSocketService(new ReactorNettyRequestUpgradeStrategy());
        return new WebSocketHandlerAdapter(webSocketService);
    }
}
