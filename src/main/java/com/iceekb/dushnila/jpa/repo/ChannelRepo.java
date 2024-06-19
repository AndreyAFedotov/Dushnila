package com.iceekb.dushnila.jpa.repo;

import com.iceekb.dushnila.jpa.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ChannelRepo extends JpaRepository<Channel, Long> {

    @Query("""
            SELECT ch FROM Channel ch
            WHERE ch.tgId = :channelTgId
            """)
    Channel findByTgId(Long channelTgId);

    @Query("""
            SELECT ch FROM Channel ch
            WHERE ch.chatName = :channelName
            """)
    Channel findByChatName(String channelName);

    @Query("""
            SELECT EXISTS (SELECT 1 FROM Channel ch WHERE ch.tgId = :tgId)
            """)
    Boolean existsByTgId(Long tgId);
}
