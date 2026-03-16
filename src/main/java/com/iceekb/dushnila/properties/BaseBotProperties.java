package com.iceekb.dushnila.properties;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.net.Proxy;
import java.time.LocalDateTime;

@Getter
@Builder
@Setter
public class BaseBotProperties {
    private String botName;
    private String botToken;
    private String botAdmin;
    private String adminMail;
    private LocalDateTime startTime;
    private Boolean proxyEnabled;
    private Proxy.Type proxyType;
    private String proxyHost;
    private Integer proxyPort;
}
