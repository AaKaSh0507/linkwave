package com.linkwave.app.service.chat;

import com.linkwave.app.domain.chat.*;
import com.linkwave.app.repository.ChatMemberRepository;
import com.linkwave.app.repository.ChatMessageRepository;
import com.linkwave.app.repository.ChatRoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing chat rooms, members, and messages.
 * 
 * Phase D: Room-based messaging business logic
 * 
 * Responsibilities:
 * - Create and manage chat rooms
 * - Add/remove members
 * - Validate message permissions
 * - Publish messages to Kafka
 */
@Service
public class ChatService {
    
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    
    private final ChatRoomRepository roomRepository;
    private final ChatMemberRepository memberRepository;
    private final ChatMessageRepository messageRepository;
    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;
    
    public ChatService(ChatRoomRepository roomRepository,
                      ChatMemberRepository memberRepository,
                      ChatMessageRepository messageRepository,
                      KafkaTemplate<String, ChatMessage> kafkaTemplate) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * Create a new direct (1-1) chat room between two users.
     */
    @Transactional
    public ChatRoomEntity createDirectRoom(String phoneNumber1, String phoneNumber2) {
        Instant now = Instant.now();
        
        ChatRoomEntity room = new ChatRoomEntity(
            UUID.randomUUID().toString(),
            ChatRoomEntity.RoomType.DIRECT,
            null, // Direct rooms don't have names
            now,
            now
        );
        
        room = roomRepository.save(room);
        
        // Add both members
        memberRepository.save(new ChatMemberEntity(room, phoneNumber1, now));
        memberRepository.save(new ChatMemberEntity(room, phoneNumber2, now));
        
        log.info("Created direct room {} for users {} and {}", 
                room.getId(), maskPhone(phoneNumber1), maskPhone(phoneNumber2));
        
        return room;
    }
    
    /**
     * Create a new group chat room.
     */
    @Transactional
    public ChatRoomEntity createGroupRoom(String name, List<String> memberPhoneNumbers) {
        Instant now = Instant.now();
        
        ChatRoomEntity room = new ChatRoomEntity(
            UUID.randomUUID().toString(),
            ChatRoomEntity.RoomType.GROUP,
            name,
            now,
            now
        );
        
        room = roomRepository.save(room);
        
        // Add all members
        for (String phoneNumber : memberPhoneNumbers) {
            memberRepository.save(new ChatMemberEntity(room, phoneNumber, now));
        }
        
        log.info("Created group room {} with {} members", room.getId(), memberPhoneNumbers.size());
        
        return room;
    }
    
    /**
     * Send a message to a room.
     * Validates sender is a member, then publishes to Kafka.
     */
    public ChatMessage sendMessage(String roomId, String senderPhoneNumber, String body) {
        // Validate room exists
        ChatRoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        
        // Validate sender is a member
        if (!memberRepository.existsByRoomAndPhoneNumber(room, senderPhoneNumber)) {
            throw new SecurityException("User is not a member of this room");
        }
        
        // Create message
        ChatMessage message = ChatMessage.create(roomId, senderPhoneNumber, body);
        
        // Publish to Kafka - key by roomId for ordering
        kafkaTemplate.send("chat.messages", roomId, message);
        
        log.info("Published message {} to room {}", message.getMessageId(), roomId);
        
        return message;
    }
    
    /**
     * Get messages in a room (paginated).
     */
    @Transactional(readOnly = true)
    public Page<ChatMessageEntity> getRoomMessages(String roomId, Pageable pageable) {
        ChatRoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        
        return messageRepository.findByRoomOrderBySentAtDesc(room, pageable);
    }
    
    /**
     * Get all rooms a user is member of.
     */
    @Transactional(readOnly = true)
    public List<ChatRoomEntity> getUserRooms(String phoneNumber) {
        return memberRepository.findRoomsByPhoneNumber(phoneNumber);
    }
    
    /**
     * Persist a message (called by Kafka consumer).
     */
    @Transactional
    public void persistMessage(ChatMessage message) {
        ChatRoomEntity room = roomRepository.findById(message.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + message.getRoomId()));
        
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setId(message.getMessageId());
        entity.setRoom(room);
        entity.setSenderPhone(message.getSenderPhoneNumber());
        entity.setBody(message.getBody());
        entity.setSentAt(Instant.ofEpochMilli(message.getSentAt()));
        entity.setTtlDays(message.getTtlDays());
        
        messageRepository.save(entity);
        
        log.debug("Persisted message {} to database", message.getMessageId());
    }
    
    /**
     * Get all members of a room.
     */
    @Transactional(readOnly = true)
    public List<ChatMemberEntity> getRoomMembers(String roomId) {
        ChatRoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        
        return memberRepository.findByRoom(room);
    }
    
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 4) + "***";
    }
}
