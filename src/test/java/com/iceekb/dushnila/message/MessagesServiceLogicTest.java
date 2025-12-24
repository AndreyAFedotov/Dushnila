package com.iceekb.dushnila.message;

import com.iceekb.dushnila.TestUtils;
import com.iceekb.dushnila.jpa.entity.Ignore;
import com.iceekb.dushnila.jpa.entity.Reaction;
import com.iceekb.dushnila.jpa.repo.ChannelRepo;
import com.iceekb.dushnila.jpa.repo.IgnoreRepo;
import com.iceekb.dushnila.jpa.repo.PointRepo;
import com.iceekb.dushnila.jpa.repo.ReactionRepo;
import com.iceekb.dushnila.jpa.repo.UserRepo;
import com.iceekb.dushnila.message.enums.ResponseTypes;
import com.iceekb.dushnila.message.responses.AutoResponseService;
import com.iceekb.dushnila.properties.BaseBotProperties;
import com.iceekb.dushnila.properties.LastMessageTxt;
import com.iceekb.dushnila.speller.SpellerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessagesServiceLogicTest extends TestUtils {

    @Mock private ChannelRepo channelRepo;
    @Mock private UserRepo userRepo;
    @Mock private PointRepo pointRepo;
    @Mock private ReactionRepo reactionRepo;
    @Mock private IgnoreRepo ignoreRepo;
    @Mock private SpellerService spellerService;
    @Mock private AutoResponseService responseService;
    @Mock private PlatformTransactionManager transactionManager;

    private MessagesService createService() {
        return new MessagesService(
                channelRepo,
                userRepo,
                pointRepo,
                reactionRepo,
                ignoreRepo,
                spellerService,
                responseService,
                transactionManager
        );
    }

    private static void invokePrivate(Object target, String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = target.getClass().getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        m.invoke(target, args);
    }

    @Test
    void onPersonalMessage_channelIsNull_doesNotThrowAndSetsResponse() {
        MessagesService service = createService();
        when(responseService.getMessage(eq(ResponseTypes.PERSONAL), anyLong())).thenReturn("Личка только для админа! :)");

        BaseBotProperties props = BaseBotProperties.builder()
                .adminMail("admin@example.com")
                .botAdmin("999")
                .startTime(LocalDateTime.now())
                .build();

        LastMessageTxt msg = createMessage();
        msg.setChannel(null); // имитируем личку, где channel может быть не установлен
        msg.setUserTgId(123L);
        msg.setResponse(null);

        assertDoesNotThrow(() -> {
            try {
                invokePrivate(service, "onPersonalMessage", new Class[]{LastMessageTxt.class, BaseBotProperties.class}, msg, props);
            } catch (Exception e) {
                // пробрасываем как runtime, чтобы assertDoesNotThrow поймал
                throw new RuntimeException(e);
            }
        });

        assertEquals("Личка только для админа! :) Для связи: admin@example.com", msg.getResponse());
    }

    @Test
    void deleteIgnoreAndUsers_removesIgnoredWordsMentionsAndPunctuationExceptAt() throws Exception {
        MessagesService service = createService();

        LastMessageTxt msg = createMessage();
        msg.getChannel().setId(1L);
        msg.setReceivedMessage("Привет, @bob! мир... hello!!! мама — анархия @ann");

        Ignore ig1 = Ignore.builder().word("мир").build();
        Ignore ig2 = Ignore.builder().word("hello").build();
        when(ignoreRepo.findAllByChatId(1L)).thenReturn(List.of(ig1, ig2));

        invokePrivate(service, "deleteIgnoreAndUsers", new Class[]{LastMessageTxt.class}, msg);

        // "—" и прочая Unicode-пунктуация должна быть вычищена, @mentions удалены, игноры убраны.
        // Здесь мы не игнорируем "мама"/"анархия", поэтому они остаются как слова.
        assertEquals("Привет мама - анархия", msg.getReceivedMessage());
    }

    @Test
    void checkReaction_phraseOverridesWord() throws Exception {
        MessagesService service = createService();

        LastMessageTxt msg = createMessage();
        msg.getChannel().setId(1L);
        msg.setReceivedMessage("Это ОЧЕНЬ плохо!!");
        msg.setResponse(null);

        Reaction phrase = Reaction.builder().textFrom("Очень плохо").textTo("PHRASE").build();
        Reaction word = Reaction.builder().textFrom("плохо").textTo("WORD").build();
        when(reactionRepo.findAllByChannelId(1L)).thenReturn(List.of(phrase, word));

        invokePrivate(service, "checkReaction", new Class[]{LastMessageTxt.class}, msg);

        assertEquals("PHRASE", msg.getResponse());
    }

    @Test
    void checkReaction_phraseWithHyphenOrDashMatches() throws Exception {
        MessagesService service = createService();

        Reaction phrasePlain = Reaction.builder().textFrom("мама анархия").textTo("PLAIN").build();
        Reaction phraseDash = Reaction.builder().textFrom("мама - анархия").textTo("DASH").build();
        when(reactionRepo.findAllByChannelId(1L)).thenReturn(List.of(phrasePlain, phraseDash));

        LastMessageTxt msg1 = createMessage();
        msg1.getChannel().setId(1L);
        msg1.setReceivedMessage("Мама-анархия");
        msg1.setResponse(null);
        invokePrivate(service, "checkReaction", new Class[]{LastMessageTxt.class}, msg1);
        // При совпадении только по нормализованной форме выбираем более "специфичную" (с дефисом),
        // чтобы не терять возможность отличать реакции при наличии нескольких вариантов в БД.
        assertEquals("DASH", msg1.getResponse());

        LastMessageTxt msg2 = createMessage();
        msg2.getChannel().setId(1L);
        msg2.setReceivedMessage("мама — анархия"); // em-dash
        msg2.setResponse(null);
        invokePrivate(service, "checkReaction", new Class[]{LastMessageTxt.class}, msg2);
        assertEquals("DASH", msg2.getResponse());

        LastMessageTxt msg3 = createMessage();
        msg3.getChannel().setId(1L);
        msg3.setReceivedMessage("мама анархия");
        msg3.setResponse(null);
        invokePrivate(service, "checkReaction", new Class[]{LastMessageTxt.class}, msg3);
        // Точное совпадение по исходному тексту должно побеждать.
        assertEquals("PLAIN", msg3.getResponse());
    }

    @Test
    void checkReaction_wordBoundaryDoesNotMatchSubword() throws Exception {
        MessagesService service = createService();

        // "котик" не должен вызывать реакцию на "кот"
        LastMessageTxt msg1 = createMessage();
        msg1.getChannel().setId(1L);
        msg1.setReceivedMessage("котик");
        msg1.setResponse(null);

        Reaction cat = Reaction.builder().textFrom("кот").textTo("CAT").build();
        when(reactionRepo.findAllByChannelId(1L)).thenReturn(List.of(cat));

        invokePrivate(service, "checkReaction", new Class[]{LastMessageTxt.class}, msg1);
        assertNull(msg1.getResponse());

        // а в "котик кот" должно сработать
        LastMessageTxt msg2 = createMessage();
        msg2.getChannel().setId(1L);
        msg2.setReceivedMessage("котик кот");
        msg2.setResponse(null);

        invokePrivate(service, "checkReaction", new Class[]{LastMessageTxt.class}, msg2);
        assertEquals("CAT", msg2.getResponse());
    }

    @Test
    void speller_whenResponsePresent_incrementsPointAtomically() throws Exception {
        MessagesService service = createService();

        LastMessageTxt msg = createMessage();
        msg.getChannel().setId(10L);
        msg.getUser().setId(20L);
        msg.setResponse(null);

        doAnswer(invocation -> {
            LastMessageTxt arg = invocation.getArgument(0);
            arg.setResponse("Есть ошибки!");
            return arg;
        }).when(spellerService).speller(any(LastMessageTxt.class));

        invokePrivate(service, "speller", new Class[]{LastMessageTxt.class}, msg);

        verify(pointRepo).incrementPoint(10L, 20L);
    }
}


