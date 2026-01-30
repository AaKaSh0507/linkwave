package com.linkwave.app.service.presence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkwave.app.domain.presence.PresenceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Service
public class PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);

    private static final String PRESENCE_KEY_PREFIX = "linkwave:presence:";
    private static final long PRESENCE_TTL_SECONDS = 75; 
    private static final long HEARTBEAT_MIN_INTERVAL_MS = 20_000; 

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    
    private final Map<String, Long> lastHeartbeatTime = new HashMap<>();

    public PresenceService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    
    public void markOnline(String userId) {
        String key = getPresenceKey(userId);

        try {
            PresenceMetadata current = getPresenceMetadata(userId);
            PresenceMetadata updated;

            if (current == null) {
                
                updated = new PresenceMetadata(userId, Instant.now(), 1);
                log.info("User {} came online (first connection)", maskUserId(userId));
            } else {
                
                updated = current.incrementConnections();
                log.info("User {} added connection (count: {})",
                        maskUserId(userId), updated.getConnectionCount());
            }

            setPresenceMetadata(key, updated);

        } catch (Exception e) {
            log.error("Failed to mark user {} as online: {}", maskUserId(userId), e.getMessage());
        }
    }

    
    public void markDisconnect(String userId) {
        String key = getPresenceKey(userId);

        try {
            PresenceMetadata current = getPresenceMetadata(userId);

            if (current == null) {
                log.warn("Disconnect called for user {} with no presence record", maskUserId(userId));
                return;
            }

            PresenceMetadata updated = current.decrementConnections();

            if (updated.hasActiveConnections()) {
                
                setPresenceMetadata(key, updated);
                log.info("User {} disconnected (remaining connections: {})",
                        maskUserId(userId), updated.getConnectionCount());
            } else {
                
                log.info("User {} disconnected (last connection, will expire in {}s)",
                        maskUserId(userId), PRESENCE_TTL_SECONDS);
                
            }

        } catch (Exception e) {
            log.error("Failed to mark user {} disconnect: {}", maskUserId(userId), e.getMessage());
        }
    }

    
    public boolean recordHeartbeat(String userId) {
        
        Long lastHeartbeat = lastHeartbeatTime.get(userId);
        long now = System.currentTimeMillis();

        if (lastHeartbeat != null && (now - lastHeartbeat) < HEARTBEAT_MIN_INTERVAL_MS) {
            log.debug("Heartbeat rate-limited for user {}", maskUserId(userId));
            return false;
        }

        String key = getPresenceKey(userId);

        try {
            PresenceMetadata current = getPresenceMetadata(userId);

            if (current == null) {
                log.warn("Heartbeat received for user {} with no presence record, marking online",
                        maskUserId(userId));
                markOnline(userId);
                return true;
            }

            
            PresenceMetadata updated = current.updateLastSeen();
            setPresenceMetadata(key, updated);

            lastHeartbeatTime.put(userId, now);
            log.debug("Heartbeat recorded for user {} (connections: {})",
                    maskUserId(userId), updated.getConnectionCount());

            return true;

        } catch (Exception e) {
            log.error("Failed to record heartbeat for user {}: {}", maskUserId(userId), e.getMessage());
            return false;
        }
    }

    
    public boolean isUserOnline(String userId) {
        String key = getPresenceKey(userId);
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    
    public Instant getLastSeen(String userId) {
        PresenceMetadata metadata = getPresenceMetadata(userId);
        return metadata != null ? metadata.getLastSeen() : null;
    }

    
    public Map<String, Boolean> getUsersPresence(List<String> userIds) {
        Map<String, Boolean> presenceMap = new HashMap<>();

        for (String userId : userIds) {
            presenceMap.put(userId, isUserOnline(userId));
        }

        return presenceMap;
    }

    
    public PresenceMetadata getPresenceMetadata(String userId) {
        String key = getPresenceKey(userId);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            return null;
        }

        try {
            return objectMapper.readValue(json, PresenceMetadata.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize presence metadata for user {}: {}",
                    maskUserId(userId), e.getMessage());
            return null;
        }
    }

    
    private void setPresenceMetadata(String key, PresenceMetadata metadata) {
        try {
            String json = objectMapper.writeValueAsString(metadata);
            redisTemplate.opsForValue().set(key, json, PRESENCE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize presence metadata: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize presence metadata", e);
        }
    }

    
    private String getPresenceKey(String userId) {
        return PRESENCE_KEY_PREFIX + userId;
    }

    
    private String maskUserId(String userId) {
        if (userId == null || userId.length() < 7) {
            return "***";
        }
        return userId.substring(0, 4) + "***" + userId.substring(userId.length() - 2);
    }

    
    public long getPresenceTtlSeconds() {
        return PRESENCE_TTL_SECONDS;
    }
}
