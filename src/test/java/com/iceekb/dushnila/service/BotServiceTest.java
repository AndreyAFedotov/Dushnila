package com.iceekb.dushnila.service;

import com.iceekb.dushnila.properties.LastMessage;
import com.iceekb.dushnila.properties.LastMessageButton;
import com.iceekb.dushnila.properties.LastMessageTxt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BotServiceTest {

    @Test
    void ctor_throwsOnEmptyToken() {
        MessagesService messagesService = mock(MessagesService.class);
        AdminService adminService = mock(AdminService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<BuildProperties> bp = (ObjectProvider<BuildProperties>) mock(ObjectProvider.class);

        assertThrows(IllegalArgumentException.class, () -> new BotService(
                " ",
                "name",
                "123",
                "admin@example.com",
                1, 1, 1,
                messagesService,
                adminService,
                bp
        ));
    }

    @Test
    void ctor_throwsOnEmptyAdmin() {
        MessagesService messagesService = mock(MessagesService.class);
        AdminService adminService = mock(AdminService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<BuildProperties> bp = (ObjectProvider<BuildProperties>) mock(ObjectProvider.class);

        assertThrows(IllegalArgumentException.class, () -> new BotService(
                "123:ABC",
                "name",
                " ",
                "admin@example.com",
                1, 1, 1,
                messagesService,
                adminService,
                bp
        ));
    }

    @Test
    void getInstance_routesToAdminOnCallbackQuery() {
        MessagesService messagesService = mock(MessagesService.class);
        AdminService adminService = mock(AdminService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<BuildProperties> bp = (ObjectProvider<BuildProperties>) mock(ObjectProvider.class);

        BotService bot = new BotService(
                "123:ABC",
                "name",
                "999",
                "admin@example.com",
                1, 1, 1,
                messagesService,
                adminService,
                bp
        );

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
        @SuppressWarnings("unchecked")
        ObjectProvider<BuildProperties> bp = (ObjectProvider<BuildProperties>) mock(ObjectProvider.class);

        BotService bot = new BotService(
                "123:ABC",
                "name",
                "999",
                "admin@example.com",
                1, 1, 1,
                messagesService,
                adminService,
                bp
        );

        Update update = mock(Update.class);
        when(update.hasCallbackQuery()).thenReturn(false);

        LastMessageTxt lm = mock(LastMessageTxt.class);
        when(messagesService.onUpdate(any(Update.class), any())).thenReturn(lm);

        LastMessage result = bot.getInstance(update);
        assertSame(lm, result);
    }

    @Test
    void getBotToken_returnsToken() {
        MessagesService messagesService = mock(MessagesService.class);
        AdminService adminService = mock(AdminService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<BuildProperties> bp = (ObjectProvider<BuildProperties>) mock(ObjectProvider.class);

        BotService bot = new BotService(
                "123:ABC",
                "name",
                "999",
                "admin@example.com",
                1, 1, 1,
                messagesService,
                adminService,
                bp
        );

        assertEquals("123:ABC", bot.getBotToken());
        assertSame(bot, bot.getUpdatesConsumer());
    }
}


