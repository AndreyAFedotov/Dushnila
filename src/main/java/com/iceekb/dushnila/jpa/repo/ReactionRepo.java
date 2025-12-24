package com.iceekb.dushnila.jpa.repo;

import com.iceekb.dushnila.jpa.entity.Reaction;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

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
    @Cacheable(value = "reactions", key = "#id")
    List<Reaction> findAllByChannelId(Long id);

    @Deprecated
    @Query("SELECT EXISTS (SELECT 1 FROM Reaction r WHERE r.textFrom = :textFrom AND r.channel.tgId = :tgId)")
    boolean existsByWordAndChatId(String textFrom, Long tgId);

    @SuppressWarnings("UnusedReturnValue")
    @Modifying
    @Query("DELETE FROM Reaction r WHERE r.channel.id = :channelId")
    @CacheEvict(value = "reactions", allEntries = true)
    int deleteAllByChannelId(Long channelId);

    // Переопределяем методы для автоматической очистки кэша
    @Override
    @CacheEvict(value = "reactions", key = "#result.channel.id")
    @NonNull
    <S extends Reaction> S save(@NonNull S entity);

    @Override
    @CacheEvict(value = "reactions", key = "#entity.channel.id")
    void delete(@NonNull Reaction entity);

    @Override
    @CacheEvict(value = "reactions", allEntries = true)
    void deleteById(@NonNull Long id);
}
