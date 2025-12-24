package com.iceekb.dushnila.speller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceekb.dushnila.message.TransCharReplace;
import com.iceekb.dushnila.message.dto.SpellerIncomingDataWord;
import com.iceekb.dushnila.message.responses.AutoResponseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SpellerServiceLogicTest {

    @Mock private AutoResponseService autoResponseService;

    private static SpellerIncomingDataWord word(String w, List<String> suggestions) {
        SpellerIncomingDataWord d = new SpellerIncomingDataWord();
        d.setWord(w);
        d.setS(suggestions);
        return d;
    }

    private static Object invokePrivate(Object target, String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = target.getClass().getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private SpellerService createService() {
        return new SpellerService(new TransCharReplace(), new ObjectMapper(), autoResponseService);
    }

    @Test
    void filterData_skipsNullWordsAndEmptySuggestions_withoutThrow() {
        SpellerService service = createService();

        SpellerIncomingDataWord wNull = null;
        SpellerIncomingDataWord wNoSuggestions = word("hello", null);
        SpellerIncomingDataWord wEmptySuggestions = word("world", List.of());
        SpellerIncomingDataWord wSame = word("кот", List.of("кот"));
        SpellerIncomingDataWord wDiff = word("кит", List.of("кот"));

        assertDoesNotThrow(() -> {
            try {
                @SuppressWarnings({"unchecked", "ConstantValue"})
                List<SpellerIncomingDataWord> filtered = (List<SpellerIncomingDataWord>) invokePrivate(
                        service,
                        "filterData",
                        new Class[]{SpellerIncomingDataWord[].class},
                        (Object) new SpellerIncomingDataWord[]{wNull, wNoSuggestions, wEmptySuggestions, wSame, wDiff}
                );
                assertEquals(1, filtered.size());
                assertEquals("кит", filtered.get(0).getWord());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void extractPairs_ignoresInvalidEntries_andMapsToFirstSuggestion() throws Exception {
        SpellerService service = createService();

        List<SpellerIncomingDataWord> data = List.of(
                word("a", null),
                word("b", List.of()),
                word(null, List.of("x")),
                word("кит", List.of("кот", "кит"))
        );

        @SuppressWarnings("unchecked")
        Map<String, String> pairs = (Map<String, String>) invokePrivate(
                service,
                "extractPairs",
                new Class[]{List.class},
                data
        );

        assertEquals(1, pairs.size());
        assertTrue(pairs.containsKey("кит"));
        assertEquals("кот", pairs.get("кит"));
    }
}


