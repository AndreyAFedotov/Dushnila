package com.iceekb.dushnila.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransCharReplaceServiceTest {

    @Test
    void modifyTransString_replacesKnownMappingsAndKeepsUnknown() {
        TransCharReplaceService svc = new TransCharReplaceService();
        // "Ghbdtn" = "Привет" на раскладке EN->RU
        assertEquals("Привет", svc.modifyTransString("Ghbdtn"));
        // неизвестные символы не меняем
        assertEquals("123!", svc.modifyTransString("123!"));
    }

    @Test
    void isTrans_trueWhenAtLeast70PercentPairsLookLikeTranslit() {
        TransCharReplaceService svc = new TransCharReplaceService();
        Map<String, String> pairs = new LinkedHashMap<>();
        // 7 из 10: EN->RU
        pairs.put("a", "ф");
        pairs.put("b", "и");
        pairs.put("c", "с");
        pairs.put("d", "в");
        pairs.put("e", "у");
        pairs.put("f", "а");
        pairs.put("g", "п");
        // 3 "не транслит" (RU->RU)
        pairs.put("кот", "кот");
        pairs.put("мир", "мир");
        pairs.put("тест", "тест");

        assertTrue(svc.isTrans(pairs));
    }

    @Test
    void isTrans_falseWhenLessThan70PercentPairsLookLikeTranslit() {
        TransCharReplaceService svc = new TransCharReplaceService();
        Map<String, String> pairs = new LinkedHashMap<>();
        // 6 из 10: EN->RU
        pairs.put("a", "ф");
        pairs.put("b", "и");
        pairs.put("c", "с");
        pairs.put("d", "в");
        pairs.put("e", "у");
        pairs.put("f", "а");
        // 4 "не транслит"
        pairs.put("кот", "кот");
        pairs.put("мир", "мир");
        pairs.put("тест", "тест");
        pairs.put("привет", "привет");

        assertFalse(svc.isTrans(pairs));
    }
}


