package com.iceekb.dushnila.message.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Единый нормализатор текста для правил игнора и реакций.
 * Раньше одна и та же логика (дефисы, пунктуация, пробелы) была продублирована
 * в IgnoreFilterService и ReactionService с тонкими различиями — теперь различия
 * явно выражены выбором метода.
 */
public final class TextNormalizer {

    /** Символы, значимые для правил игнора (маски, упоминания): @ * ? _ - */
    private static final String IGNORE_RULE_PUNCT = "[\\p{P}&&[^@*?_\\-]]";

    /** Символы, значимые для текста сообщения при фильтрации игнора: @ _ - */
    private static final String IGNORE_MESSAGE_PUNCT = "[\\p{P}&&[^@_\\-]]";

    /** Символы, значимые для реакций: только дефис (различает "мама анархия" и "мама - анархия") */
    private static final String REACTION_PUNCT = "[\\p{P}&&[^-]]";

    private TextNormalizer() {
    }

    /** Нормализация значения правила игнора: lowercase, сохраняются @ * ? _ - */
    public static String forIgnoreRule(String s) {
        return normalize(s, true, IGNORE_RULE_PUNCT);
    }

    /** Нормализация текста сообщения для фильтрации игнора: регистр сохраняется, сохраняются @ _ - */
    public static String forIgnoreMessage(String s) {
        return normalize(s, false, IGNORE_MESSAGE_PUNCT);
    }

    /** Нормализация для реакций (и ключей, и текста): lowercase, сохраняется только дефис */
    public static String forReaction(String s) {
        return normalize(s, true, REACTION_PUNCT);
    }

    private static String normalize(String s, boolean lowerCase, String punctuationPattern) {
        if (s == null) {
            return "";
        }
        String result = lowerCase ? s.toLowerCase() : s;
        return result
                // Приводим любые Unicode-дефисы/тире к обычному '-'
                .replaceAll("\\p{Pd}+", "-")
                // Удаляем остальную пунктуацию по заданному шаблону
                .replaceAll(punctuationPattern, StringUtils.SPACE)
                // Нормализуем пробелы вокруг '-': "a-b", "a -b", "a- b" => "a - b"
                .replaceAll("\\s*-\\s*", " - ")
                .replaceAll("\\s+", StringUtils.SPACE)
                .trim();
    }
}
