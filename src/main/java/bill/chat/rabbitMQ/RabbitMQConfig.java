package bill.chat.rabbitMQ;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        return new TopicExchange ("sse.exchange");
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