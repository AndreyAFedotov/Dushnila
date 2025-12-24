package com.iceekb.dushnila.message.enums;

import lombok.Getter;

@Getter
public enum AdminCommand {
    APPROVE("Одобрить канал"),
    APPROVE_ADD("Канал одобрен"),
    DAPPROVE("Удалить одобрение"),
    DAPPROVE_ADD("Одобрение удалено"),
    DELETE_CHANNEL("Удалить канал"),
    DELETE_CHANNEL_ADD("Канал удалён"),
    CHANNELS("Список каналов"),
    UNKNOWN("Команда не опознана"),
    UPTIME("Время работы");

    private final String label;

    AdminCommand(String label) {
        this.label = label;
    }
}
