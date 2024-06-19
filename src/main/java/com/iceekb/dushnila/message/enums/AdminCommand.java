package com.iceekb.dushnila.message.enums;

public enum AdminCommand {
    HELP("/help - помощь"),
    APPROVE("/approve channelName - одобрить канал"),
    DAPPROVE("/dapprove channelName - удалить одобрение"),
    CHANNELS("/channels - список каналов"),
    UNKNOWN("Команда не опознана"),
    IMPORT("Импорт из старой базы"),
    UPTIME("Время работы");

    private final String label;

    AdminCommand(String label) {
        this.label = label;
    }
}
