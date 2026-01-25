package com.linkwave.app.domain.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ChatEvent.
 */
class ChatEventTest {
    
    @Test
    void create_shouldGenerateMessageIdAndTimestamp() {
        // When
        ChatEvent event = ChatEvent.create("+14155552671", "+14155559999", "Hello World");
        
        // Then
        assertThat(event.getMessageId()).isNotNull();
        assertThat(event.getMessageId()).isNotBlank();
        assertThat(event.getSender()).isEqualTo("+14155552671");
        assertThat(event.getRecipient()).isEqualTo("+14155559999");
        assertThat(event.getBody()).isEqualTo("Hello World");
        assertThat(event.getTimestamp()).isGreaterThan(0);
    }
    
    @Test
    void maskPhoneNumber_shouldMaskCorrectly() {
        // When
        String masked = ChatEvent.maskPhoneNumber("+14155552671");
        
        // Then
        assertThat(masked).isEqualTo("+141***71");
    }
    
    @Test
    void maskPhoneNumber_withShortNumber_shouldReturnMasked() {
        // When
        String masked = ChatEvent.maskPhoneNumber("+1234");
        
        // Then
        assertThat(masked).isEqualTo("***");
    }
    
    @Test
    void maskPhoneNumber_withNull_shouldReturnMasked() {
        // When
        String masked = ChatEvent.maskPhoneNumber(null);
        
        // Then
        assertThat(masked).isEqualTo("***");
    }
    
    @Test
    void getMaskedSender_shouldReturnMasked() {
        // Given
        ChatEvent event = ChatEvent.create("+14155552671", "+14155559999", "Test");
        
        // When
        String masked = event.getMaskedSender();
        
        // Then
        assertThat(masked).isEqualTo("+141***71");
    }
    
    @Test
    void getMaskedRecipient_shouldReturnMasked() {
        // Given
        ChatEvent event = ChatEvent.create("+14155552671", "+14155559999", "Test");
        
        // When
        String masked = event.getMaskedRecipient();
        
        // Then
        assertThat(masked).isEqualTo("+141***99");
    }
    
    @Test
    void toString_shouldNotExposeFullPhoneNumbersOrBody() {
        // Given
        ChatEvent event = ChatEvent.create("+14155552671", "+14155559999", "Secret message");
        
        // When
        String string = event.toString();
        
        // Then
        assertThat(string).doesNotContain("+14155552671");
        assertThat(string).doesNotContain("+14155559999");
        assertThat(string).doesNotContain("Secret message");
        assertThat(string).contains("***");
        assertThat(string).contains("bodyLength=14");
    }
}
