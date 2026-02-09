package com.linkwave.app.repository;

import com.linkwave.app.domain.chat.ChatMemberEntity;
import com.linkwave.app.domain.chat.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMemberEntity, Long> {

    List<ChatMemberEntity> findByRoom(ChatRoomEntity room);
    @Query("SELECT cm.room FROM ChatMemberEntity cm WHERE cm.phoneNumber = :phoneNumber")
    List<ChatRoomEntity> findRoomsByPhoneNumber(@Param("phoneNumber") String phoneNumber);
    boolean existsByRoomAndPhoneNumber(ChatRoomEntity room, String phoneNumber);
    Optional<ChatMemberEntity> findByRoomAndPhoneNumber(ChatRoomEntity room, String phoneNumber);
}
