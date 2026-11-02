package com.iceekb.dushnila.properties;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Счётчики ошибок getUpdates.
 * Хранит только количество и время первой ошибки — без накопления timestamps в памяти.
 */
public final class GetUpdatesRetryStats {

    private static final AtomicLong count = new AtomicLong();
    private static final AtomicReference<LocalDateTime> firstTime = new AtomicReference<>();

    private GetUpdatesRetryStats() {
    }

    public static void add(LocalDateTime time) {
        count.incrementAndGet();
        firstTime.updateAndGet(current -> (current == null || time.isBefore(current)) ? time : current);
    }

    @SuppressWarnings("unused")
    public static void clear() {
        count.set(0);
        firstTime.set(null);
    }

    public static long getCount() {
        return count.get();
    }

    public static LocalDateTime getFirstTime() {
        return firstTime.get();
    }
}
