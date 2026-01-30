package com.linkwave.app.service.kafka;

import com.linkwave.app.domain.chat.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ChatMessageProducer.
 */
@ExtendWith(MockitoExtension.class)
class ChatMessageProducerTest {

    @Mock
    private KafkaTemplate<String, ChatMessage> kafkaTemplate;

    @Mock
    private SendResult<String, ChatMessage> sendResult;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<ChatMessage> eventCaptor;

    private ChatMessageProducer producer;

    @BeforeEach
    void setUp() {
        producer = new ChatMessageProducer(kafkaTemplate);
    }

    @Test
    void publishChatMessage_shouldSendToCorrectTopic() {
        // Given
        ChatMessage message = ChatMessage.create("+14155552671", "+14155559999", "Hello");
        CompletableFuture<SendResult<String, ChatMessage>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(any(String.class), any(String.class), any(ChatMessage.class)))
                .thenReturn(future);

        // When
        producer.publishChatMessage(message);

        // Then
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("linkwave.chat.messages.v2");
        assertThat(keyCaptor.getValue()).isEqualTo(message.getRecipientPhoneNumber());
        assertThat(eventCaptor.getValue()).isEqualTo(message);
    }

    @Test
    void publishChatMessage_shouldPartitionByRecipient() {
        // Given: Multiple events to different recipients
        ChatMessage message1 = ChatMessage.create("+14155552671", "+14155559998", "Message 1");
        ChatMessage message2 = ChatMessage.create("+14155552671", "+14155559999", "Message 2");

        CompletableFuture<SendResult<String, ChatMessage>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(ChatMessage.class)))
                .thenReturn(future);

        // When
        producer.publishChatMessage(message1);
        producer.publishChatMessage(message2);

        // Then
        verify(kafkaTemplate).send(eq("linkwave.chat.messages.v2"), eq("+14155559998"), eq(message1));
        verify(kafkaTemplate).send(eq("linkwave.chat.messages.v2"), eq("+14155559999"), eq(message2));
    }

    @Test
    void getChatEventsTopic_shouldReturnCorrectTopic() {
        // When
        String topic = producer.getChatEventsTopic();

        // Then
        assertThat(topic).isEqualTo("linkwave.chat.messages.v2");
    }
}
