package com.iceekb.dushnila;

import com.iceekb.dushnila.jpa.entity.Channel;
import com.iceekb.dushnila.jpa.entity.User;
import com.iceekb.dushnila.message.enums.ChannelApproved;
import com.iceekb.dushnila.properties.LastMessageTxt;

import java.time.LocalDateTime;
import java.util.ArrayList;

public abstract class TestUtils {

    private static final LocalDateTime AT_DATE_TIME = LocalDateTime.of(2020, 1, 1, 0, 0);
    public static final String FORMAT_ERROR = "Ошибочный формат команды";

    protected static LastMessageTxt createMessage() {
        return LastMessageTxt.builder()
                .channelName("Test Chat Name on Message")
                .channelTgId(2L)
                .userTgId(4L)
                .userName("Test User NickName on Message")
                .channel(createChanel())
                .user(createUser())
                .receivedMessage("test message")
                .isPersonal(false)
                .isPersonalFromAdmin(false)
                .isValid(true)
                .isError(false)
                .validationErrors(new ArrayList<>())
                .build();
    }

    protected static Channel createChanel() {
        return Channel.builder()
                .id(1L)
                .tgId(2L)
                .chatName("Test Chat Name on Channel")
                .firstMessage(AT_DATE_TIME)
                .lastMessage(AT_DATE_TIME)
                .messageCount(0L)
                .approved(ChannelApproved.WAITING)
                .build();
    }

    protected static User createUser() {
        return User.builder()
                .id(3L)
                .tgId(4L)
                .nickName("Test User NickName on User")
                .firstMessage(AT_DATE_TIME)
                .lastMessage(AT_DATE_TIME)
                .build();
    }
}
