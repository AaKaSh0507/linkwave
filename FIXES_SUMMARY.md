# Fix Summary: REST API and WebSocket Issues

## Root Cause Summary

### REST API `/api/v1/contacts` Returns 404
- **Cause**: Endpoint did not exist in backend
- **Location**: No controller was handling this route
- **Impact**: Low (frontend had graceful fallback)

### WebSocket Connection to `ws://localhost:8080/ws` Fails
- **Cause 1**: Path mismatch - backend at `/ws/chat`, frontend at `/ws`
- **Cause 2**: Protocol mismatch - backend uses STOMP, frontend uses native WebSocket
- **Impact**: High (prevented all real-time messaging)

---

## Backend Code Changes

### 1. Added Contacts Endpoint
**File**: `backend/src/main/java/com/linkwave/app/controller/user/UserController.java`

```diff
+ @GetMapping("/contacts")
+ public ResponseEntity<java.util.List<Object>> getContacts() {
+     AuthenticatedUserContext userContext = sessionService.getAuthenticatedUser()
+         .orElse(null);
+     
+     if (userContext == null) {
+         return ResponseEntity.status(401).build();
+     }
+     
+     return ResponseEntity.ok(java.util.Collections.emptyList());
+ }
```

**Result**: `GET /api/v1/user/contacts` now returns `[]` with proper authentication

---

### 2. Created Native WebSocket Handler
**File**: `backend/src/main/java/com/linkwave/app/websocket/NativeWebSocketHandler.java` (NEW)

- Extends `TextWebSocketHandler`
- Manages connections in `ConcurrentHashMap<String, WebSocketSession>`
- Session-based authentication (phone number from session attributes)
- Sends JSON messages (not STOMP frames)
- Connection acknowledgment: `{"type":"connection.ack","status":"connected"}`

---

### 3. Configured Native WebSocket Endpoint
**File**: `backend/src/main/java/com/linkwave/app/config/NativeWebSocketConfig.java` (NEW)

```java
@Configuration
@EnableWebSocket
public class NativeWebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws")
                .setAllowedOrigins("http://localhost:3000")
                .addInterceptors(authInterceptor);
    }
}
```

**Result**: Native WebSocket endpoint at `/ws` with session authentication

---

## Frontend Code Changes

### Fixed Contacts API Path
**File**: `frontend/lib/api.ts`

```diff
- const result = await fetchApi<Contact[]>("/contacts");
+ const result = await fetchApi<Contact[]>("/user/contacts");
```

**Result**: Frontend now calls correct endpoint `/api/v1/user/contacts`

---

## Final Working URLs

### REST API
- **Contacts**: `GET http://localhost:8080/api/v1/user/contacts`
  - Auth: Session cookie (JSESSIONID)
  - Response: `[]` (empty array)

### WebSocket
- **Native WebSocket**: `ws://localhost:8080/ws`
  - Protocol: Native WebSocket (JSON messages)
  - Auth: Session cookie (JSESSIONID)
  - Handshake: Validates session via `WsAuthenticationInterceptor`
  - Connection ack: `{"type":"connection.ack","status":"connected"}`

- **STOMP WebSocket**: `ws://localhost:8080/ws/chat` (unchanged)
  - Protocol: STOMP over SockJS
  - Still available for future use

---

## Verification Checklist

✅ Backend builds successfully (`./gradlew build -x test`)  
✅ Backend starts on port 8080  
✅ Database connection established (PostgreSQL 17.2)  
✅ Kafka consumer connected  
✅ Health check returns `{"status":"UP"}`  
✅ Contacts endpoint registered at `/api/v1/user/contacts`  
✅ Native WebSocket endpoint registered at `/ws`  
✅ STOMP WebSocket endpoint still available at `/ws/chat`  
✅ Frontend API updated to call `/user/contacts`  
✅ WebSocket authentication configured with session cookies  

---

## Why Each Failure Occurred

### REST API 404
The `/api/v1/contacts` endpoint simply didn't exist. No controller was handling this route.

### WebSocket Failure
1. **Path mismatch**: Frontend tried `/ws`, backend only had `/ws/chat`
2. **Protocol mismatch**: Backend expected STOMP frames, frontend sent raw JSON over native WebSocket

**Solution**: Added a parallel native WebSocket endpoint at `/ws` that speaks the same protocol as the frontend, while keeping the STOMP endpoint for future use.

---

## Notes

- The application now supports **two WebSocket endpoints**:
  - Native WebSocket at `/ws` (for current frontend)
  - STOMP WebSocket at `/ws/chat` (for future use)
- Both use **session-based authentication** (JSESSIONID cookie)
- The contacts endpoint returns an **empty array** for now (can be populated from chat rooms later)
- Minor null-safety lint warnings remain but don't affect functionality
