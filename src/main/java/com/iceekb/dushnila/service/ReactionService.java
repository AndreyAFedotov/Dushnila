package com.iceekb.dushnila.service;

import com.iceekb.dushnila.jpa.entity.Reaction;
import com.iceekb.dushnila.jpa.repo.ReactionRepo;
import com.iceekb.dushnila.properties.LastMessageTxt;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Выбор и применение реакций (слова/фразы) для входящего сообщения.
 *
 * Поведение намеренно повторяет прежний MessagesService#checkReaction, чтобы не было регрессий:
 * - единая нормализация (с сохранением значимости дефиса)
 * - фразовые реакции применяются до слов
 * - фразы "покрывают" символы, чтобы слова внутри фразы не срабатывали
 * - при конфликте фраз, нормализующихся в один ключ, выбирается "лучший" кандидат
 */
@Service
@RequiredArgsConstructor
public class ReactionService {
    private final ReactionRepo reactionRepo;

    public void applyReaction(LastMessageTxt lastMessage) {
        if (lastMessage == null
                || lastMessage.getChannel() == null
                || lastMessage.getChannel().getId() == null
                || StringUtils.isBlank(lastMessage.getReceivedMessage())) {
            return;
        }

        List<Reaction> reactions = reactionRepo.findAllByChannelId(lastMessage.getChannel().getId());
        if (reactions.isEmpty()) return;

        // Единая нормализация для текста сообщения и ключей реакций.
        // Важно: дефис/тире должны оставаться значимыми, иначе разные реакции
        // "мама анархия" и "мама - анархия" схлопнутся в один ключ.
        Function<String, String> normalizer = s -> {
            if (s == null) return "";
            return s.toLowerCase()
                    // Приводим любые Unicode-дефисы/тире к обычному '-'
                    .replaceAll("\\p{Pd}+", "-")
                    // Удаляем всю остальную Unicode-пунктуацию, КРОМЕ '-' (он нужен для различения реакций)
                    .replaceAll("[\\p{P}&&[^-]]", " ")
                    // Нормализуем пробелы вокруг '-': "a-b", "а -b", "a- b" => "a - b"
                    .replaceAll("\\s*-\\s*", " - ")
                    .replaceAll("\\s+", " ")
                    .trim();
        };

        String originalLower = lastMessage.getReceivedMessage().toLowerCase();
        String normalisedMessage = normalizer.apply(lastMessage.getReceivedMessage());

        List<Reaction> phraseReactions = reactions.stream()
                .filter(r -> normalizer.apply(r.getTextFrom()).contains(" "))
                .toList();

        List<Reaction> wordReactions = reactions.stream()
                .filter(r -> !normalizer.apply(r.getTextFrom()).contains(" "))
                .toList();

        Set<Integer> coveredPositions = new HashSet<>();
        StringJoiner result = new StringJoiner(". ");

        // Сначала обрабатываем фразы
        // Если несколько фраз нормализуются в один ключ — выбираем "лучшее" совпадение:
        // 1) точное совпадение по исходному тексту приоритетнее
        // 2) иначе совпадение по нормализованному тексту
        //noinspection ClassCanBeRecord
        class PhraseCandidate {
            final String originalKey;
            final String response;
            final int score; // 2 = exact, 1 = normalized

            PhraseCandidate(String originalKey, String response, int score) {
                this.originalKey = originalKey;
                this.response = response;
                this.score = score;
            }
        }

        Map<String, PhraseCandidate> bestByNormalized = new java.util.HashMap<>();
        for (Reaction r : phraseReactions) {
            String originalKey = (r.getTextFrom() == null) ? "" : r.getTextFrom().toLowerCase();
            String normalizedKey = normalizer.apply(r.getTextFrom());
            if (StringUtils.isBlank(normalizedKey)) continue;

            int score = 0;
            if (StringUtils.isNotBlank(originalKey) && originalLower.contains(originalKey)) {
                score = 2;
            } else if (normalisedMessage.contains(normalizedKey)) {
                score = 1;
            }
            if (score == 0) continue;

            PhraseCandidate current = bestByNormalized.get(normalizedKey);
            if (current == null
                    || score > current.score
                    || (score == current.score && originalKey.length() > current.originalKey.length())) {
                bestByNormalized.put(normalizedKey, new PhraseCandidate(originalKey, r.getTextTo(), score));
            }
        }

        bestByNormalized.forEach((phrase, cand) -> {
            int index = 0;
            boolean phraseFound = false;
            while ((index = normalisedMessage.indexOf(phrase, index)) != -1) {
                for (int i = index; i < index + phrase.length(); i++) {
                    coveredPositions.add(i);
                }
                phraseFound = true;
                index += phrase.length();
            }
            if (phraseFound) {
                result.add(cand.response);
            }
        });

        // Затем обрабатываем слова, исключая те, что были покрыты фразами
        Map<String, String> wordMap = wordReactions.stream()
                .collect(Collectors.toMap(
                        r -> normalizer.apply(r.getTextFrom()),
                        Reaction::getTextTo,
                        (existing, replacement) -> existing
                ));

        wordMap.forEach((word, value) -> {
            // Важно: \b в Java по умолчанию плохо работает с кириллицей (без UNICODE_CHARACTER_CLASS),
            // поэтому используем Unicode-границы "не буква/цифра/_" вокруг слова.
            String pattern = "(?iu)(?<![\\p{L}\\p{N}_])" + Pattern.quote(word) + "(?![\\p{L}\\p{N}_])";
            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(normalisedMessage);

            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();

                boolean isCovered = false;
                for (int i = start; i < end; i++) {
                    if (coveredPositions.contains(i)) {
                        isCovered = true;
                        break;
                    }
                }

                if (!isCovered) {
                    result.add(value);
                    break; // Достаточно одного совпадения для слова
                }
            }
        });

        String response = result.toString();
        if (StringUtils.isNotBlank(response)) {
            lastMessage.setResponse(response);
        }
    }
}


