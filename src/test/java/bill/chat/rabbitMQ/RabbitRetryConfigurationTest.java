package bill.chat.rabbitMQ;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bill.chat.dto.ChatDTO;
import bill.chat.websocket.payload.dto.WebSocketErrorDTO;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

class RabbitRetryConfigurationTest {

    @Test
    void retries3TimesAndPublishesError() throws Throwable {
        RabbitMQConfig config = new RabbitMQConfig();
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        MessageConverter messageConverter = mock(MessageConverter.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

        ChatDTO failedChat = new ChatDTO();
        failedChat.setChannelId("channel-1");
        failedChat.setSenderId("user-1");
        failedChat.setContent("hello");
        when(messageConverter.fromMessage(any(Message.class))).thenReturn(failedChat);

        SimpleRabbitListenerContainerFactory factory = config.rabbitListenerContainerFactory(
                connectionFactory, messageConverter, rabbitTemplate);

        Advice[] adviceChain = (Advice[]) ReflectionTestUtils.getField(factory, "adviceChain");
        assertNotNull(adviceChain);
        assertTrue(adviceChain.length > 0);

        MethodInterceptor interceptor = (MethodInterceptor) adviceChain[0];
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(
                ReflectionUtils.findMethod(RabbitRetryConfigurationTest.class, "dummyListenerMethod", Message.class));
        Message amqpMessage = new Message("{}".getBytes(), new MessageProperties());
        when(invocation.getArguments()).thenReturn(new Object[] {
                new Object(),
                amqpMessage
        });
        when(invocation.getThis()).thenReturn(this);

        assertDoesNotThrow(() -> interceptor.invoke(invocation));

        verify(invocation, times(4)).getMethod();
        verify(invocation, times(4)).getArguments();
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("chat.error.exchange"),
                eq(""),
                any(WebSocketErrorDTO.class));
    }

    @SuppressWarnings("unused")
    private void dummyListenerMethod(Message message) {
        throw new RuntimeException("db down");
    }
}
