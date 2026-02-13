package com.linkwave.app.service.kafka;

import com.linkwave.app.domain.chat.ChatMessage;
import com.linkwave.app.service.chat.ChatService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatMessageConsumer {

  private static final Logger log = LoggerFactory.getLogger(ChatMessageConsumer.class);

  private final ChatService chatService;
  private final SimpMessagingTemplate messagingTemplate;
  private final Counter kafkaConsumedTotal;
  private final Counter kafkaConsumeErrors;
  private final Timer kafkaConsumeDuration;

  public ChatMessageConsumer(
      ChatService chatService,
      SimpMessagingTemplate messagingTemplate,
      MeterRegistry meterRegistry) {
    this.chatService = chatService;
    this.messagingTemplate = messagingTemplate;
    this.kafkaConsumedTotal =
        Counter.builder("kafka.messages.consumed.total")
            .tag("topic", "chat.messages")
            .register(meterRegistry);
    this.kafkaConsumeErrors =
        Counter.builder("kafka.errors.total").tag("operation", "consume").register(meterRegistry);
    this.kafkaConsumeDuration = Timer.builder("kafka.consume.duration").register(meterRegistry);
  }

  @KafkaListener(
      topics = "chat.messages",
      groupId = "${spring.kafka.consumer.group-id}",
      containerFactory = "chatMessageKafkaListenerContainerFactory")
  @Transactional
  public void consumeChatMessage(ConsumerRecord<String, ChatMessage> record) {
    Timer.Sample sample = Timer.start();
    ChatMessage message = record.value();

    log.info(
        "Consumed chat message: messageId={}, roomId={}, sender={}, bodyLength={}, partition={},"
            + " offset={}",
        message.getMessageId(),
        message.getRoomId(),
        message.getMaskedSender(),
        message.getBody() != null ? message.getBody().length() : 0,
        record.partition(),
        record.offset());

    try {
      chatService.persistMessage(message);
      log.debug("Persisted message {} to DB", message.getMessageId());
      messagingTemplate.convertAndSend("/topic/room." + message.getRoomId(), message);
      log.debug(
          "Broadcasted message {} to /topic/room.{}", message.getMessageId(), message.getRoomId());
      kafkaConsumedTotal.increment();

    } catch (Exception e) {
      kafkaConsumeErrors.increment();
      log.error("Failed to process message {}: {}", message.getMessageId(), e.getMessage(), e);
      throw e;
    } finally {
      sample.stop(kafkaConsumeDuration);
    }
  }
}
