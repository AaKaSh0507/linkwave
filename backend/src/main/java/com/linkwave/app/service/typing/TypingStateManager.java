package com.linkwave.app.service.typing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Service
public class TypingStateManager {

    private static final Logger log = LoggerFactory.getLogger(TypingStateManager.class);

    
    private static final long TYPING_TIMEOUT_SECONDS = 5;
    private static final long RATE_LIMIT_SECONDS = 2;

    
    private final Map<String, Set<TypingState>> roomTypingState = new ConcurrentHashMap<>();

    
    private final Map<String, Instant> lastTypingStart = new ConcurrentHashMap<>();

    
    public boolean markTypingStart(String roomId, String userId, String sessionId) {
        String rateLimitKey = userId + ":" + roomId;
        Instant now = Instant.now();

        
        Instant lastStart = lastTypingStart.get(rateLimitKey);
        if (lastStart != null && now.isBefore(lastStart.plusSeconds(RATE_LIMIT_SECONDS))) {
            log.debug("Rate limited typing.start for user {} in room {}", maskUserId(userId), roomId);
            return false;
        }

        
        lastTypingStart.put(rateLimitKey, now);

        
        roomTypingState.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet())
                .add(new TypingState(userId, sessionId, now));

        log.debug("User {} started typing in room {}", maskUserId(userId), roomId);
        return true;
    }

    
    public void markTypingStop(String roomId, String userId, String sessionId) {
        Set<TypingState> typingUsers = roomTypingState.get(roomId);
        if (typingUsers != null) {
            typingUsers.removeIf(state -> state.userId.equals(userId) && state.sessionId.equals(sessionId));

            
            if (typingUsers.isEmpty()) {
                roomTypingState.remove(roomId);
            }

            log.debug("User {} stopped typing in room {}", maskUserId(userId), roomId);
        }
    }

    
    public Set<String> getTypingUsers(String roomId) {
        Set<TypingState> typingUsers = roomTypingState.get(roomId);
        if (typingUsers == null || typingUsers.isEmpty()) {
            return Collections.emptySet();
        }

        return typingUsers.stream()
                .map(state -> state.userId)
                .collect(Collectors.toSet());
    }

    
    public List<String> clearUserTyping(String userId, String sessionId) {
        List<String> affectedRooms = new ArrayList<>();

        for (Map.Entry<String, Set<TypingState>> entry : roomTypingState.entrySet()) {
            String roomId = entry.getKey();
            Set<TypingState> typingUsers = entry.getValue();

            boolean removed = typingUsers
                    .removeIf(state -> state.userId.equals(userId) && state.sessionId.equals(sessionId));

            if (removed) {
                affectedRooms.add(roomId);
            }

            
            if (typingUsers.isEmpty()) {
                roomTypingState.remove(roomId);
            }
        }

        if (!affectedRooms.isEmpty()) {
            log.debug("Cleared typing state for user {} in {} rooms",
                    maskUserId(userId), affectedRooms.size());
        }

        
        lastTypingStart.entrySet().removeIf(e -> e.getKey().startsWith(userId + ":"));

        return affectedRooms;
    }

    
    @Scheduled(fixedDelay = 2000)
    public List<ExpiredTypingState> cleanupStaleTyping() {
        Instant cutoff = Instant.now().minusSeconds(TYPING_TIMEOUT_SECONDS);
        List<ExpiredTypingState> expired = new ArrayList<>();

        for (Map.Entry<String, Set<TypingState>> entry : roomTypingState.entrySet()) {
            String roomId = entry.getKey();
            Set<TypingState> typingUsers = entry.getValue();

            List<TypingState> stale = typingUsers.stream()
                    .filter(state -> state.lastActivity.isBefore(cutoff))
                    .collect(Collectors.toList());

            for (TypingState state : stale) {
                typingUsers.remove(state);
                expired.add(new ExpiredTypingState(roomId, state.userId, state.sessionId));
            }

            
            if (typingUsers.isEmpty()) {
                roomTypingState.remove(roomId);
            }
        }

        if (!expired.isEmpty()) {
            log.debug("Cleaned up {} stale typing indicators", expired.size());
        }

        return expired;
    }

    
    public TypingStats getStats() {
        int totalRooms = roomTypingState.size();
        int totalTypingUsers = roomTypingState.values().stream()
                .mapToInt(Set::size)
                .sum();

        return new TypingStats(totalRooms, totalTypingUsers);
    }

    
    private static class TypingState {
        final String userId;
        final String sessionId;
        final Instant lastActivity;

        TypingState(String userId, String sessionId, Instant lastActivity) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.lastActivity = lastActivity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TypingState that = (TypingState) o;
            return userId.equals(that.userId) && sessionId.equals(that.sessionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, sessionId);
        }
    }

    
    public static class ExpiredTypingState {
        public final String roomId;
        public final String userId;
        public final String sessionId;

        public ExpiredTypingState(String roomId, String userId, String sessionId) {
            this.roomId = roomId;
            this.userId = userId;
            this.sessionId = sessionId;
        }
    }

    
    public static class TypingStats {
        public final int activeRooms;
        public final int typingUsers;

        public TypingStats(int activeRooms, int typingUsers) {
            this.activeRooms = activeRooms;
            this.typingUsers = typingUsers;
        }
    }

    private String maskUserId(String userId) {
        if (userId == null || userId.length() < 7) {
            return "***";
        }
        return userId.substring(0, 4) + "***" + userId.substring(userId.length() - 2);
    }
}
