package com.iceekb.dushnila.service;

import com.iceekb.dushnila.jpa.repo.ChannelRepo;
import com.iceekb.dushnila.jpa.repo.IgnoreRepo;
import com.iceekb.dushnila.jpa.repo.PointRepo;
import com.iceekb.dushnila.jpa.repo.ReactionRepo;
import com.iceekb.dushnila.jpa.repo.UserRepo;
import com.iceekb.dushnila.message.enums.AdminCommand;
import com.iceekb.dushnila.properties.BaseBotProperties;
import com.iceekb.dushnila.properties.LastMessageTxt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessagesServiceAdminMenuTest {

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

    @SuppressWarnings("SameParameterValue")
    private static Update personalTextUpdate(long userId, String text) {
        Update update = mock(Update.class);
        Message msg = mock(Message.class);
        org.telegram.telegrambots.meta.api.objects.User from = mock(org.telegram.telegrambots.meta.api.objects.User.class);
        Chat chat = mock(Chat.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(msg);
        when(msg.hasText()).thenReturn(true);
        when(msg.getText()).thenReturn(text);
        // личка: chatId == userId
        when(msg.getChatId()).thenReturn(userId);
        when(msg.getMessageId()).thenReturn(1);
        when(msg.getFrom()).thenReturn(from);
        when(msg.getChat()).thenReturn(chat);
        when(chat.getTitle()).thenReturn(null);
        when(from.getId()).thenReturn(userId);
        when(from.getUserName()).thenReturn("admin");
        when(from.getFirstName()).thenReturn("Admin");
        when(from.getLastName()).thenReturn("");

        return update;
    }

    @Test
    void adminPersonalMenu_containsDeleteChannelButton() {
        MessagesService svc = createService();

        BaseBotProperties props = BaseBotProperties.builder()
                .botAdmin("999")
                .adminMail("admin@example.com")
                .startTime(LocalDateTime.now())
                .build();

        Update update = personalTextUpdate(999L, "/start");
        LastMessageTxt lm = svc.onUpdate(update, props);

        assertNotNull(lm.getMenu());
        assertTrue(
                lm.getMenu().getKeyboard().stream()
                        .flatMap(Collection::stream)
                        .anyMatch(btn -> AdminCommand.DELETE_CHANNEL.getLabel().equals(btn.getText()))
        );
    }
}


