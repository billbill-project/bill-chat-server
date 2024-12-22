package bill.chat.config;

import bill.chat.config.interceptor.BillChatInterceptor;
import bill.chat.websocket.handler.MyWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final MyWebSocketHandler myWebSocketHandler;
    private final BillChatInterceptor billChatInterceptor;

    private static final String ENDPOINT = "/ws";
    private static final String SIMPLE_BROKER = "/topic";
    private static final String PUBLISH = "/app";

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

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(SIMPLE_BROKER);
        registry.setApplicationDestinationPrefixes(PUBLISH);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(ENDPOINT)
                .setAllowedOriginPatterns("*");
    }

    // https://shout-to-my-mae.tistory.com/430 참고
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(billChatInterceptor);
    }
}
