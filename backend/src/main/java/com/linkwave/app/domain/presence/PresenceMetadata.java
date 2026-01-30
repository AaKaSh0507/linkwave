package com.linkwave.app.domain.presence;

import java.time.Instant;


public class PresenceMetadata {

    private final String userId;
    private final Instant lastSeen;
    private final int connectionCount;

    public PresenceMetadata(String userId, Instant lastSeen, int connectionCount) {
        this.userId = userId;
        this.lastSeen = lastSeen;
        this.connectionCount = connectionCount;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public int getConnectionCount() {
        return connectionCount;
    }

    
    public PresenceMetadata incrementConnections() {
        return new PresenceMetadata(userId, Instant.now(), connectionCount + 1);
    }

    
    public PresenceMetadata decrementConnections() {
        return new PresenceMetadata(userId, Instant.now(), Math.max(0, connectionCount - 1));
    }

    
    public PresenceMetadata updateLastSeen() {
        return new PresenceMetadata(userId, Instant.now(), connectionCount);
    }

    
    public boolean hasActiveConnections() {
        return connectionCount > 0;
    }

    @Override
    public String toString() {
        return "PresenceMetadata{" +
                "userId='" + userId + '\'' +
                ", lastSeen=" + lastSeen +
                ", connectionCount=" + connectionCount +
                '}';
    }
}
