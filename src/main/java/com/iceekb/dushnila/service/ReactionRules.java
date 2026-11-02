package com.iceekb.dushnila.service;

import com.iceekb.dushnila.jpa.entity.Reaction;
import com.iceekb.dushnila.message.util.TextNormalizer;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Скомпилированные правила реакций канала.
 * Regex'ы компилируются один раз при загрузке правил (и кэшируются),
 * а не на каждое входящее сообщение. Объект неизменяем и потокобезопасен.
 */
public final class ReactionRules {

    private record PhraseRule(String originalKey, String normalizedKey, String response) {
    }

    private record WordRule(Pattern pattern, String response) {
    }

    private final List<PhraseRule> phrases;
    private final List<WordRule> words;

    private ReactionRules(List<PhraseRule> phrases, List<WordRule> words) {
        this.phrases = phrases;
        this.words = words;
    }

    public boolean isEmpty() {
        return phrases.isEmpty() && words.isEmpty();
    }

    public static ReactionRules compile(List<Reaction> reactions) {
        List<PhraseRule> phrases = new ArrayList<>();
        // Дедупликация слов по нормализованному ключу — достаточно одного правила
        Map<String, WordRule> words = new HashMap<>();

        for (Reaction r : reactions) {
            String originalKey = (r.getTextFrom() == null) ? "" : r.getTextFrom().toLowerCase();
            String normalizedKey = TextNormalizer.forReaction(r.getTextFrom());
            if (StringUtils.isBlank(normalizedKey)) {
                continue;
            }

            if (normalizedKey.contains(" ")) {
                phrases.add(new PhraseRule(originalKey, normalizedKey, r.getTextTo()));
            } else {
                // \b в Java по умолчанию плохо работает с кириллицей,
                // поэтому используем Unicode-границы "не буква/цифра/_" вокруг слова.
                Pattern pattern = Pattern.compile(
                        "(?iu)(?<![\\p{L}\\p{N}_])" + Pattern.quote(normalizedKey) + "(?![\\p{L}\\p{N}_])");
                words.putIfAbsent(normalizedKey, new WordRule(pattern, r.getTextTo()));
            }
        }

        return new ReactionRules(List.copyOf(phrases), List.copyOf(words.values()));
    }

    /**
     * @param originalLower      исходное сообщение в нижнем регистре
     * @param normalisedMessage  сообщение после {@link TextNormalizer#forReaction(String)}
     * @return собранный ответ или пустая строка, если реакций не нашлось
     */
    public String apply(String originalLower, String normalisedMessage) {
        Set<Integer> coveredPositions = new HashSet<>();
        StringJoiner result = new StringJoiner(". ");

        // Сначала фразы: выбираем лучший кандидат на нормализованный ключ
        Map<String, PhraseRule> bestByNormalized = new HashMap<>();
        for (PhraseRule rule : phrases) {
            int score = 0;
            if (StringUtils.isNotBlank(rule.originalKey()) && originalLower.contains(rule.originalKey())) {
                score = 2; // точное совпадение
            } else if (normalisedMessage.contains(rule.normalizedKey())) {
                score = 1; // совпадение после нормализации
            }
            if (score == 0) {
                continue;
            }

            PhraseRule current = bestByNormalized.get(rule.normalizedKey());
            if (current == null
                    || score > scoreOf(current, originalLower, normalisedMessage)
                    || (score == scoreOf(current, originalLower, normalisedMessage)
                        && rule.originalKey().length() > current.originalKey().length())) {
                bestByNormalized.put(rule.normalizedKey(), rule);
            }
        }

        bestByNormalized.forEach((phrase, rule) -> {
            int index = 0;
            boolean phraseFound = false;
            while ((index = normalisedMessage.indexOf(rule.normalizedKey(), index)) != -1) {
                for (int i = index; i < index + rule.normalizedKey().length(); i++) {
                    coveredPositions.add(i);
                }
                phraseFound = true;
                index += rule.normalizedKey().length();
            }
            if (phraseFound) {
                result.add(rule.response());
            }
        });

        // Затем слова, исключая позиции, покрытые фразами
        for (WordRule rule : words) {
            Matcher matcher = rule.pattern().matcher(normalisedMessage);
            while (matcher.find()) {
                boolean isCovered = false;
                for (int i = matcher.start(); i < matcher.end(); i++) {
                    if (coveredPositions.contains(i)) {
                        isCovered = true;
                        break;
                    }
                }
                if (!isCovered) {
                    result.add(rule.response());
                    break; // Достаточно одного совпадения для слова
                }
            }
        }

        return result.toString();
    }

    private static int scoreOf(PhraseRule rule, String originalLower, String normalisedMessage) {
        if (StringUtils.isNotBlank(rule.originalKey()) && originalLower.contains(rule.originalKey())) {
            return 2;
        }
        return normalisedMessage.contains(rule.normalizedKey()) ? 1 : 0;
    }
}
