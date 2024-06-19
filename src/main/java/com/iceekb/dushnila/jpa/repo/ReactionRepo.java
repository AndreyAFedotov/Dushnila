package com.iceekb.dushnila.jpa.repo;

import com.iceekb.dushnila.jpa.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReactionRepo extends JpaRepository<Reaction, Long> {

    @Query("""
            SELECT r FROM Reaction r
            WHERE r.textFrom = :textFrom
            AND r.channel.id = :id
            """)
    Reaction findByTextFromAndChanelId(String textFrom, Long id);

    @Query("""
            SELECT r FROM Reaction r
            WHERE r.channel.tgId = :tgId
            """)
    List<Reaction> findAllByCatTgId(Long tgId);

    @Query("""
                SELECT r FROM Reaction r
                WHERE r.channel.id = :id
            """)
    List<Reaction> findAllByChannelId(Long id);

    @Query("SELECT EXISTS (SELECT 1 FROM Reaction r WHERE r.textFrom = :textFrom AND r.channel.tgId = :tgId)")
    boolean existsByWordAndChatId(String textFrom, Long tgId);
}
