package com.iceekb.dushnila.service;

import com.iceekb.dushnila.properties.BotProperties;
import com.iceekb.dushnila.properties.LastMessage;
import com.iceekb.dushnila.properties.LastMessageButton;
import com.iceekb.dushnila.properties.LastMessageTxt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BotServiceTest {

    private static BotProperties props(String token, String admin) {
        BotProperties props = new BotProperties();
        props.getMain().setToken(token);
        props.getMain().setName("name");
        props.getMain().setAdmin(admin);
        props.getMain().setAdminMail("admin@example.com");
        return props;
    }

    @SuppressWarnings("unchecked")
    private static BotService createBot(BotProperties props, MessagesService messagesService, AdminService adminService) {
        return new BotService(
                mock(TelegramClient.class),
                props,
                messagesService,
                adminService,
                (ObjectProvider<BuildProperties>) mock(ObjectProvider.class)
        );
    }

    @Test
    void ctor_throwsOnEmptyToken() {
        assertThrows(IllegalArgumentException.class, () -> createBot(
                props(" ", "123"),
                mock(MessagesService.class),
                mock(AdminService.class)
        ));
    }

    @Test
    void ctor_throwsOnEmptyAdmin() {
        assertThrows(IllegalArgumentException.class, () -> createBot(
                props("123:ABC", " "),
                mock(MessagesService.class),
                mock(AdminService.class)
        ));
    }

    @Test
    void getInstance_routesToAdminOnCallbackQuery() {
        MessagesService messagesService = mock(MessagesService.class);
        AdminService adminService = mock(AdminService.class);
        BotService bot = createBot(props("123:ABC", "999"), messagesService, adminService);

        Update update = mock(Update.class);
        when(update.hasCallbackQuery()).thenReturn(true);

        LastMessageButton lm = mock(LastMessageButton.class);
        when(adminService.onUpdate(any(Update.class), any())).thenReturn(lm);

        LastMessage result = bot.getInstance(update);
        assertSame(lm, result);
    }

    @Test
    void getInstance_routesToMessagesOnNonCallback() {
        MessagesService messagesService = mock(MessagesService.class);
        AdminService adminService = mock(AdminService.class);
        BotService bot = createBot(props("123:ABC", "999"), messagesService, adminService);

        Update update = mock(Update.class);
        when(update.hasCallbackQuery()).thenReturn(false);

        LastMessageTxt lm = mock(LastMessageTxt.class);
        when(messagesService.onUpdate(any(Update.class), any())).thenReturn(lm);

        LastMessage result = bot.getInstance(update);
        assertSame(lm, result);
    }

    @Test
    void getBotToken_returnsToken() {
        BotService bot = createBot(
                props("123:ABC", "999"),
                mock(MessagesService.class),
                mock(AdminService.class)
        );

        assertEquals("123:ABC", bot.getBotToken());
        assertSame(bot, bot.getUpdatesConsumer());
    }
}
