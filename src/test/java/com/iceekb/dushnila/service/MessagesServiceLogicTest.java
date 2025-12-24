package com.iceekb.dushnila.service;

import com.iceekb.dushnila.TestUtils;
import com.iceekb.dushnila.jpa.repo.ChannelRepo;
import com.iceekb.dushnila.jpa.repo.IgnoreRepo;
import com.iceekb.dushnila.jpa.repo.PointRepo;
import com.iceekb.dushnila.jpa.repo.ReactionRepo;
import com.iceekb.dushnila.jpa.repo.UserRepo;
import com.iceekb.dushnila.message.enums.ResponseTypes;
import com.iceekb.dushnila.properties.BaseBotProperties;
import com.iceekb.dushnila.properties.LastMessageTxt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @Mock private IgnoreFilterService ignoreFilterService;
    @Mock private ReactionService reactionService;
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
                ignoreFilterService,
                reactionService,
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


