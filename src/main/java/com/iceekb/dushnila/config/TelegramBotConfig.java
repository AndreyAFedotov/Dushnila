package com.iceekb.dushnila.config;

import com.iceekb.dushnila.properties.BotProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
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
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public OkHttpClient telegramOkHttpClient(BotProperties botProperties) {
        BotProperties.Main main = botProperties.getMain();
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(main.getConnectTimeout(), TimeUnit.SECONDS)
                .readTimeout(main.getReadTimeout(), TimeUnit.SECONDS)
                .writeTimeout(main.getWriteTimeout(), TimeUnit.SECONDS);

        BotProperties.ProxyProps proxyProps = botProperties.getProxy();
        if (proxyProps.getEnabled() != null && proxyProps.getEnabled()) {
            if (!StringUtils.isNotBlank(proxyProps.getHost()) || proxyProps.getPort() == null) {
                throw new IllegalArgumentException("Add proxy host and port");
            }

            Proxy proxy = new Proxy(
                    proxyProps.getType(),
                    new InetSocketAddress(proxyProps.getHost().trim(), proxyProps.getPort())
            );
            builder.proxy(proxy);

            if (StringUtils.isNotBlank(proxyProps.getUser()) && StringUtils.isNotBlank(proxyProps.getPassword())) {
                builder.proxyAuthenticator((route, response) -> {
                    String credential = Credentials.basic(proxyProps.getUser(), proxyProps.getPassword());
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
                                         BotProperties botProperties) {
        return new OkHttpTelegramClient(telegramOkHttpClient, botProperties.getMain().getToken());
    }

    @Bean
    public TelegramBotsLongPollingApplication telegramBotsApplication(OkHttpClient telegramOkHttpClient,
                                                                      ObjectMapper objectMapper) {
        return new TelegramBotsLongPollingApplication(() -> objectMapper, () -> telegramOkHttpClient);
    }
}
