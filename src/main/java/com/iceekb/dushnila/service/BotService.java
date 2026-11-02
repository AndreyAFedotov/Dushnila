package com.iceekb.dushnila.service;

import com.iceekb.dushnila.properties.BaseBotProperties;
import com.iceekb.dushnila.properties.BotProperties;
import com.iceekb.dushnila.properties.LastMessage;
import com.iceekb.dushnila.properties.LastMessageButton;
import com.iceekb.dushnila.properties.LastMessageTxt;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class BotService implements SpringLongPollingBot, LongPollingUpdateConsumer {

    private static final String START_MESSAGE = "The bot has been successfully launched!";

    /**
     * Количество рабочих потоков обработки апдейтов.
     * Апдейты одного чата всегда попадают в один поток (полоса по chatId),
     * поэтому порядок сообщений внутри чата сохраняется, а медленный спеллер
     * в одном чате не тормозит остальные.
     */
    private static final int WORKERS = 8;

    private final TelegramClient telegramClient;
    private final MessagesService messagesService;
    private final AdminService adminService;
    private final String botToken;
    private final BaseBotProperties properties;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;
    private final List<ExecutorService> workers;

    public BotService(TelegramClient telegramClient,
                      BotProperties botProperties,
                      MessagesService messagesService,
                      AdminService adminService,
                      ObjectProvider<BuildProperties> buildPropertiesProvider) {
        this.telegramClient = telegramClient;
        this.messagesService = messagesService;
        this.adminService = adminService;
        this.botToken = botProperties.getMain().getToken();
        this.buildPropertiesProvider = buildPropertiesProvider;

        // Валидация параметров
        if (botToken == null || botToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Bot token cannot be null or empty");
        }
        String admin = botProperties.getMain().getAdmin();
        if (admin == null || admin.trim().isEmpty()) {
            throw new IllegalArgumentException("Bot admin ID cannot be null or empty");
        }

        BotProperties.ProxyProps proxy = botProperties.getProxy();
        this.properties = BaseBotProperties.builder()
                .botToken(botToken)
                .botName(botProperties.getMain().getName())
                .botAdmin(admin)
                .adminMail(botProperties.getMain().getAdminMail())
                .proxyEnabled(proxy.getEnabled())
                .proxyType(proxy.getType())
                .proxyHost(proxy.getHost())
                .proxyPort(proxy.getPort())
                .proxyUser(proxy.getUser())
                .proxyPassword(proxy.getPassword())
                .startTime(LocalDateTime.now())
                .build();

        this.workers = IntStream.range(0, WORKERS)
                .mapToObj(i -> Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "dushnila-update-" + i);
                    thread.setDaemon(true);
                    return thread;
                }))
                .toList();
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

        SendMessage.SendMessageBuilder<?, ?> messageBuilder = SendMessage.builder()
                .chatId(lastMessage.getChannelTgId())
                .replyToMessageId(lastMessage.getMessageId())
                .text(lastMessage.getResponse());
        if (lastMessage.getMenu() != null) {
            messageBuilder.replyMarkup(lastMessage.getMenu());
        }
        try {
            telegramClient.executeAsync(messageBuilder.build());
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private void sendMessageButton(LastMessageButton lastMessage) {
        try {
            SendMessage.SendMessageBuilder<?, ?> messageBuilder = SendMessage.builder()
                    .chatId(lastMessage.getChannelTgId())
                    .text(lastMessage.getResponse());
            if (lastMessage.getMenu() != null) {
                messageBuilder.replyMarkup(lastMessage.getMenu());
            }
            telegramClient.executeAsync(messageBuilder.build());

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

        if (properties.getProxyEnabled()) {
            log.info("Bot proxy enabled");
            log.info("Bot proxy type: {}", properties.getProxyType());
            log.info("Bot proxy host: {}", properties.getProxyHost());
            log.info("Bot proxy port: {}", properties.getProxyPort());
        }

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
        // Апдейты обрабатываются асинхронно: polling-поток не блокируется
        // на сетевых вызовах спеллера и запросах к БД.
        list.forEach(update -> workerFor(update).submit(() -> {
            try {
                processUpdate(update);
            } catch (Exception e) {
                log.error("Update processing failed: {}", e.getMessage(), e);
            }
        }));
    }

    private ExecutorService workerFor(Update update) {
        Long chatId = null;
        if (update.hasMessage() && update.getMessage() != null) {
            chatId = update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()
                && update.getCallbackQuery() != null
                && update.getCallbackQuery().getMessage() != null) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
        }
        int stripe = (int) Math.floorMod(chatId == null ? 0L : chatId, WORKERS);
        return workers.get(stripe);
    }

    private void processUpdate(Update update) {
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
    }

    @PreDestroy
    void shutdown() {
        workers.forEach(ExecutorService::shutdown);
    }
}
