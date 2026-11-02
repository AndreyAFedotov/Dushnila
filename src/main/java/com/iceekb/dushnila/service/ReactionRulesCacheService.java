package com.iceekb.dushnila.service;

import com.iceekb.dushnila.jpa.entity.Reaction;
import com.iceekb.dushnila.jpa.repo.ReactionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Кэш скомпилированных правил реакций по каналам.
 * Сбрасывается через @CacheEvict на методах ReactionRepo.
 */
@Service
@RequiredArgsConstructor
public class ReactionRulesCacheService {

    private final ReactionRepo reactionRepo;

    @Cacheable(value = "reactionRules", key = "#channelId")
    public ReactionRules getRules(Long channelId) {
        List<Reaction> reactions = reactionRepo.findAllByChannelId(channelId);
        return ReactionRules.compile(reactions);
    }
}
