package com.iceekb.dushnila.service;

import com.iceekb.dushnila.jpa.entity.Channel;
import com.iceekb.dushnila.jpa.entity.Ignore;
import com.iceekb.dushnila.jpa.entity.Point;
import com.iceekb.dushnila.jpa.entity.Reaction;
import com.iceekb.dushnila.jpa.entity.User;
import com.iceekb.dushnila.jpa.repo.ChannelRepo;
import com.iceekb.dushnila.jpa.repo.IgnoreRepo;
import com.iceekb.dushnila.jpa.repo.PointRepo;
import com.iceekb.dushnila.jpa.repo.ReactionRepo;
import com.iceekb.dushnila.jpa.repo.UserRepo;
import com.iceekb.dushnila.message.enums.AdminCommand;
import com.iceekb.dushnila.message.enums.ChatCommand;
import com.iceekb.dushnila.message.enums.MessageValidationError;
import com.iceekb.dushnila.message.enums.ResponseTypes;
import com.iceekb.dushnila.message.util.ServiceUtil;
import com.iceekb.dushnila.message.util.TextUtil;
import com.iceekb.dushnila.properties.BaseBotProperties;
import com.iceekb.dushnila.properties.LastMessageTxt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class MessagesService {
    private final static List<AdminCommand> OK_COMMANDS = List.of(
            AdminCommand.APPROVE,
            AdminCommand.DAPPROVE,
            AdminCommand.DELETE_CHANNEL,
            AdminCommand.CHANNELS,
            AdminCommand.UPTIME
    );

    public static final String ERROR = "error";
    public static final String PARAM = "param";
    public static final String TO = "to";
    public static final String FROM = "from";

    private final ChannelRepo channelRepo;
    private final UserRepo userRepo;
    private final PointRepo pointRepo;
    private final ReactionRepo reactionRepo;
    private final IgnoreRepo ignoreRepo;
    private final IgnoreFilterService ignoreFilterService;
    private final ReactionService reactionService;
    private final SpellerService spellerService;
    private final AutoResponseService responseService;
    private final PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;

    @PostConstruct
    void initTx() {
        this.tx = new TransactionTemplate(transactionManager);
    }

    private void runInTx(Runnable action) {
        // Защита для unit-тестов/нестандартных окружений: если tx не инициализирован, выполняем напрямую.
        if (tx == null) {
            action.run();
            return;
        }
        tx.executeWithoutResult(status -> action.run());
    }

    public LastMessageTxt onUpdate(Update update, BaseBotProperties properties) {
        LastMessageTxt lastMessage = new LastMessageTxt(update, properties);

        if (lastMessage.isValid()) {
            handleValidMessage(lastMessage, properties);
        } else {
            logValidationErrors(lastMessage);
        }

        return lastMessage;
    }

    private void handleValidMessage(LastMessageTxt lastMessage, BaseBotProperties properties) {
        if (lastMessage.isPersonal()) {
            if (lastMessage.isPersonalFromAdmin()) {
                lastMessage.setChannelName("PERSONAL");
                onPersonalMessageFromAdmin(lastMessage);
            } else {
                onPersonalMessage(lastMessage, properties);
            }
        } else {
            onChannelMessage(lastMessage);
        }
    }

    private void logValidationErrors(LastMessageTxt lastMessage) {
        try {
            lastMessage.getValidationErrors().stream()
                    .filter(error -> error != MessageValidationError.TYPE)
                    .forEach(error -> log.error(error.getLabel()));
        } catch (Exception e) {
            log.error("The message is not valid. But the validator's message is err: {}", e.getMessage());
        }
    }

    private void onChannelMessage(LastMessageTxt lastMessage) {
        try {
            if (isPingPong(lastMessage)) return;

            runInTx(() -> checkMessageAccess(lastMessage));
            if (shouldReturn(lastMessage)) return;

            runInTx(() -> checkCommands(lastMessage));
            if (shouldReturn(lastMessage)) return;

            lastMessage.setReceivedMessage(
                    ignoreFilterService.filterMessage(lastMessage.getChannel().getId(), lastMessage.getReceivedMessage())
            );
            if (StringUtils.isBlank(lastMessage.getReceivedMessage())) return;

            reactionService.applyReaction(lastMessage);
            if (shouldReturn(lastMessage)) return;

            speller(lastMessage);
        } catch (RuntimeException e) {
            log.error("Unexpected error: {}", e.getMessage());
        }
    }

    private boolean shouldReturn(LastMessageTxt lastMessage) {
        return StringUtils.isNotBlank(lastMessage.getResponse()) || lastMessage.isError();
    }

    private void onPersonalMessageFromAdmin(LastMessageTxt lastMessage) {
        if (!lastMessage.getReceivedMessage().equals("/help") && !lastMessage.getReceivedMessage().equals("/start")) {
            lastMessage.setResponse("Команда не распознана (/start)");
            return;
        }

        InlineKeyboardMarkup markupInline = Arrays.stream(AdminCommand.values())
                .filter(OK_COMMANDS::contains)
                .map(cmd -> InlineKeyboardButton.builder()
                        .text(cmd.getLabel())
                        .callbackData(cmd.toString())
                        .build())
                .map(InlineKeyboardRow::new)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        InlineKeyboardMarkup::new
                ));

        lastMessage.setMenu(markupInline);
        lastMessage.setResponse("Меню администратора");
    }

    private void speller(LastMessageTxt lastMessage) {
        spellerService.speller(lastMessage);
        if (StringUtils.isNotBlank(lastMessage.getResponse())) {
            // Важно: начисление очков делаем в отдельной короткой транзакции,
            // чтобы не держать транзакцию БД во время сетевого запроса к спеллеру.
            runInTx(() -> addPoint(lastMessage));
        }
    }

    private void addPoint(LastMessageTxt lastMessage) {
        pointRepo.incrementPoint(
                lastMessage.getChannel().getId(),
                lastMessage.getUser().getId()
        );
    }

    private void checkMessageAccess(LastMessageTxt lastMessage) {
        // Channel
        Channel channelResult = retrieveOrCreateChannel(lastMessage);
        channelRepo.save(channelResult);
        // User
        if (!lastMessage.isError()) {
            User user = retrieveOrCreateUser(lastMessage);
            userRepo.save(user);
        }
    }

    private Channel retrieveOrCreateChannel(LastMessageTxt lastMessage) {
        Long channelTgId = ServiceUtil.checkChannelTgId(lastMessage.getChannelTgId());
        Channel channel = channelRepo.findByTgId(channelTgId);
        return (channel == null) ? ServiceUtil.createNewChannel(lastMessage) : ServiceUtil.channelAnalysis(channel, lastMessage);
    }

    private User retrieveOrCreateUser(LastMessageTxt lastMessage) {
        User user = userRepo.findByTgId(lastMessage.getUserTgId());
        return (user == null) ? ServiceUtil.createNewUser(lastMessage) : ServiceUtil.userAnalysis(user, lastMessage);
    }

    private void checkCommands(LastMessageTxt lastMessage) {
        ChatCommand command = ServiceUtil.getCommand(lastMessage);
        if (command != null) {
            doCommandAction(command, lastMessage);
        }
    }

    private void doCommandAction(ChatCommand command, LastMessageTxt lastMessage) {
        switch (command) {
            case STAT -> createStat(lastMessage);
            case HELP -> createHelp(lastMessage);
            case CREPLACE -> createReplace(lastMessage);
            case DREPLACE -> deleteReplace(lastMessage);
            case LREPLACE -> listReplace(lastMessage);
            case CIGNORE -> createIgnore(lastMessage);
            case DIGNORE -> deleteIgnore(lastMessage);
            case LIGNORE -> listIgnore(lastMessage);
            default -> throw new IllegalStateException("Unexpected Command value: " + command);
        }
    }

    private void listIgnore(LastMessageTxt lastMessage) {
        List<Ignore> words = ignoreRepo.findAllByChatId(lastMessage.getChannel().getId());

        if (!words.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Игнор (слова / фразы / маски):").append("\n");
            for (Ignore ignore : words) {
                sb.append(ignore.getWord()).append("\n");
            }
            lastMessage.setResponse(sb.toString());
        } else {
            lastMessage.setResponse("Список пуст");
        }
    }

    private void deleteIgnore(LastMessageTxt lastMessage) {
        Map<String, String> data = getLine1Param(lastMessage);
        if (data.containsKey(ERROR)) return;

        Ignore ignore = ignoreRepo.findByWordAndChatId(data.get(PARAM), lastMessage.getChannel().getId());
        if (ignore == null) {
            lastMessage.setResponse("Не обнаружено");
        } else {
            ignoreRepo.deleteById(ignore.getId());
            lastMessage.setResponse("Удалено: " + ignore.getWord());
            log.info("Ignore removed <{}> for channel <{}> by user <{}>",
                    ignore.getWord(),
                    lastMessage.getChannel().getChatName(),
                    lastMessage.getUser().getNickName());
        }
    }

    private void createIgnore(LastMessageTxt lastMessage) {
        Map<String, String> data = getLine1Param(lastMessage);
        if (data.containsKey(ERROR)) return;

        Ignore ignore = ignoreRepo.findByWordAndChatId(data.get(PARAM), lastMessage.getChannel().getId());
        if (ignore != null) {
            lastMessage.setResponse("Уже настроено");
        } else {
            Ignore ignoreResult = ServiceUtil.createNewIgnore(lastMessage, data);
            lastMessage.setResponse("Добавлено: " + ignoreResult.getWord());
            ignoreRepo.save(ignoreResult);
            log.info("Ignore created <{}> for channel <{}> by user <{}>",
                    ignoreResult.getWord(),
                    lastMessage.getChannel().getChatName(),
                    lastMessage.getUser().getNickName());
        }
    }

    private void listReplace(LastMessageTxt lastMessage) {
        List<Reaction> pairs = reactionRepo.findAllByCatTgId(lastMessage.getChannel().getTgId());
        if (!pairs.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Пары:").append("\n");
            for (Reaction reaction : pairs) {
                sb.append(reaction.getTextFrom()).append(" -> ").append(reaction.getTextTo()).append("\n");
            }
            lastMessage.setResponse(sb.toString());
        } else {
            lastMessage.setResponse("Список пуст");
        }

    }

    private void deleteReplace(LastMessageTxt lastMessage) {
        Map<String, String> data = getLine1Param(lastMessage);
        if (data.containsKey(ERROR)) return;

        Reaction reaction = reactionRepo.findByTextFromAndChanelId(data.get(PARAM), lastMessage.getChannel().getId());
        if (reaction == null) {
            lastMessage.setResponse("Не обнаружено");
        } else {
            reactionRepo.deleteById(reaction.getId());
            lastMessage.setResponse("Удалено");
            log.info("Replace deleted <{} -> {}> for channel <{}> by user <{}>",
                    reaction.getTextFrom(),
                    reaction.getTextTo(),
                    lastMessage.getChannel().getChatName(),
                    lastMessage.getUser().getNickName());
        }
    }

    private void createReplace(LastMessageTxt lastMessage) {
        Map<String, String> data = getLine2Param(lastMessage);
        if (data.containsKey(ERROR)) return;

        Reaction reaction = reactionRepo.findByTextFromAndChanelId(data.get(FROM), lastMessage.getChannel().getId());
        if (reaction != null) {
            reaction.setTextTo(data.get(TO));
            reaction.setCreatedOn(LocalDateTime.now());
            lastMessage.setResponse(
                    String.format("Обновлена замена: \"%s\" на \"%s\"",
                            reaction.getTextFrom(),
                            reaction.getTextTo())
            );
            reactionRepo.save(reaction);
            log.info("Replace updated <{} -> {}> for channel <{}> by user <{}>",
                    reaction.getTextFrom(),
                    reaction.getTextTo(),
                    lastMessage.getChannel().getChatName(),
                    lastMessage.getUser().getNickName());

            return;
        }

        Reaction newReaction = ServiceUtil.createNewReaction(lastMessage, data);
        reactionRepo.save(newReaction);
    }

    private void createHelp(LastMessageTxt lastMessage) {
        lastMessage.setResponse(TextUtil.HELP_MESSAGE);
    }

    private void createStat(LastMessageTxt lastMessage) {
        List<Point> points = pointRepo.findPointsForChannelId(lastMessage.getChannel().getId());

        if (points != null && !points.isEmpty()) {
            StringBuilder stb = new StringBuilder();
            stb.append("Наши чемпионы (ошибок): \n\n");
            int count = 0;
            for (Point point : points) {
                stb.append(++count).append(". ").append("@");
                stb.append(point.getUser().getNickName()).append(": ").append(point.getPointCount()).append("\n");
            }
            lastMessage.setResponse(stb.toString());
        }
    }

    private void onPersonalMessage(LastMessageTxt lastMessage, BaseBotProperties properties) {
        // Для лички `channel` может быть не инициализирован (ветка идёт в обход checkMessageAccess),
        // поэтому используем безопасный ключ для шаблонов ответов.
        Long responseKey = lastMessage.getChannel() != null
                ? lastMessage.getChannel().getId()
                : lastMessage.getUserTgId();
        lastMessage.setResponse(responseService.getMessage(ResponseTypes.PERSONAL, responseKey) + " Для связи: " + properties.getAdminMail());
    }

    private static Map<String, String> getLine1Param(LastMessageTxt lastMessage) {
        Map<String, String> data = TextUtil.line1param(lastMessage.getReceivedMessage());
        if (data.containsKey(ERROR)) {
            lastMessage.setError(true);
            lastMessage.setResponse(data.get(ERROR));
        }
        return data;
    }

    private static Map<String, String> getLine2Param(LastMessageTxt lastMessage) {
        Map<String, String> data = TextUtil.line2param(lastMessage.getReceivedMessage());
        if (data.containsKey(ERROR)) {
            lastMessage.setError(true);
            lastMessage.setResponse(data.get(ERROR));
        }
        return data;
    }

    private boolean isPingPong(LastMessageTxt lastMessage) {
        if (lastMessage.getReceivedMessage().equalsIgnoreCase("ping")) {
            lastMessage.setResponse("pong");
            log.info(">>>>> Ping-pong action by {}! :)", lastMessage.getUserName());
            return true;
        }
        return false;
    }
}