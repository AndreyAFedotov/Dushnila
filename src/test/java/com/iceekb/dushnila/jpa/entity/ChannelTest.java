package com.iceekb.dushnila.jpa.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ChannelTest {

    @Test
    void equalsAndHashCode_useIdAndTgId() {
        Channel a = Channel.builder().id(1L).tgId(2L).chatName("A").build();
        Channel b = Channel.builder().id(1L).tgId(2L).chatName("B").build();
        Channel c = Channel.builder().id(1L).tgId(3L).chatName("A").build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}


