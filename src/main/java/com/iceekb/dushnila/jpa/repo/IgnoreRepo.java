package com.iceekb.dushnila.jpa.repo;

import com.iceekb.dushnila.jpa.entity.Ignore;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    @Cacheable(value = "ignoredWords", key = "#channelId")
    List<Ignore> findAllByChatId(Long channelId);

    @NotNull
    @Override
    @CacheEvict(value = "ignoredWords", key = "#result.channel.id")
    <S extends Ignore> S save(S entity);

    @Override
    @CacheEvict(value = "ignoredWords", key = "#entity.channel.id")
    void delete(@NotNull Ignore entity);

    @Override
    @CacheEvict(value = "ignoredWords", key = "#entity.channel.id")
    void deleteById(@NotNull Long id);
}
