package com.iceekb.dushnila.jpa.repo;

import com.iceekb.dushnila.jpa.entity.Ignore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IgnoreRepo extends JpaRepository<Ignore, Long> {

    @Query("""
            SELECT ig FROM Ignore ig
            WHERE ig.word = :word
            AND ig.channel.id = :id
            """)
    Ignore findByWordAndChatId(String word, Long id);

    @Query("""
            SELECT EXISTS (SELECT 1 FROM Ignore ig WHERE ig.word = :word AND ig.channel.tgId = :id)
            """)
    Boolean existsByWordAndChatId(String word, Long id);

    @Query("""
            SELECT ig FROM Ignore ig
            WHERE ig.channel.id = :channelId
            """)
    List<Ignore> findAllByChatId(Long channelId);
}
