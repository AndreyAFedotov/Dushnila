package com.iceekb.dushnila.service;

import com.iceekb.dushnila.message.util.TextNormalizer;
import com.iceekb.dushnila.properties.LastMessageTxt;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionRulesCacheService rulesCacheService;

    public void applyReaction(LastMessageTxt lastMessage) {
        if (lastMessage == null
                || lastMessage.getChannel() == null
                || lastMessage.getChannel().getId() == null
                || StringUtils.isBlank(lastMessage.getReceivedMessage())) {
            return;
        }

        ReactionRules rules = rulesCacheService.getRules(lastMessage.getChannel().getId());
        if (rules.isEmpty()) {
            return;
        }

        String originalLower = lastMessage.getReceivedMessage().toLowerCase();
        String normalisedMessage = TextNormalizer.forReaction(lastMessage.getReceivedMessage());

        String response = rules.apply(originalLower, normalisedMessage);
        if (StringUtils.isNotBlank(response)) {
            lastMessage.setResponse(response);
        }
    }
}
