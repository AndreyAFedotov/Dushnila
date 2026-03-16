package com.iceekb.dushnila.properties;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Хранилище времён событий getUpdates retry (таймауты/ошибки).
 * Используется TelegramLogFilter и доступно админ-команде /timeout.
 */
public final class GetUpdatesRetryStats {

    private static final Set<LocalDateTime> timestamps = Collections.synchronizedSet(new HashSet<>());

    private GetUpdatesRetryStats() {
    }

    public static void add(LocalDateTime time) {
        timestamps.add(time);
    }

    public static void clear() {
        timestamps.clear();
    }

    /** Текущее количество накопленных событий. */
    public static int getCount() {
        synchronized (timestamps) {
            return timestamps.size();
        }
    }

    /** Время первого события в текущем окне, или null если пусто. */
    public static LocalDateTime getFirstTime() {
        synchronized (timestamps) {
            return timestamps.isEmpty() ? null : Collections.min(timestamps);
        }
    }
}
