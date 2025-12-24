package com.iceekb.dushnila.service;

import com.iceekb.dushnila.jpa.entity.Ignore;
import com.iceekb.dushnila.jpa.repo.IgnoreRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IgnoreFilterServiceTest {

    @Mock private IgnoreRepo ignoreRepo;

    private IgnoreFilterService createService() {
        return new IgnoreFilterService(ignoreRepo);
    }

    @Test
    void removesIgnoredWordsMentionsAndPunctuationExceptAt() {
        IgnoreFilterService service = createService();
        when(ignoreRepo.findAllByChatId(1L)).thenReturn(List.of(
                Ignore.builder().word("мир").build(),
                Ignore.builder().word("hello").build()
        ));

        String result = service.filterMessage(1L, "Привет, @bob! мир... hello!!! мама — анархия @ann");
        assertEquals("Привет мама - анархия", result);
    }

    @Test
    void removesIgnoredPhrase() {
        IgnoreFilterService service = createService();
        when(ignoreRepo.findAllByChatId(1L)).thenReturn(List.of(
                Ignore.builder().word("очень плохо").build()
        ));

        String result = service.filterMessage(1L, "Это ОЧЕНЬ плохо!!");
        assertEquals("Это", result);
    }

    @Test
    void removesIgnoredWordByMask() {
        IgnoreFilterService service = createService();
        when(ignoreRepo.findAllByChatId(1L)).thenReturn(List.of(
                Ignore.builder().word("прив*").build()
        ));

        String result = service.filterMessage(1L, "Привет приветик привет-привет");
        assertEquals("", result);
    }

    @Test
    void removesIgnoredPhraseByMaskAcrossDashVariants() {
        IgnoreFilterService service = createService();
        when(ignoreRepo.findAllByChatId(1L)).thenReturn(List.of(
                Ignore.builder().word("мама*анархия").build()
        ));

        String result = service.filterMessage(1L, "Привет мама — анархия конец");
        assertEquals("Привет конец", result);
    }

    @Test
    void phraseDoesNotMatchInsideToken_boundaryRespected() {
        IgnoreFilterService service = createService();
        when(ignoreRepo.findAllByChatId(1L)).thenReturn(List.of(
                Ignore.builder().word("очень плохо").build()
        ));

        String result = service.filterMessage(1L, "неочень плохо");
        assertEquals("неочень плохо", result);
    }

    @Test
    void wordIgnoreNormalizesPunctuationInIgnoreValue() {
        IgnoreFilterService service = createService();
        when(ignoreRepo.findAllByChatId(1L)).thenReturn(List.of(
                Ignore.builder().word("hello!!!").build()
        ));

        String result = service.filterMessage(1L, "hello!!! мир");
        assertEquals("мир", result);
    }

    @Test
    void maskWithQuestionMarkMatchesExactlyOneChar() {
        IgnoreFilterService service = createService();
        when(ignoreRepo.findAllByChatId(1L)).thenReturn(List.of(
                Ignore.builder().word("ко?").build()
        ));

        String result = service.filterMessage(1L, "кот код ко коты");
        assertEquals("ко коты", result);
    }

    @Test
    void hyphenTokenRemovedWhenNeighborsRemoved_leftSide() {
        IgnoreFilterService service = createService();
        when(ignoreRepo.findAllByChatId(1L)).thenReturn(List.of(
                Ignore.builder().word("мама").build()
        ));

        String result = service.filterMessage(1L, "мама - анархия");
        assertEquals("анархия", result);
    }

    @Test
    void hyphenTokenRemovedWhenNeighborsRemoved_rightSide() {
        IgnoreFilterService service = createService();
        when(ignoreRepo.findAllByChatId(1L)).thenReturn(List.of(
                Ignore.builder().word("анархия").build()
        ));

        String result = service.filterMessage(1L, "мама - анархия");
        assertEquals("мама", result);
    }

    @Test
    void hyphenTokenPreservedWhenBothSidesPresent() {
        IgnoreFilterService service = createService();
        when(ignoreRepo.findAllByChatId(1L)).thenReturn(List.of());

        String result = service.filterMessage(1L, "мама - анархия");
        assertEquals("мама - анархия", result);
    }

    @Test
    void phraseWithHyphenMatchesDashVariants() {
        IgnoreFilterService service = createService();
        when(ignoreRepo.findAllByChatId(1L)).thenReturn(List.of(
                Ignore.builder().word("мама - анархия").build()
        ));

        assertEquals("Привет конец", service.filterMessage(1L, "Привет мама-анархия конец"));
        assertEquals("Привет конец", service.filterMessage(1L, "Привет мама — анархия конец"));
    }

    @Test
    void maskWithUnderscore_isPreservedAndMatches() {
        IgnoreFilterService service = createService();
        when(ignoreRepo.findAllByChatId(1L)).thenReturn(List.of(
                Ignore.builder().word("_цчу_*").build()
        ));

        String result = service.filterMessage(1L, "_цчу_ОбработкаТочечныйРасчетПоказателей");
        assertEquals("", result);
    }
}


