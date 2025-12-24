package com.iceekb.dushnila.properties;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import lombok.extern.slf4j.Slf4j;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.TRACE;
import static ch.qos.logback.classic.Level.WARN;

@Slf4j
public class TelegramLogFilter extends Filter<ILoggingEvent> {
    private static final String TG_EVENT = "*** Telegram Event - ";

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event.getLoggerName().contains("org.telegram")) {
            Level level = event.getLevel();
            String textEvent = TG_EVENT + event.getFormattedMessage();
            if (level.equals(ERROR)) {
                log.error(textEvent);
            } else if (level.equals(WARN)) {
                log.warn(textEvent);
            } else if (level.equals(DEBUG)) {
                log.debug(textEvent);
            } else if (level.equals(TRACE)) {
                log.trace(textEvent);
            } else {
                log.info(textEvent);
            }
            return FilterReply.DENY;
        }
        return FilterReply.ACCEPT;
    }
}
