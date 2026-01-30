package com.linkwave.app.service.room;

import com.linkwave.app.domain.chat.ChatMemberEntity;
import com.linkwave.app.repository.ChatRoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;


@Service
public class RoomMembershipService {

    private static final Logger log = LoggerFactory.getLogger(RoomMembershipService.class);

    private final ChatRoomRepository roomRepository;

    public RoomMembershipService(ChatRoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    
    public boolean isUserInRoom(String userId, String roomId) {
        return roomRepository.findById(roomId)
                .map(room -> room.getMembers().stream()
                        .anyMatch(member -> member.getPhoneNumber().equals(userId)))
                .orElse(false);
    }

    
    public Set<String> getRoomMembers(String roomId) {
        return roomRepository.findById(roomId)
                .map(room -> room.getMembers().stream()
                        .map(ChatMemberEntity::getPhoneNumber)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    
    public int getRoomMemberCount(String roomId) {
        return roomRepository.findById(roomId)
                .map(room -> room.getMembers().size())
                .orElse(0);
    }
}
