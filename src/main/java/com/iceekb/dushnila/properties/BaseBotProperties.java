package com.iceekb.dushnila.properties;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
}
