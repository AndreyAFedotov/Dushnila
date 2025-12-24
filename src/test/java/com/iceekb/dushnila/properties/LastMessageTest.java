package com.iceekb.dushnila.properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LastMessageTest {

    static class LM extends LastMessage {
    }

    @Test
    void checkIsPersonal_trueWhenChatIdEqualsUserId() {
        LM lm = new LM();
        assertTrue(lm.checkIsPersonal(10L, 10L));
        assertFalse(lm.checkIsPersonal(10L, 11L));
    }

    @Test
    void checkIsPersonalFromAdmin_trueOnlyForMatchingAdminId() {
        LM lm = new LM();
        assertTrue(lm.checkIsPersonalFromAdmin(5L, 5L, "5"));
        assertFalse(lm.checkIsPersonalFromAdmin(5L, 5L, "6"));
        assertFalse(lm.checkIsPersonalFromAdmin(5L, 6L, "5"));
    }
}


