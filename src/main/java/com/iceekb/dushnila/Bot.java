package com.iceekb.dushnila;

import com.iceekb.dushnila.message.MessagesService;
import com.iceekb.dushnila.properties.BaseBotProperties;
import com.iceekb.dushnila.properties.LastMessage;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class Bot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final String START_MESSAGE = "The bot has been successfully launched!";
    private final TelegramClient telegramClient;
    private final MessagesService messagesService;
    private final String botToken;
    private final BaseBotProperties properties;

    public Bot(@Value("${bot.token}") String token,
               @Value("${bot.name}") String name,
               @Value("${bot.admin}") String admin,
               @Value("${bot.adminMail}") String adminMail,
               @Value("${bot.connectTimeout}") Integer connectTimeout,
               @Value("${bot.readTimeout}") Integer readTimeout,
               @Value("${bot.writeTimeout}") Integer writeTimeout,
               MessagesService messagesService) {
        this.messagesService = messagesService;
        this.botToken = token;

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

    private void sendMessage(LastMessage lastMessage) {
        SendChatAction sendChatAction = new SendChatAction(
                lastMessage.getChannelTgId().toString(),
                ActionType.TYPING.toString());
        try {
            telegramClient.execute(sendChatAction);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }

        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(lastMessage.getChannelTgId())
                    .replyToMessageId(lastMessage.getMessageId())
                    .text(lastMessage.getResponse())
                    .build());
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void consume(Update update) {
        LastMessage lastMessage = messagesService.onUpdate(update, properties);
        if (StringUtils.isNotBlank(lastMessage.getResponse())) {
            sendMessage(lastMessage);
        } else if (!lastMessage.isValid()) {
            lastMessage.getValidationErrors().forEach(
                    t -> log.warn("Message validation error: <{}> for channel <{}>",
                            t.getLabel(),
                            lastMessage.getChannelName()));
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
        log.info("************* Registered bot running state is: {}", botSession.isRunning());
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(properties.getBotAdmin())
                    .text(START_MESSAGE)
                    .build());
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
}
