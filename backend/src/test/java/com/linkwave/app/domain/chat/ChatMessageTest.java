package com.linkwave.app.domain.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ChatMessage.
 */
class ChatMessageTest {

    @Test
    void create_shouldGenerateMessageIdAndTimestamp() {
        // When
        ChatMessage message = ChatMessage.create("+14155552671", "+14155559999", "Hello World");

        // Then
        assertThat(message.getMessageId()).isNotNull();
        assertThat(message.getMessageId()).isNotBlank();
        assertThat(message.getSenderPhoneNumber()).isEqualTo("+14155552671");
        assertThat(message.getRecipientPhoneNumber()).isEqualTo("+14155559999");
        assertThat(message.getBody()).isEqualTo("Hello World");
        assertThat(message.getSentAt()).isGreaterThan(0);
        assertThat(message.getTtlDays()).isEqualTo(7);
    }

    @Test
    void getMaskedSender_shouldReturnMasked() {
        // Given
        ChatMessage message = ChatMessage.create("+14155552671", "+14155559999", "Test");

        // When
        String masked = message.getMaskedSender();

        // Then
        assertThat(masked).isEqualTo("+141***71");
    }

    @Test
    void getMaskedRecipient_shouldReturnMasked() {
        // Given
        ChatMessage message = ChatMessage.create("+14155552671", "+14155559999", "Test");

        // When
        String masked = message.getMaskedRecipient();

        // Then
        assertThat(masked).isEqualTo("+141***99");
    }

    @Test
    void toString_shouldNotExposeFullPhoneNumbersOrBody() {
        // Given
        ChatMessage message = ChatMessage.create("+14155552671", "+14155559999", "Secret message");

        // When
        String string = message.toString();

        // Then
        assertThat(string).doesNotContain("+14155552671");
        assertThat(string).doesNotContain("+14155559999");
        assertThat(string).doesNotContain("Secret message");
        assertThat(string).contains("***");
        assertThat(string).contains("bodyLength=14");
    }
}
