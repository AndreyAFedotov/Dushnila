package com.iceekb.dushnila.service;

import com.iceekb.dushnila.jpa.entity.Ignore;
import com.iceekb.dushnila.jpa.repo.IgnoreRepo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IgnoreRulesCacheService {
    private final IgnoreRepo ignoreRepo;

    @Cacheable(value = "ignoreRules", key = "#chatId")
    @NotNull
    public IgnoreFilterService.IgnoreRules getRules(Long chatId) {
        List<String> rawIgnores = ignoreRepo.findAllByChatId(chatId).stream()
                .map(Ignore::getWord)
                .filter(StringUtils::isNotBlank)
                .toList();
        return IgnoreFilterService.IgnoreRules.compile(rawIgnores);
    }
}


