package com.iceekb.dushnila.jpa.repo;

import com.iceekb.dushnila.jpa.entity.Ignore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;


import java.util.List;

public interface IgnoreRepo extends JpaRepository<Ignore, Long> {

    @Query("""
            SELECT ig FROM Ignore ig
            WHERE ig.word = :word
            AND ig.channel.id = :id
            """)
    Ignore findByWordAndChatId(String word, Long id);

    @Query("""
            SELECT ig FROM Ignore ig
            WHERE ig.channel.id = :channelId
            """)
    @Cacheable(value = "ignoredWords", key = "#channelId")
    List<Ignore> findAllByChatId(Long channelId);

    @SuppressWarnings("UnusedReturnValue")
    @Modifying
    @Query("DELETE FROM Ignore ig WHERE ig.channel.id = :channelId")
    @CacheEvict(cacheNames = {"ignoredWords", "ignoreRules"}, allEntries = true)
    int deleteAllByChannelId(Long channelId);

    @Override
    @CacheEvict(cacheNames = {"ignoredWords", "ignoreRules"}, key = "#result.channel.id")
    @NonNull
    <S extends Ignore> S save(@NonNull S entity);

    @Override
    @CacheEvict(cacheNames = {"ignoredWords", "ignoreRules"}, key = "#entity.channel.id")
    void delete(@NonNull Ignore entity);

    @Override
    @CacheEvict(cacheNames = {"ignoredWords", "ignoreRules"}, allEntries = true)
    void deleteById(@NonNull Long id);
}
