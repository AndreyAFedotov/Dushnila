package com.iceekb.dushnila.service;

import com.iceekb.dushnila.message.enums.ResponseTypes;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoResponseServiceTest {

    @Test
    void init_populatesBaseCaches() {
        AutoResponseService svc = new AutoResponseService();
        svc.init();

        String m = svc.getMessage(ResponseTypes.PUBLIC, 1L);
        assertNotNull(m);
    }

    @Test
    void getMessage_returnsAllUniqueUntilExhausted_perChannelAndType() {
        AutoResponseService svc = new AutoResponseService();
        svc.init();

        int n = AutoResponseService.PUBLIC_DATA.size();
        Set<String> got = new HashSet<>();
        for (int i = 0; i < n; i++) {
            got.add(svc.getMessage(ResponseTypes.PUBLIC, 123L));
        }
        // Внутри одного "цикла" на канале ответы не должны повторяться (они удаляются из workingCopy)
        assertTrue(got.size() == n);

        // После исчерпания список восстанавливается — следующий вызов должен отдать что-то не-null
        assertNotNull(svc.getMessage(ResponseTypes.PUBLIC, 123L));
    }

    @Test
    void channelCache_isBoundedAndDoesNotGrowUnbounded() {
        AutoResponseService svc = new AutoResponseService();
        svc.init();

        // Прогоняем много каналов, чтобы сработала очистка.
        for (long ch = 1; ch <= 1100; ch++) {
            svc.getMessage(ResponseTypes.PERSONAL, ch);
        }
        // MAX_CHANNELS_IN_MEMORY = 1000, поэтому размер должен удерживаться <= 1000
        assertTrue(svc.getChannelResponses().size() <= 1000);
    }
}


