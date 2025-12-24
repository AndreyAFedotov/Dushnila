package com.iceekb.dushnila;

import com.iceekb.dushnila.message.AdminService;
import com.iceekb.dushnila.message.MessagesService;
import com.iceekb.dushnila.properties.BaseBotProperties;
import com.iceekb.dushnila.properties.LastMessage;
import com.iceekb.dushnila.properties.LastMessageButton;
import com.iceekb.dushnila.properties.LastMessageTxt;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class Bot implements SpringLongPollingBot, LongPollingUpdateConsumer {

    private static final String START_MESSAGE = "The bot has been successfully launched!";
    private final TelegramClient telegramClient;
    private final MessagesService messagesService;
    private final AdminService adminService;
    private final String botToken;
    private final BaseBotProperties properties;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    public Bot(@Value("${bot.token}") String token,
               @Value("${bot.name}") String name,
               @Value("${bot.admin}") String admin,
               @Value("${bot.adminMail}") String adminMail,
               @Value("${bot.connectTimeout}") Integer connectTimeout,
               @Value("${bot.readTimeout}") Integer readTimeout,
               @Value("${bot.writeTimeout}") Integer writeTimeout,
               MessagesService messagesService,
               AdminService adminService,
               ObjectProvider<BuildProperties> buildPropertiesProvider) {
        this.messagesService = messagesService;
        this.adminService = adminService;
        this.botToken = token;
        this.buildPropertiesProvider = buildPropertiesProvider;

        // Валидация параметров
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Bot token cannot be null or empty");
        }
        if (admin == null || admin.trim().isEmpty()) {
            throw new IllegalArgumentException("Bot admin ID cannot be null or empty");
        }

        var client = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .build();

        this.telegramClient = new OkHttpTelegramClient(client, token);
        this.properties = BaseBotProperties.builder()
                .botToken(token)
                .botName(name)
                .botAdmin(admin)
                .adminMail(adminMail)
                .startTime(LocalDateTime.now())
                .build();
    }

    private String startupMessageForAdmin() {
        BuildProperties bp = buildPropertiesProvider.getIfAvailable();
        if (bp == null) {
            return START_MESSAGE;
        }
        return START_MESSAGE + "\nVersion: " + bp.getVersion();
    }

    private void sendMessageTxt(LastMessageTxt lastMessage) {
        SendChatAction sendChatAction = new SendChatAction(
                lastMessage.getChannelTgId().toString(),
                ActionType.TYPING.toString());
        try {
            telegramClient.executeAsync(sendChatAction);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }

        if (lastMessage.getMenu() == null) {
            try {
                telegramClient.executeAsync(SendMessage.builder()
                        .chatId(lastMessage.getChannelTgId())
                        .replyToMessageId(lastMessage.getMessageId())
                        .text(lastMessage.getResponse())
                        .build());
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        } else {
            try {
                telegramClient.executeAsync(SendMessage.builder()
                        .chatId(lastMessage.getChannelTgId())
                        .replyToMessageId(lastMessage.getMessageId())
                        .text(lastMessage.getResponse())
                        .replyMarkup(lastMessage.getMenu())
                        .build());
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        }
    }

    private void sendMessageButton(LastMessageButton lastMessage) {
        try {
            if (lastMessage.getMenu() != null) {
                telegramClient.executeAsync(SendMessage.builder()
                        .chatId(lastMessage.getChannelTgId())
                        .text(lastMessage.getResponse())
                        .replyMarkup(lastMessage.getMenu())
                        .build());
            } else {
                telegramClient.executeAsync(SendMessage.builder()
                        .chatId(lastMessage.getChannelTgId())
                        .text(lastMessage.getResponse())
                        .build());
            }

            // Отвечаем на callbackQuery, чтобы убрать анимацию загрузки
            if (lastMessage.getQuery() != null) {
                telegramClient.executeAsync(AnswerCallbackQuery.builder()
                        .callbackQueryId(lastMessage.getQuery().getId())
                        .build());
            }
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @AfterBotRegistration
    @SuppressWarnings("unused")
    public void afterRegistration(BotSession botSession) {
        log.info("************* Bot registration started *************");
        log.info("Bot session running state: {}", botSession.isRunning());
        log.info("Bot token: {}", botToken != null ? "SET" : "NOT SET");
        log.info("Bot admin ID: {}", properties.getBotAdmin());

        try {
            if (botSession.isRunning()) {
                log.info("Sending startup message to admin...");
                telegramClient.executeAsync(SendMessage.builder()
                        .chatId(properties.getBotAdmin())
                        .text(startupMessageForAdmin())
                        .build());
                log.info("Startup message sent successfully");
            } else {
                log.warn("Bot session is not running, skipping startup message");
            }
        } catch (TelegramApiException e) {
            log.error("Telegram API error during startup: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during startup: {}", e.getMessage(), e);
        }

        log.info("************* Bot registration completed *************");
    }

    public LastMessage getInstance(Update update) {
        if (update.hasCallbackQuery()) {
            return adminService.onUpdate(update, properties);
        } else {
            return messagesService.onUpdate(update, properties);
        }
    }

    @Override
    public void consume(List<Update> list) {
        list.forEach(update -> {
            LastMessage lm = getInstance(update);

            if (lm instanceof LastMessageTxt lastMessage) {
                if (StringUtils.isNotBlank(lastMessage.getResponse())) {
                    sendMessageTxt(lastMessage);
                } else if (!lastMessage.isValid()) {
                    lastMessage.getValidationErrors().forEach(
                            t -> log.warn("Message validation error: <{}> for channel <{}>",
                                    t.getLabel(),
                                    lastMessage.getChannelName()));
                }
            } else if (lm instanceof LastMessageButton lastMessageB) {
                if (lastMessageB.getMenu() != null || lastMessageB.getResponse() != null) {
                    sendMessageButton(lastMessageB);
                } else if (!lastMessageB.isValid()) {
                    lastMessageB.getValidationErrors().forEach(
                            t -> log.warn("Button message validation error: <{}>",
                                    t.getLabel()));
                }
            }
        });
    }
}
