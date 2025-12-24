package com.iceekb.dushnila.jpa.repo;

import com.iceekb.dushnila.jpa.entity.Reaction;
import jakarta.validation.constraints.NotNull;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@SuppressWarnings("NullableProblems")
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

    @Query("SELECT EXISTS (SELECT 1 FROM Reaction r WHERE r.textFrom = :textFrom AND r.channel.tgId = :tgId)")
    boolean existsByWordAndChatId(String textFrom, Long tgId);

    // Переопределяем методы для автоматической очистки кэша
    @NotNull
    @Override
    @CacheEvict(value = "reactions", key = "#result.channel.id")
    <S extends Reaction> S save(@NotNull S entity);

    @Override
    @CacheEvict(value = "reactions", key = "#entity.channel.id")
    void delete(@NotNull Reaction entity);

    @Override
    @CacheEvict(value = "reactions", allEntries = true)
    void deleteById(@NotNull Long id);
}
