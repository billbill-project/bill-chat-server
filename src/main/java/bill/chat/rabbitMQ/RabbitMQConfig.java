package bill.chat.rabbitMQ;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
public class RabbitMQConfig {
    @Value("${spring.rabbitmq.host}")
    private String host;
    @Value("${spring.rabbitmq.username}")
    private String username;
    @Value("${spring.rabbitmq.password}")
    private String password;
    @Value("${spring.rabbitmq.port}")
    private int port;

    // 채팅 메시지 브로드캐스트용
    @Bean
    public FanoutExchange chatExchange() {
        return new FanoutExchange("chat.exchange");
    }

    // SSE 메시지 전송용
    @Bean
    public TopicExchange sseExchange() {
        return new TopicExchange("sse.exchange");
    }

    // Push 알림 메시지 전송용
    @Bean
    public TopicExchange pushExchange() {
        return new TopicExchange("push.exchange");
    }

    // 메세지 저장용
    @Bean
    public DirectExchange chatProcessingExchange() {
        return new DirectExchange("chat.processing.exchange");
    }

    @Bean
    public Queue chatQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue sseQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Queue pushQueue() {
        return new Queue("push.queue", true);
    }

    @Bean
    public Queue chatProcessingQueue() {
        return new Queue("chat.processing.queue", true);
    }

    @Bean
    public Binding chatProcessingBinding(Queue chatProcessingQueue, DirectExchange chatProcessingExchange) {
        return BindingBuilder.bind(chatProcessingQueue).to(chatProcessingExchange).with("chat.process");
    }

    @Bean
    public Binding chatQueueBinding(@Qualifier("chatQueue") Queue queue, FanoutExchange chatExchange) {
        return BindingBuilder.bind(queue).to(chatExchange);
    }

    @Bean
    public Binding sseQueueBinding(@Qualifier("sseQueue") Queue queue, TopicExchange sseExchange) {
        return BindingBuilder.bind(queue).to(sseExchange).with("#");
    }

    @Bean
    public Binding pushQueueBinding(@Qualifier("pushQueue") Queue queue, TopicExchange pushExchange) {
        return BindingBuilder.bind(queue).to(pushExchange).with("#");
    }

    // 에러 메시지 브로드캐스트용
    @Bean
    public FanoutExchange chatErrorExchange() {
        return new FanoutExchange("chat.error.exchange");
    }

    @Bean
    public Queue chatErrorQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding chatErrorQueueBinding(@Qualifier("chatErrorQueue") Queue queue, FanoutExchange chatErrorExchange) {
        return BindingBuilder.bind(queue).to(chatErrorExchange);
    }

    /**
     * RabbitMQ Listener 재시도 정책
     * - 최대 3회 재시도 (초기 시도 포함 총 4회 시도)
     * - 1초, 2초, 3초 간격으로 재시도 (Exponential Backoff)
     * - 모두 실패하면 Recoverer가 에러 이벤트를 발행하고 큐에서 제거
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter, RabbitTemplate rabbitTemplate) {

        RetryOperationsInterceptor retryInterceptor = RetryInterceptorBuilder.stateless()
                .maxAttempts(3) // 최대 3회 재시도
                .backOffOptions(1000, 2.0, 3000) // 초기 1초, 배수 2.0, 최대 3초
                .recoverer((message, cause) -> {
                    try {
                        bill.chat.dto.ChatDTO failedChat = (bill.chat.dto.ChatDTO) messageConverter
                                .fromMessage(message);
                        bill.chat.websocket.payload.dto.WebSocketErrorDTO errorDTO = bill.chat.websocket.payload.dto.WebSocketErrorDTO
                                .builder()
                                .type("ERROR")
                                .channelId(failedChat.getChannelId())
                                .senderId(failedChat.getSenderId())
                                .content(failedChat.getContent())
                                .errorMessage("메시지 전송에 실패했습니다.")
                                .build();
                        rabbitTemplate.convertAndSend("chat.error.exchange", "", errorDTO);
                        org.slf4j.LoggerFactory.getLogger(RabbitMQConfig.class)
                                .error("메시지 3회 처리 실패, 에러 이벤트 발행됨: channelId={}, senderId={}", failedChat.getChannelId(),
                                        failedChat.getSenderId());
                    } catch (Exception e) {
                        org.slf4j.LoggerFactory.getLogger(RabbitMQConfig.class)
                                .error("Recoverer 처리 중 에러 발생", e);
                    }
                })
                .build();

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAdviceChain(retryInterceptor);
        // AMQP 리스너 스레드 풀 설정
        // block()이 리스너 스레드를 점유하므로, 처리량에 맞게 스레드 수를 확보한다.
        factory.setConcurrentConsumers(3); // 기본 3개 스레드 (동시 메시지 처리)
        factory.setMaxConcurrentConsumers(10); // 부하 시 최대 10개까지 자동 확장
        factory.setPrefetchCount(1); // 스레드당 1개씩만 가져옴 (공정한 분배)
        return factory;
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        return factory;
    }

    @Bean
    MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}