package com.linkwave.app.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwave.app.domain.auth.AuthenticatedUserContext;
import com.linkwave.app.domain.chat.ChatEvent;
import com.linkwave.app.domain.websocket.WsMessageEnvelope;
import com.linkwave.app.service.session.SessionService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Integration tests for WebSocket → Kafka flow.
 * 
 * Tests:
 * - chat.send → Kafka publish success
 * - Kafka consumer receives event
 * - chat.sent acknowledgment to sender
 * - Invalid payload validation
 * - Unauthenticated user rejection
 * 
 * Note: Disabled by default due to embedded Kafka startup time.
 * Enable manually to run full integration tests.
 */
@Disabled("Embedded Kafka tests - enable manually for full integration testing")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(
    partitions = 1,
    topics = {"linkwave.chat.events"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DirtiesContext
class KafkaWebSocketIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @MockBean
    private SessionService sessionService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private String wsUrl;
    private Consumer<String, ChatEvent> testConsumer;
    
    @BeforeEach
    void setUp() {
        wsUrl = "ws://localhost:" + port + "/ws";
        
        // Create test consumer for verification
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.linkwave.app.domain.chat");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ChatEvent.class.getName());
        
        testConsumer = new DefaultKafkaConsumerFactory<String, ChatEvent>(consumerProps)
            .createConsumer();
        testConsumer.subscribe(Collections.singletonList("linkwave.chat.events"));
    }
    
    @Test
    void chatSend_authenticated_shouldPublishToKafkaAndSendAck() throws Exception {
        // Given: Authenticated user
        String senderPhone = "+14155552671";
        String recipientPhone = "+14155559999";
        AuthenticatedUserContext userContext = new AuthenticatedUserContext(senderPhone, Instant.now());
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.of(userContext));
        
        // When: Connect and send chat.send
        StandardWebSocketClient client = new StandardWebSocketClient();
        TestWebSocketHandler handler = new TestWebSocketHandler();
        
        WebSocketSession session = client.execute(handler, new WebSocketHttpHeaders(), URI.create(wsUrl))
            .get(5, TimeUnit.SECONDS);
        
        // Send chat message
        String messageBody = "Hello from Kafka test!";
        String chatPayload = String.format("{\"body\":\"%s\"}", messageBody);
        WsMessageEnvelope chatSend = new WsMessageEnvelope(
            "chat.send",
            recipientPhone,
            objectMapper.readTree(chatPayload)
        );
        String chatJson = objectMapper.writeValueAsString(chatSend);
        session.sendMessage(new TextMessage(chatJson));
        
        // Then: Should receive chat.sent acknowledgment
        String ackResponse = handler.messages.poll(5, TimeUnit.SECONDS);
        assertThat(ackResponse).isNotNull();
        
        WsMessageEnvelope ack = objectMapper.readValue(ackResponse, WsMessageEnvelope.class);
        assertThat(ack.getEvent()).isEqualTo("chat.sent");
        assertThat(ack.getPayload().has("messageId")).isTrue();
        
        String messageId = ack.getPayload().get("messageId").asText();
        assertThat(messageId).isNotBlank();
        
        // And: Event should be in Kafka
        ConsumerRecords<String, ChatEvent> records = testConsumer.poll(Duration.ofSeconds(5));
        assertThat(records.isEmpty()).isFalse();
        
        ConsumerRecord<String, ChatEvent> record = records.iterator().next();
        ChatEvent event = record.value();
        
        assertThat(event.getMessageId()).isEqualTo(messageId);
        assertThat(event.getSender()).isEqualTo(senderPhone);
        assertThat(event.getRecipient()).isEqualTo(recipientPhone);
        assertThat(event.getBody()).isEqualTo(messageBody);
        assertThat(event.getTimestamp()).isGreaterThan(0);
        
        // Verify partition key is recipient
        assertThat(record.key()).isEqualTo(recipientPhone);
        
        // Cleanup
        session.close();
    }
    
    @Test
    void chatSend_withoutTo_shouldCloseConnection() throws Exception {
        // Given: Authenticated user
        String phoneNumber = "+14155552672";
        AuthenticatedUserContext userContext = new AuthenticatedUserContext(phoneNumber, Instant.now());
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.of(userContext));
        
        // When: Send chat.send without 'to' field
        StandardWebSocketClient client = new StandardWebSocketClient();
        TestWebSocketHandler handler = new TestWebSocketHandler();
        
        WebSocketSession session = client.execute(handler, new WebSocketHttpHeaders(), URI.create(wsUrl))
            .get(5, TimeUnit.SECONDS);
        
        String chatPayload = "{\"body\":\"Hello\"}";
        WsMessageEnvelope chatSend = new WsMessageEnvelope(
            "chat.send",
            null, // Missing 'to'
            objectMapper.readTree(chatPayload)
        );
        String chatJson = objectMapper.writeValueAsString(chatSend);
        session.sendMessage(new TextMessage(chatJson));
        
        // Then: Connection should close
        Thread.sleep(500);
        assertThat(session.isOpen()).isFalse();
    }
    
    @Test
    void chatSend_withoutBody_shouldCloseConnection() throws Exception {
        // Given: Authenticated user
        String phoneNumber = "+14155552673";
        AuthenticatedUserContext userContext = new AuthenticatedUserContext(phoneNumber, Instant.now());
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.of(userContext));
        
        // When: Send chat.send without 'body' in payload
        StandardWebSocketClient client = new StandardWebSocketClient();
        TestWebSocketHandler handler = new TestWebSocketHandler();
        
        WebSocketSession session = client.execute(handler, new WebSocketHttpHeaders(), URI.create(wsUrl))
            .get(5, TimeUnit.SECONDS);
        
        String chatPayload = "{}"; // Missing 'body'
        WsMessageEnvelope chatSend = new WsMessageEnvelope(
            "chat.send",
            "+14155559999",
            objectMapper.readTree(chatPayload)
        );
        String chatJson = objectMapper.writeValueAsString(chatSend);
        session.sendMessage(new TextMessage(chatJson));
        
        // Then: Connection should close
        Thread.sleep(500);
        assertThat(session.isOpen()).isFalse();
    }
    
    @Test
    void chatSend_withEmptyBody_shouldCloseConnection() throws Exception {
        // Given: Authenticated user
        String phoneNumber = "+14155552674";
        AuthenticatedUserContext userContext = new AuthenticatedUserContext(phoneNumber, Instant.now());
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.of(userContext));
        
        // When: Send chat.send with empty body
        StandardWebSocketClient client = new StandardWebSocketClient();
        TestWebSocketHandler handler = new TestWebSocketHandler();
        
        WebSocketSession session = client.execute(handler, new WebSocketHttpHeaders(), URI.create(wsUrl))
            .get(5, TimeUnit.SECONDS);
        
        String chatPayload = "{\"body\":\"\"}"; // Empty body
        WsMessageEnvelope chatSend = new WsMessageEnvelope(
            "chat.send",
            "+14155559999",
            objectMapper.readTree(chatPayload)
        );
        String chatJson = objectMapper.writeValueAsString(chatSend);
        session.sendMessage(new TextMessage(chatJson));
        
        // Then: Connection should close
        Thread.sleep(500);
        assertThat(session.isOpen()).isFalse();
    }
    
    @Test
    void chatSend_unauthenticated_shouldRejectConnection() throws Exception {
        // Given: No authenticated session
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.empty());
        
        // When/Then: Connection should be rejected
        StandardWebSocketClient client = new StandardWebSocketClient();
        TestWebSocketHandler handler = new TestWebSocketHandler();
        
        try {
            client.execute(handler, new WebSocketHttpHeaders(), URI.create(wsUrl)).get(5, TimeUnit.SECONDS);
            assertThat(false).as("Connection should have been rejected").isTrue();
        } catch (Exception e) {
            // Expected - connection rejected
            assertThat(e.getMessage()).contains("401");
        }
    }
    
    @Test
    void multipleChatMessages_shouldMaintainOrder() throws Exception {
        // Given: Authenticated user
        String senderPhone = "+14155552675";
        String recipientPhone = "+14155559998";
        AuthenticatedUserContext userContext = new AuthenticatedUserContext(senderPhone, Instant.now());
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.of(userContext));
        
        // When: Send multiple messages to same recipient
        StandardWebSocketClient client = new StandardWebSocketClient();
        TestWebSocketHandler handler = new TestWebSocketHandler();
        
        WebSocketSession session = client.execute(handler, new WebSocketHttpHeaders(), URI.create(wsUrl))
            .get(5, TimeUnit.SECONDS);
        
        List<String> messageIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String messageBody = "Message " + i;
            String chatPayload = String.format("{\"body\":\"%s\"}", messageBody);
            WsMessageEnvelope chatSend = new WsMessageEnvelope(
                "chat.send",
                recipientPhone,
                objectMapper.readTree(chatPayload)
            );
            String chatJson = objectMapper.writeValueAsString(chatSend);
            session.sendMessage(new TextMessage(chatJson));
            
            // Get acknowledgment
            String ackResponse = handler.messages.poll(5, TimeUnit.SECONDS);
            assertThat(ackResponse).isNotNull();
            WsMessageEnvelope ack = objectMapper.readValue(ackResponse, WsMessageEnvelope.class);
            messageIds.add(ack.getPayload().get("messageId").asText());
        }
        
        // Then: All messages should be in Kafka
        ConsumerRecords<String, ChatEvent> records = testConsumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isEqualTo(3);
        
        // Verify all messages went to same partition (ordered by recipient)
        Set<Integer> partitions = new HashSet<>();
        for (ConsumerRecord<String, ChatEvent> record : records) {
            partitions.add(record.partition());
            assertThat(record.key()).isEqualTo(recipientPhone);
        }
        assertThat(partitions).hasSize(1); // All in same partition
        
        // Cleanup
        session.close();
    }
    
    /**
     * Test handler to capture WebSocket messages.
     */
    private static class TestWebSocketHandler extends TextWebSocketHandler {
        
        final BlockingQueue<String> messages = new ArrayBlockingQueue<>(10);
        
        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            messages.offer(message.getPayload());
        }
    }
}
