package com.linkwave.app.controller.chat;

import com.linkwave.app.domain.auth.AuthenticatedUserContext;
import com.linkwave.app.domain.chat.ChatMemberEntity;
import com.linkwave.app.domain.chat.ChatMessageEntity;
import com.linkwave.app.domain.chat.ChatRoomEntity;
import com.linkwave.app.service.chat.ChatService;
import com.linkwave.app.service.session.SessionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for chat room management.
 * 
 * Phase D: Room management endpoints
 * 
 * Endpoints:
 * - POST /api/v1/chat/rooms/direct - Create direct room
 * - POST /api/v1/chat/rooms/group - Create group room
 * - GET /api/v1/chat/rooms - Get user's rooms
 * - GET /api/v1/chat/rooms/{roomId}/messages - Get room messages
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatRoomController {
    
    private final ChatService chatService;
    private final SessionService sessionService;
    
    public ChatRoomController(ChatService chatService, SessionService sessionService) {
        this.chatService = chatService;
        this.sessionService = sessionService;
    }
    
    /**
     * Create a direct (1-1) chat room.
     */
    @PostMapping("/rooms/direct")
    public ResponseEntity<RoomResponse> createDirectRoom(@RequestBody CreateDirectRoomRequest request) {
        AuthenticatedUserContext user = sessionService.getAuthenticatedUser()
            .orElseThrow(() -> new SecurityException("Unauthorized"));
        
        ChatRoomEntity room = chatService.createDirectRoom(user.getPhoneNumber(), request.otherUserPhone());
        
        return ResponseEntity.ok(new RoomResponse(
            room.getId(),
            room.getRoomType().name(),
            room.getName(),
            room.getCreatedAt().toEpochMilli()
        ));
    }
    
    /**
     * Create a group chat room.
     */
    @PostMapping("/rooms/group")
    public ResponseEntity<RoomResponse> createGroupRoom(@RequestBody CreateGroupRoomRequest request) {
        AuthenticatedUserContext user = sessionService.getAuthenticatedUser()
            .orElseThrow(() -> new SecurityException("Unauthorized"));
        
        // Add creator to members if not already included
        List<String> members = request.members();
        if (!members.contains(user.getPhoneNumber())) {
            members = new java.util.ArrayList<>(members);
            members.add(user.getPhoneNumber());
        }
        
        ChatRoomEntity room = chatService.createGroupRoom(request.name(), members);
        
        return ResponseEntity.ok(new RoomResponse(
            room.getId(),
            room.getRoomType().name(),
            room.getName(),
            room.getCreatedAt().toEpochMilli()
        ));
    }
    
    /**
     * Get all rooms the current user is a member of.
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<RoomResponse>> getUserRooms() {
        AuthenticatedUserContext user = sessionService.getAuthenticatedUser()
            .orElseThrow(() -> new SecurityException("Unauthorized"));
        
        List<ChatRoomEntity> rooms = chatService.getUserRooms(user.getPhoneNumber());
        
        List<RoomResponse> response = rooms.stream()
            .map(room -> new RoomResponse(
                room.getId(),
                room.getRoomType().name(),
                room.getName(),
                room.getCreatedAt().toEpochMilli()
            ))
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get messages in a room.
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<MessagesResponse> getRoomMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Page<ChatMessageEntity> messagesPage = chatService.getRoomMessages(
            roomId, 
            PageRequest.of(page, size)
        );
        
        List<MessageResponse> messages = messagesPage.getContent().stream()
            .map(msg -> new MessageResponse(
                msg.getId(),
                msg.getRoom().getId(),
                msg.getSenderPhone(),
                msg.getBody(),
                msg.getSentAt().toEpochMilli()
            ))
            .toList();
        
        return ResponseEntity.ok(new MessagesResponse(
            messages,
            messagesPage.getTotalElements(),
            messagesPage.getTotalPages(),
            messagesPage.getNumber()
        ));
    }
    
    /**
     * Get members of a room.
     */
    @GetMapping("/rooms/{roomId}/members")
    public ResponseEntity<List<MemberResponse>> getRoomMembers(@PathVariable String roomId) {
        List<ChatMemberEntity> members = chatService.getRoomMembers(roomId);
        
        List<MemberResponse> response = members.stream()
            .map(member -> new MemberResponse(
                member.getPhoneNumber(),
                member.getJoinedAt().toEpochMilli()
            ))
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    // Request/Response DTOs
    
    public record CreateDirectRoomRequest(String otherUserPhone) {}
    
    public record CreateGroupRoomRequest(String name, List<String> members) {}
    
    public record RoomResponse(String id, String type, String name, long createdAt) {}
    
    public record MessageResponse(String id, String roomId, String sender, String body, long sentAt) {}
    
    public record MessagesResponse(List<MessageResponse> messages, long total, int totalPages, int currentPage) {}
    
    public record MemberResponse(String phoneNumber, long joinedAt) {}
}
