package com.iceekb.dushnila.properties;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelegramLogFilterTest {

    @Test
    void decide_deniesTelegramLogs_andAcceptsOthers() {
        TelegramLogFilter filter = new TelegramLogFilter();

        ILoggingEvent tg = mock(ILoggingEvent.class);
        when(tg.getLoggerName()).thenReturn("org.telegram.whatever");
        when(tg.getLevel()).thenReturn(Level.INFO);
        when(tg.getFormattedMessage()).thenReturn("hello");
        assertEquals(FilterReply.DENY, filter.decide(tg));

        ILoggingEvent other = mock(ILoggingEvent.class);
        when(other.getLoggerName()).thenReturn("com.iceekb.app");
        when(other.getLevel()).thenReturn(Level.INFO);
        when(other.getFormattedMessage()).thenReturn("hello");
        assertEquals(FilterReply.ACCEPT, filter.decide(other));
    }
}


