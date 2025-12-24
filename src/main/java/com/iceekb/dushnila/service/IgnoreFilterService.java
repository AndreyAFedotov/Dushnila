package com.iceekb.dushnila.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IgnoreFilterService {
    private final IgnoreRulesCacheService ignoreRulesCacheService;

    public String filterMessage(Long chatId, String message) {
        if (chatId == null || StringUtils.isBlank(message)) {
            return "";
        }
        IgnoreRules rules = ignoreRulesCacheService.getRules(chatId);
        return rules.apply(message);
    }

    public static final class IgnoreRules {
        private final Set<String> ignoredWords;              // точные слова (lower)
        private final List<Pattern> phrasePatterns;          // точные фразы
        private final List<Pattern> phraseMaskPatterns;      // маски по строке (в т.ч. "мама*анархия")
        private final List<Pattern> tokenMaskPatterns;       // маски по токену ("прив*")

        private IgnoreRules(Set<String> ignoredWords,
                            List<Pattern> phrasePatterns,
                            List<Pattern> phraseMaskPatterns,
                            List<Pattern> tokenMaskPatterns) {
            this.ignoredWords = ignoredWords;
            this.phrasePatterns = phrasePatterns;
            this.phraseMaskPatterns = phraseMaskPatterns;
            this.tokenMaskPatterns = tokenMaskPatterns;
        }

        static IgnoreRules compile(List<String> rawIgnores) {
            Function<String, String> ignoreNormalizer = s -> {
                if (s == null) return "";
                return s.toLowerCase()
                        .replaceAll("\\p{Pd}+", "-")
                        // удаляем пунктуацию, КРОМЕ @, '_', '-', '*', '?'
                        // '_' (underscore) является Unicode-пунктуацией (Pc), но для наших кейсов он значим.
                        .replaceAll("[\\p{P}&&[^@*?_\\-]]", StringUtils.SPACE)
                        .replaceAll("\\s*-\\s*", " - ")
                        .replaceAll("\\s+", StringUtils.SPACE)
                        .trim();
            };

            List<String> normalized = rawIgnores.stream()
                    .map(ignoreNormalizer)
                    .filter(StringUtils::isNotBlank)
                    .toList();

            Set<String> ignoredWords = normalized.stream()
                    .filter(v -> !v.contains(StringUtils.SPACE))
                    .filter(v -> !v.contains("*") && !v.contains("?"))
                    .collect(Collectors.toSet());

            List<Pattern> phrasePatterns = normalized.stream()
                    .filter(v -> v.contains(StringUtils.SPACE))
                    .filter(v -> !v.contains("*") && !v.contains("?"))
                    .map(IgnoreFilterService::compileExactPhrase)
                    .toList();

            List<String> masks = normalized.stream()
                    .filter(v -> v.contains("*") || v.contains("?"))
                    .toList();

            List<Pattern> phraseMaskPatterns = masks.stream()
                    .map(IgnoreFilterService::compileGlobPhrase)
                    .toList();

            List<Pattern> tokenMaskPatterns = masks.stream()
                    .filter(m -> !m.contains(StringUtils.SPACE))
                    .map(IgnoreFilterService::compileGlobToken)
                    .toList();

            return new IgnoreRules(ignoredWords, phrasePatterns, phraseMaskPatterns, tokenMaskPatterns);
        }

        String apply(String message) {
            // 1) Вырезаем фразы и маски по всей строке
            String withoutPhrases = normalizeMessage(message);
            for (Pattern p : phrasePatterns) {
                withoutPhrases = p.matcher(withoutPhrases).replaceAll(" ");
            }
            for (Pattern p : phraseMaskPatterns) {
                withoutPhrases = p.matcher(withoutPhrases).replaceAll(" ");
            }
            withoutPhrases = withoutPhrases.replaceAll("\\s+", " ").trim();

            // 2) Фильтруем токены
            List<String> tokens = Arrays.stream(withoutPhrases.split("\\s+"))
                    .filter(StringUtils::isNotBlank)
                    .filter(w -> w.charAt(0) != '@')
                    .filter(w -> !ignoredWords.contains(w.toLowerCase()))
                    .filter(w -> tokenMaskPatterns.stream().noneMatch(p -> p.matcher(w).matches()))
                    .collect(Collectors.toList());

            // 3) Убираем "сиротские" дефисы
            List<String> compact = dropOrphanHyphens(tokens);
            return String.join(StringUtils.SPACE, compact).trim();
        }

        private static String normalizeMessage(String s) {
            if (s == null) return "";
            return s
                    .replaceAll("\\p{Pd}+", "-")
                    // '_' оставляем значимым (см. ignoreNormalizer)
                    .replaceAll("[\\p{P}&&[^@_\\-]]", StringUtils.SPACE)
                    .replaceAll("\\s*-\\s*", " - ")
                    .replaceAll("\\s+", StringUtils.SPACE)
                    .trim();
        }

        private static List<String> dropOrphanHyphens(List<String> tokens) {
            List<String> compact = new ArrayList<>(tokens.size());
            for (int i = 0; i < tokens.size(); i++) {
                String t = tokens.get(i);
                if (!"-".equals(t)) {
                    compact.add(t);
                    continue;
                }
                boolean hasLeft = !compact.isEmpty() && !"-".equals(compact.get(compact.size() - 1));
                boolean hasRight = (i + 1) < tokens.size() && !"-".equals(tokens.get(i + 1));
                if (hasLeft && hasRight) {
                    compact.add(t);
                }
            }
            return compact;
        }
    }

    private static Pattern compileExactPhrase(String phrase) {
        return Pattern.compile("(?<!\\S)" + Pattern.quote(phrase) + "(?=\\s|$)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private static Pattern compileGlobToken(String glob) {
        // '*' = любое количество символов внутри токена (без пробелов)
        // '?' = ровно один символ внутри токена (без пробелов)
        StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*') {
                sb.append("[^\\s]*");
            } else if (c == '?') {
                sb.append("[^\\s]");
            } else {
                sb.append(Pattern.quote(String.valueOf(c)));
            }
        }
        sb.append("$");
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private static Pattern compileGlobPhrase(String globPhrase) {
        // Компилируем маску по всей строке.
        // Границы: начало совпадения должно начинаться на границе токена, и заканчиваться на границе токена.
        // '*' = любая последовательность символов (включая пробелы)
        // '?' = один любой символ (включая пробел)
        StringBuilder sb = new StringBuilder();
        sb.append("(?<!\\S)");

        for (int i = 0; i < globPhrase.length(); i++) {
            char c = globPhrase.charAt(i);
            if (c == '*') {
                sb.append(".*?");
            } else if (c == '?') {
                sb.append(".");
            } else if (Character.isWhitespace(c)) {
                sb.append("\\s+");
            } else {
                sb.append(Pattern.quote(String.valueOf(c)));
            }
        }

        sb.append("(?=\\s|$)");
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}


