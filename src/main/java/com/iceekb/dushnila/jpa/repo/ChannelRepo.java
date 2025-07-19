package com.iceekb.dushnila.jpa.repo;

import com.iceekb.dushnila.jpa.entity.Channel;
import com.iceekb.dushnila.jpa.enums.ChannelApproved;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChannelRepo extends JpaRepository<Channel, Long> {

    @Query("""
            SELECT ch FROM Channel ch
            WHERE ch.tgId = :channelTgId
            """)
    Channel findByTgId(Long channelTgId);

    @Query("""
            SELECT EXISTS (SELECT 1 FROM Channel ch WHERE ch.tgId = :tgId)
            """)
    Boolean existsByTgId(Long tgId);

    @Query(value = "select ch from Channel ch where ch.approved in :approved")
    List<Channel> findByApproved(@Param("approved") List<ChannelApproved> approved);
}
