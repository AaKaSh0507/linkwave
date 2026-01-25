package com.linkwave.app.service.kafka;

import com.linkwave.app.domain.chat.ChatEvent;
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
 * Unit tests for ChatEventProducer.
 */
@ExtendWith(MockitoExtension.class)
class ChatEventProducerTest {
    
    @Mock
    private KafkaTemplate<String, ChatEvent> kafkaTemplate;
    
    @Mock
    private SendResult<String, ChatEvent> sendResult;
    
    @Captor
    private ArgumentCaptor<String> topicCaptor;
    
    @Captor
    private ArgumentCaptor<String> keyCaptor;
    
    @Captor
    private ArgumentCaptor<ChatEvent> eventCaptor;
    
    private ChatEventProducer producer;
    
    @BeforeEach
    void setUp() {
        producer = new ChatEventProducer(kafkaTemplate);
    }
    
    @Test
    void publishChatEvent_shouldSendToCorrectTopic() {
        // Given
        ChatEvent event = ChatEvent.create("+14155552671", "+14155559999", "Hello");
        CompletableFuture<SendResult<String, ChatEvent>> future = CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(any(String.class), any(String.class), any(ChatEvent.class)))
            .thenReturn(future);
        
        // When
        producer.publishChatEvent(event);
        
        // Then
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        
        assertThat(topicCaptor.getValue()).isEqualTo("linkwave.chat.events");
        assertThat(keyCaptor.getValue()).isEqualTo(event.getRecipient());
        assertThat(eventCaptor.getValue()).isEqualTo(event);
    }
    
    @Test
    void publishChatEvent_shouldPartitionByRecipient() {
        // Given: Multiple events to different recipients
        ChatEvent event1 = ChatEvent.create("+14155552671", "+14155559998", "Message 1");
        ChatEvent event2 = ChatEvent.create("+14155552671", "+14155559999", "Message 2");
        
        CompletableFuture<SendResult<String, ChatEvent>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(ChatEvent.class)))
            .thenReturn(future);
        
        // When
        producer.publishChatEvent(event1);
        producer.publishChatEvent(event2);
        
        // Then
        verify(kafkaTemplate).send(eq("linkwave.chat.events"), eq("+14155559998"), eq(event1));
        verify(kafkaTemplate).send(eq("linkwave.chat.events"), eq("+14155559999"), eq(event2));
    }
    
    @Test
    void getChatEventsTopic_shouldReturnCorrectTopic() {
        // When
        String topic = producer.getChatEventsTopic();
        
        // Then
        assertThat(topic).isEqualTo("linkwave.chat.events");
    }
}
