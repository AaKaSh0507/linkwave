package com.linkwave.app.service.typing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


class TypingStateManagerTest {

    private TypingStateManager typingStateManager;

    private static final String TEST_ROOM_1 = "room-123";
    private static final String TEST_ROOM_2 = "room-456";
    private static final String TEST_USER_1 = "+14155551234";
    private static final String TEST_USER_2 = "+14155555678";
    private static final String SESSION_1 = "session-1";
    private static final String SESSION_2 = "session-2";

    @BeforeEach
    void setUp() {
        typingStateManager = new TypingStateManager();
    }

    @AfterEach
    void tearDown() {
        
        typingStateManager.clearUserTyping(TEST_USER_1, SESSION_1);
        typingStateManager.clearUserTyping(TEST_USER_2, SESSION_2);
    }

    @Test
    void testMarkTypingStart_addsUserToTypingSet() {
        
        boolean added = typingStateManager.markTypingStart(TEST_ROOM_1, TEST_USER_1, SESSION_1);

        
        assertThat(added).isTrue();

        Set<String> typingUsers = typingStateManager.getTypingUsers(TEST_ROOM_1);
        assertThat(typingUsers).contains(TEST_USER_1);
    }

    @Test
    void testMarkTypingStop_removesUserFromTypingSet() {
        
        typingStateManager.markTypingStart(TEST_ROOM_1, TEST_USER_1, SESSION_1);

        
        typingStateManager.markTypingStop(TEST_ROOM_1, TEST_USER_1, SESSION_1);

        
        Set<String> typingUsers = typingStateManager.getTypingUsers(TEST_ROOM_1);
        assertThat(typingUsers).doesNotContain(TEST_USER_1);
    }

    @Test
    void testRateLimiting_duplicateStartIgnored() {
        
        boolean first = typingStateManager.markTypingStart(TEST_ROOM_1, TEST_USER_1, SESSION_1);
        assertThat(first).isTrue();

        
        boolean second = typingStateManager.markTypingStart(TEST_ROOM_1, TEST_USER_1, SESSION_1);

        
        assertThat(second).isFalse();
    }

    @Test
    void testMultipleUsersTyping_inSameRoom() {
        
        typingStateManager.markTypingStart(TEST_ROOM_1, TEST_USER_1, SESSION_1);
        typingStateManager.markTypingStart(TEST_ROOM_1, TEST_USER_2, SESSION_2);

        
        Set<String> typingUsers = typingStateManager.getTypingUsers(TEST_ROOM_1);
        assertThat(typingUsers).containsExactlyInAnyOrder(TEST_USER_1, TEST_USER_2);
    }

    @Test
    void testRoomIsolation_typingInDifferentRooms() {
        
        typingStateManager.markTypingStart(TEST_ROOM_1, TEST_USER_1, SESSION_1);

        
        Set<String> room2Typing = typingStateManager.getTypingUsers(TEST_ROOM_2);
        assertThat(room2Typing).doesNotContain(TEST_USER_1);
    }

    @Test
    void testClearUserTyping_removesFromAllRooms() {
        
        typingStateManager.markTypingStart(TEST_ROOM_1, TEST_USER_1, SESSION_1);
        typingStateManager.markTypingStart(TEST_ROOM_2, TEST_USER_1, SESSION_1);

        
        List<String> affectedRooms = typingStateManager.clearUserTyping(TEST_USER_1, SESSION_1);

        
        assertThat(affectedRooms).containsExactlyInAnyOrder(TEST_ROOM_1, TEST_ROOM_2);
        assertThat(typingStateManager.getTypingUsers(TEST_ROOM_1)).doesNotContain(TEST_USER_1);
        assertThat(typingStateManager.getTypingUsers(TEST_ROOM_2)).doesNotContain(TEST_USER_1);
    }

    @Test
    void testMultiDevice_sameUserDifferentSessions() throws InterruptedException {
        
        typingStateManager.markTypingStart(TEST_ROOM_1, TEST_USER_1, SESSION_1);

        
        Thread.sleep(2100);

        typingStateManager.markTypingStart(TEST_ROOM_1, TEST_USER_1, SESSION_2);

        
        Set<String> typingUsers = typingStateManager.getTypingUsers(TEST_ROOM_1);
        assertThat(typingUsers).contains(TEST_USER_1);

        
        typingStateManager.markTypingStop(TEST_ROOM_1, TEST_USER_1, SESSION_1);

        
        typingUsers = typingStateManager.getTypingUsers(TEST_ROOM_1);
        assertThat(typingUsers).contains(TEST_USER_1);

        
        typingStateManager.markTypingStop(TEST_ROOM_1, TEST_USER_1, SESSION_2);

        
        typingUsers = typingStateManager.getTypingUsers(TEST_ROOM_1);
        assertThat(typingUsers).doesNotContain(TEST_USER_1);
    }

    @Test
    void testGetTypingUsers_emptyRoomReturnsEmptySet() {
        
        Set<String> typingUsers = typingStateManager.getTypingUsers("nonexistent-room");

        
        assertThat(typingUsers).isEmpty();
    }

    @Test
    void testGetStats_returnsCorrectCounts() {
        
        typingStateManager.markTypingStart(TEST_ROOM_1, TEST_USER_1, SESSION_1);
        typingStateManager.markTypingStart(TEST_ROOM_2, TEST_USER_2, SESSION_2);

        
        TypingStateManager.TypingStats stats = typingStateManager.getStats();

        
        assertThat(stats.activeRooms).isEqualTo(2);
        assertThat(stats.typingUsers).isEqualTo(2);
    }

    @Test
    void testCleanupStaleTyping_doesNotRemoveRecentActivity() {
        
        typingStateManager.markTypingStart(TEST_ROOM_1, TEST_USER_1, SESSION_1);

        
        List<TypingStateManager.ExpiredTypingState> expired = typingStateManager.cleanupStaleTyping();

        
        assertThat(expired).isEmpty();
        assertThat(typingStateManager.getTypingUsers(TEST_ROOM_1)).contains(TEST_USER_1);
    }

    @Test
    void testMarkTypingStop_nonExistentUser_handlesGracefully() {
        
        typingStateManager.markTypingStop(TEST_ROOM_1, TEST_USER_1, SESSION_1);

        
        Set<String> typingUsers = typingStateManager.getTypingUsers(TEST_ROOM_1);
        assertThat(typingUsers).isEmpty();
    }
}
