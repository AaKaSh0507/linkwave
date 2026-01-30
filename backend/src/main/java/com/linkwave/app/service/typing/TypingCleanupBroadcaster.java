package com.linkwave.app.service.typing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwave.app.domain.typing.TypingEvent;
import com.linkwave.app.service.room.RoomMembershipService;
import com.linkwave.app.websocket.NativeWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;


@Service
public class TypingCleanupBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(TypingCleanupBroadcaster.class);

    private final TypingStateManager typingStateManager;
    private final RoomMembershipService roomMembershipService;
    private final NativeWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public TypingCleanupBroadcaster(
            TypingStateManager typingStateManager,
            RoomMembershipService roomMembershipService,
            NativeWebSocketHandler webSocketHandler,
            ObjectMapper objectMapper) {
        this.typingStateManager = typingStateManager;
        this.roomMembershipService = roomMembershipService;
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
    }

    
    @Scheduled(fixedDelay = 2000, initialDelay = 3000)
    public void broadcastExpiredTyping() {
        List<TypingStateManager.ExpiredTypingState> expired = typingStateManager.cleanupStaleTyping();

        for (TypingStateManager.ExpiredTypingState state : expired) {
            broadcastTypingStop(state.roomId, state.userId);
        }
    }

    
    private void broadcastTypingStop(String roomId, String senderId) {
        try {
            Set<String> members = roomMembershipService.getRoomMembers(roomId);
            if (members.isEmpty()) {
                return; 
            }

            TypingEvent event = new TypingEvent(senderId, roomId, TypingEvent.TypingAction.STOP);
            String json = objectMapper.writeValueAsString(event);

            for (String memberId : members) {
                if (!memberId.equals(senderId)) {
                    webSocketHandler.sendToUser(memberId, json);
                }
            }

            log.debug("Broadcasted auto-timeout typing.stop for user {} in room {}",
                    maskUserId(senderId), roomId);

        } catch (Exception e) {
            log.error("Error broadcasting expired typing event: {}", e.getMessage());
        }
    }

    private String maskUserId(String userId) {
        if (userId == null || userId.length() < 7) {
            return "***";
        }
        return userId.substring(0, 4) + "***" + userId.substring(userId.length() - 2);
    }
}
