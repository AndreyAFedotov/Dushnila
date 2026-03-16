package com.iceekb.dushnila.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

@Configuration
public class TelegramBotConfig {

    @Bean
    public OkHttpClient telegramOkHttpClient(@Value("${bot.main.connectTimeout}") Integer connectTimeout,
                                             @Value("${bot.main.readTimeout}") Integer readTimeout,
                                             @Value("${bot.main.writeTimeout}") Integer writeTimeout,
                                             @Value("${bot.proxy.enabled}") Boolean proxyEnabled,
                                             @Value("${bot.proxy.type}") Proxy.Type proxyType,
                                             @Value("${bot.proxy.host}") String proxyHost,
                                             @Value("${bot.proxy.port}") Integer proxyPort,
                                             @Value("${bot.proxy.user}") String proxyUser,
                                             @Value("${bot.proxy.password}") String proxyPassword) {

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS);

        if (proxyEnabled != null && proxyEnabled) {
            if (!StringUtils.isNotBlank(proxyHost) || proxyPort == null) {
                throw new IllegalArgumentException("Add proxy host and port");
            }

            Proxy proxy = new Proxy(
                    proxyType,
                    new InetSocketAddress(proxyHost.trim(), proxyPort)
            );
            builder.proxy(proxy);

            if (StringUtils.isNotBlank(proxyUser) && StringUtils.isNotBlank(proxyPassword)) {
                builder.proxyAuthenticator((route, response) -> {
                    String credential = Credentials.basic(proxyUser, proxyPassword);
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                });
            }
        }

        return builder.build();
    }

    @Bean
    public TelegramClient telegramClient(OkHttpClient telegramOkHttpClient,
                                         @Value("${bot.main.token}") String token) {
        return new OkHttpTelegramClient(telegramOkHttpClient, token);
    }

    @Bean
    public TelegramBotsLongPollingApplication telegramBotsApplication(OkHttpClient telegramOkHttpClient) {
        return new TelegramBotsLongPollingApplication(ObjectMapper::new, () -> telegramOkHttpClient);
    }
}

