package com.iceekb.dushnila.message.enums;

import lombok.Getter;

@Getter
public enum MessageValidationError {
    TYPE("Not a text message"),
    USER_ID("Telegram user ID is lost"),
    CHAT_ID("Telegram channel ID is lost"),
    CHAT_NAME("Telegram channel name is lost"),
    EMPTY_MESSAGE("Message is empty"),
    CHANNEL_NOT_APPROVED("Channel is not approved");

    private final String label;

    MessageValidationError(String label) {
        this.label = label;
    }
}
