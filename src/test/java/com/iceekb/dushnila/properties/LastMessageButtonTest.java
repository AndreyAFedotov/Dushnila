package com.iceekb.dushnila.properties;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LastMessageButtonTest {

    @Test
    void ctor_populatesFieldsFromCallbackQuery() {
        Update update = mock(Update.class);
        CallbackQuery query = mock(CallbackQuery.class);
        Message msg = mock(Message.class);
        User user = mock(User.class);

        when(update.getCallbackQuery()).thenReturn(query);
        when(query.getMessage()).thenReturn(msg);
        when(msg.getChatId()).thenReturn(100L);
        when(query.getFrom()).thenReturn(user);
        when(user.getId()).thenReturn(200L);
        when(query.getData()).thenReturn("channels");

        BaseBotProperties props = BaseBotProperties.builder().botAdmin("200").build();

        LastMessageButton lm = new LastMessageButton(update, props);
        assertNotNull(lm.getQuery());
        assertEquals(100L, lm.getChannelTgId());
        assertEquals(200L, lm.getUserTgId());
        assertEquals("channels", lm.getReceivedMessage());
        assertFalse(lm.isPersonal());
        assertFalse(lm.isPersonalFromAdmin()); // chatId != userId
        assertTrue(lm.isValid());
    }
}


