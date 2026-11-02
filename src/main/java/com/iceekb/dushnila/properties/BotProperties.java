package com.iceekb.dushnila.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.Proxy;

/**
 * Типизированная конфигурация бота (префикс "bot" в application.yml).
 * Заменяет россыпь @Value в конструкторах.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "bot")
public class BotProperties {

    private Main main = new Main();
    private ProxyProps proxy = new ProxyProps();

    @Getter
    @Setter
    public static class Main {
        private String name;
        private String token;
        private String admin;
        private String adminMail;
        private Integer connectTimeout = 30;
        private Integer readTimeout = 90;
        private Integer writeTimeout = 30;
    }

    @Getter
    @Setter
    public static class ProxyProps {
        private Boolean enabled = false;
        private Proxy.Type type = Proxy.Type.HTTP;
        private String host;
        private Integer port;
        private String user;
        private String password;
    }
}
