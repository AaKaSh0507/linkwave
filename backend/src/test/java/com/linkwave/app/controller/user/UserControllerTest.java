package com.linkwave.app.controller.user;

import com.linkwave.app.domain.auth.AuthenticatedUserContext;
import com.linkwave.app.service.session.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"linkwave.chat.messages.v2"})
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionService sessionService;

    @Test
    @WithMockUser
    void getMe_withAuthenticatedSession_shouldReturnUserProfile() throws Exception {
        // Given: Authenticated session
        String phoneNumber = "+1234567890";
        Instant authenticatedAt = Instant.parse("2026-01-25T10:00:00Z");
        AuthenticatedUserContext userContext = new AuthenticatedUserContext(phoneNumber, authenticatedAt);
        
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.of(userContext));

        // When/Then: Should return user profile with masked phone
        mockMvc.perform(get("/api/v1/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("+123***90"))
                .andExpect(jsonPath("$.authenticatedAt").value("2026-01-25T10:00:00Z"));

        verify(sessionService, times(1)).getAuthenticatedUser();
    }

    @Test
    @WithMockUser
    void getMe_withoutAuthenticatedSession_shouldReturnUnauthorized() throws Exception {
        // Given: No authenticated session
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.empty());

        // When/Then: Should return 401
        mockMvc.perform(get("/api/v1/user/me"))
                .andExpect(status().isUnauthorized());

        verify(sessionService, times(1)).getAuthenticatedUser();
    }

    @Test
    @WithMockUser
    void getMe_withInvalidSession_shouldReturnUnauthorized() throws Exception {
        // Given: Session exists but not authenticated
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.empty());

        // When/Then: Should return 401
        mockMvc.perform(get("/api/v1/user/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getMe_shouldMaskPhoneNumberInResponse() throws Exception {
        // Given: Authenticated session with full phone number
        String phoneNumber = "+14155552671";
        Instant authenticatedAt = Instant.now();
        AuthenticatedUserContext userContext = new AuthenticatedUserContext(phoneNumber, authenticatedAt);
        
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.of(userContext));

        // When/Then: Response should contain masked phone number
        mockMvc.perform(get("/api/v1/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("+141***71"));
    }

    @Test
    @WithMockUser
    void getMe_shouldReturnIsoFormattedTimestamp() throws Exception {
        // Given: Authenticated session
        String phoneNumber = "+1234567890";
        Instant authenticatedAt = Instant.parse("2026-01-25T15:30:45.123Z");
        AuthenticatedUserContext userContext = new AuthenticatedUserContext(phoneNumber, authenticatedAt);
        
        when(sessionService.getAuthenticatedUser()).thenReturn(Optional.of(userContext));

        // When/Then: Timestamp should be in ISO format
        mockMvc.perform(get("/api/v1/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticatedAt").value("2026-01-25T15:30:45.123Z"));
    }
}
