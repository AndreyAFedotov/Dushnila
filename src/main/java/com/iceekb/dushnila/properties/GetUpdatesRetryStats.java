package com.iceekb.dushnila.properties;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class GetUpdatesRetryStats {

    private static final Set<LocalDateTime> timestamps = Collections.synchronizedSet(new HashSet<>());

    private GetUpdatesRetryStats() {
    }

    public static void add(LocalDateTime time) {
        timestamps.add(time);
    }

    @SuppressWarnings("unused")
    public static void clear() {
        timestamps.clear();
    }

    public static int getCount() {
        synchronized (timestamps) {
            return timestamps.size();
        }
    }

    public static LocalDateTime getFirstTime() {
        synchronized (timestamps) {
            return timestamps.isEmpty() ? null : Collections.min(timestamps);
        }
    }
}
