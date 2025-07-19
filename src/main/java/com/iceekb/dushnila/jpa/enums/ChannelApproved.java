package com.iceekb.dushnila.jpa.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum ChannelApproved {
    APPROVED("App", 'A'),
    REJECTED("Rej", 'R'),
    WAITING("Wait", 'W');

    @Getter
    private static final Map<Character, ChannelApproved> idMap = Arrays.stream(ChannelApproved.values())
            .collect(Collectors.toMap(it -> it.id, Function.identity()));
    private final String desc;
    private final Character id;

    ChannelApproved(String desc, Character id) {
        this.desc = desc;
        this.id = id;
    }

    public static ChannelApproved of(Character id) {
        return Optional.ofNullable(idMap.get(id))
                .orElseThrow(() -> new RuntimeException("No present by: " + id));
    }

    public String getName() {
        return this.name();
    }
}
