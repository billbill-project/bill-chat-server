package bill.chat.config;

import bill.chat.config.interceptor.BillChatInterceptor;
import bill.chat.websocket.handler.MyWebSocketHandler;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

@Configuration
public class WebSocketConfig {

    private final MyWebSocketHandler myWebSocketHandler;
    private final BillChatInterceptor billChatInterceptor;

    // MyWebSocketHandler를 생성자 주입으로 받아옴
    public WebSocketConfig(MyWebSocketHandler myWebSocketHandler, BillChatInterceptor billChatInterceptor) {
        this.myWebSocketHandler = myWebSocketHandler;
        this.billChatInterceptor = billChatInterceptor;
    }

    @Bean
    public HandlerMapping handlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/greeting", myWebSocketHandler);

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(1);

        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
