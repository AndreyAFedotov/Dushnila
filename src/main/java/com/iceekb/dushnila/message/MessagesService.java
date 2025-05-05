package com.iceekb.dushnila.message;

import com.iceekb.dushnila.jpa.entity.Channel;
import com.iceekb.dushnila.jpa.entity.Ignore;
import com.iceekb.dushnila.jpa.entity.Point;
import com.iceekb.dushnila.jpa.entity.Reaction;
import com.iceekb.dushnila.jpa.entity.User;
import com.iceekb.dushnila.jpa.enums.ChannelApproved;
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
import com.iceekb.dushnila.properties.LastMessage;
import com.iceekb.dushnila.speller.SpellerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class MessagesService {
    public static final String ERROR = "error";
    public static final String PARAM = "param";
    public static final String TO = "to";
    public static final String FROM = "from";

    private final ChannelRepo channelRepo;
    private final UserRepo userRepo;
    private final PointRepo pointRepo;
    private final ReactionRepo reactionRepo;
    private final IgnoreRepo ignoreRepo;
    private final SpellerService spellerService;

    public LastMessage onUpdate(Update update, BaseBotProperties properties) {
        LastMessage lastMessage = new LastMessage(update, properties);

        if (lastMessage.isValid()) {
            handleValidMessage(lastMessage, properties);
        } else {
            logValidationErrors(lastMessage);
        }

        return lastMessage;
    }

    private void handleValidMessage(LastMessage lastMessage, BaseBotProperties properties) {
        if (lastMessage.isPersonal()) {
            if (lastMessage.isPersonalFromAdmin()) {
                lastMessage.setChannelName("PERSONAL");
                onPersonalMessageFromAdmin(lastMessage, properties);
            } else {
                onPersonalMessage(lastMessage, properties);
            }
        } else {
            onChannelMessage(lastMessage);
        }
    }

    private void logValidationErrors(LastMessage lastMessage) {
        lastMessage.getValidationErrors().stream()
                .filter(error -> error != MessageValidationError.TYPE)
                .forEach(error -> log.error(error.getLabel()));
    }

    private void onChannelMessage(LastMessage lastMessage) {
        try {
            if (isPingPong(lastMessage)) return;

            checkMessageAccess(lastMessage);
            if (shouldReturn(lastMessage)) return;

            checkCommands(lastMessage);
            if (shouldReturn(lastMessage)) return;

            deleteIgnoreAndUsers(lastMessage);
            if (StringUtils.isBlank(lastMessage.getReceivedMessage())) return;

            checkReaction(lastMessage);
            if (shouldReturn(lastMessage)) return;

            speller(lastMessage);
        } catch (RuntimeException e) {
            log.error("Unexpected error: {}", e.getMessage());
        }
    }

    private boolean shouldReturn(LastMessage lastMessage) {
        return StringUtils.isNotBlank(lastMessage.getResponse()) || lastMessage.isError();
    }

    private void onPersonalMessageFromAdmin(LastMessage lastMessage, BaseBotProperties properties) {
        AdminCommand command = ServiceUtil.getAdminCommand(lastMessage);

        switch (command) {
            case HELP -> handleAdminHelpCommand(lastMessage);
            case APPROVE -> handleAdminApproveCommand(lastMessage, command);
            case DAPPROVE -> handleAdminDapproveCommand(lastMessage, command);
            case CHANNELS -> handleAdminChannelsCommand(lastMessage);
            case UPTIME -> handleAdminUptimeCommand(lastMessage, properties);
            default -> lastMessage.setResponse("Команда не распознана");
        }
    }

    private void checkReaction(LastMessage lastMessage) {
        List<Reaction> reactions = reactionRepo.findAllByChannelId(lastMessage.getChannel().getId());
        Map<String, String> reactionMap = reactions.stream()
                .collect(Collectors.toMap(
                        reaction -> reaction.getTextFrom().toLowerCase(),
                        Reaction::getTextTo,
                        (existing, replacement) -> existing));

        // Обрабатываем сообщение
        String[] words = lastMessage.getReceivedMessage()
                .replaceAll("\\p{Punct}", " ")  // Заменяем знаки препинания на пробелы
                .replaceAll("\\s+", " ")        // Заменяем множественные пробелы на один
                .trim()
                .split("\\s+");                 // Разбиваем по пробелам

        // Обрабатываем слова и собираем результат
        StringJoiner result = new StringJoiner(". ");
        for (String word : words) {
            if (StringUtils.isNotBlank(word)) {
                // Ищем реакцию (без учета регистра)
                String reaction = reactionMap.getOrDefault(word.toLowerCase(), null);
                if (reaction != null) {
                    result.add(reaction);
                }
            }
        }

        String response = result.toString();
        if (response != null && !response.isEmpty()) {
            lastMessage.setResponse(response);
        }
    }

    private void speller(LastMessage lastMessage) {
        lastMessage = spellerService.speller(lastMessage);
        if (StringUtils.isNotBlank(lastMessage.getResponse())) {
            addPoint(lastMessage);
        }
    }

    private void addPoint(LastMessage lastMessage) {
        Point point = pointRepo.findPointsForChannelIdAndUserId(
                lastMessage.getChannel().getId(),
                lastMessage.getUser().getId()
        );
        if (point != null) {
            point.setPointCount(point.getPointCount() + 1);
            pointRepo.save(point);
        } else {
            Point newPoint = ServiceUtil.createNewPoint(lastMessage);
            pointRepo.save(newPoint);
        }
    }

    private void deleteIgnoreAndUsers(LastMessage lastMessage) {
        Long chatId = lastMessage.getChannel().getId();
        Set<String> ignoredWords = ignoreRepo.findAllByChatId(chatId).stream()
                .map(Ignore::getWord)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        String processedMessage = Arrays.stream(lastMessage.getReceivedMessage()
                        .replaceAll("\\p{Punct}&&[^@]", " ")  // Удаляем все знаки препинания, КРОМЕ @
                        .replaceAll("\\s+", " ")        // Нормализуем пробелы
                        .trim()
                        .split("\\s+"))                // Разбиваем по пробелам
                .filter(word -> !word.isEmpty())        // Удаляем пустые строки
                .filter(word -> !ignoredWords.contains(word.toLowerCase())) // Фильтруем игнорируемые слова
                .filter(word -> word.charAt(0) != '@')  // Фильтруем юзернеймы
                .collect(Collectors.joining(" "));     // Собираем обратно в строку

        lastMessage.setReceivedMessage(processedMessage);
    }


    private void checkMessageAccess(LastMessage lastMessage) {
        // Channel
        Channel channelResult = retrieveOrCreateChannel(lastMessage);
        channelRepo.save(channelResult);
        // User
        if (!lastMessage.isError()) {
            User user = retrieveOrCreateUser(lastMessage);
            userRepo.save(user);
        }
    }

    private Channel retrieveOrCreateChannel(LastMessage lastMessage) {
        Long channelTgId = ServiceUtil.checkChannelTgId(lastMessage.getChannelTgId());
        Channel channel = channelRepo.findByTgId(channelTgId);
        return (channel == null) ? ServiceUtil.createNewChannel(lastMessage) : ServiceUtil.channelAnalysis(channel, lastMessage);
    }

    private User retrieveOrCreateUser(LastMessage lastMessage) {
        User user = userRepo.findByTgId(lastMessage.getUserTgId());
        return (user == null) ? ServiceUtil.createNewUser(lastMessage) : ServiceUtil.userAnalysis(user, lastMessage);
    }

    private void checkCommands(LastMessage lastMessage) {
        ChatCommand command = ServiceUtil.getCommand(lastMessage);
        if (command != null) {
            doCommandAction(command, lastMessage);
        }
    }

    private void doCommandAction(ChatCommand command, LastMessage lastMessage) {
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

    private void listIgnore(LastMessage lastMessage) {
        List<Ignore> words = ignoreRepo.findAllByChatId(lastMessage.getChannel().getId());

        if (!words.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Слова:").append("\n");
            for (Ignore ignore : words) {
                sb.append(ignore.getWord()).append("\n");
            }
            lastMessage.setResponse(sb.toString());
        } else {
            lastMessage.setResponse("Список пуст");
        }
    }

    private void deleteIgnore(LastMessage lastMessage) {
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

    private void createIgnore(LastMessage lastMessage) {
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

    private void listReplace(LastMessage lastMessage) {
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

    private void deleteReplace(LastMessage lastMessage) {
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

    private void createReplace(LastMessage lastMessage) {
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

    private void createHelp(LastMessage lastMessage) {
        lastMessage.setResponse(TextUtil.HELP_MESSAGE);
    }

    private void createStat(LastMessage lastMessage) {
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

    private void handleAdminHelpCommand(LastMessage lastMessage) {
        lastMessage.setResponse(TextUtil.ADMIN_MENU);
    }

    private void handleAdminApproveCommand(LastMessage lastMessage, AdminCommand command) {
        Channel channel = checkAdmChannel(lastMessage, command);
        if (channel != null) {
            if (channel.getApproved() == ChannelApproved.APPROVED) {
                lastMessage.setResponse("Канал уже одобрен");
            } else {
                channel.setApproved(ChannelApproved.APPROVED);
                channelRepo.save(channel);
                log.info("Channel is approved {}", channel.getChatName());
                lastMessage.setResponse("Канал одобрен");
            }
        }
    }

    private void handleAdminDapproveCommand(LastMessage lastMessage, AdminCommand command) {
        Channel channel = checkAdmChannel(lastMessage, command);
        if (channel != null) {
            if (channel.getApproved() == ChannelApproved.APPROVED || channel.getApproved() == ChannelApproved.WAITING) {
                channel.setApproved(ChannelApproved.REJECTED);
                channelRepo.save(channel);
                lastMessage.setResponse("Одобрение снято");
                log.info("Channel unapproved {}", channel.getChatName());
            } else {
                lastMessage.setResponse("Канал еще не одобрен: " + channel.getApproved().getDesc());
            }
        }
    }

    private void handleAdminChannelsCommand(LastMessage lastMessage) {
        List<Channel> channels = channelRepo.findAll();
        StringBuilder result = new StringBuilder("Список каналов:\n");
        channels.forEach(channel -> result.append(channel.getChatName())
                .append(" --- ")
                .append(channel.getApproved())
                .append("\n"));
        lastMessage.setResponse(result.toString());
    }

    private void handleAdminUptimeCommand(LastMessage lastMessage, BaseBotProperties properties) {
        Duration uptime = Duration.between(properties.getStartTime(), LocalDateTime.now());
        String uptimeStr = setUptimeLine(uptime);
        lastMessage.setResponse(uptimeStr);
    }

    private String setUptimeLine(Duration uptime) {
        long days = uptime.toDaysPart();
        long hours = uptime.toHoursPart();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();

        return String.format("Bot uptime: %03d days %02d:%02d:%02d", days, hours, minutes, seconds);
    }


    private Channel checkAdmChannel(LastMessage lastMessage, AdminCommand command) {
        String message = lastMessage.getReceivedMessage().replaceFirst("/", "").trim();
        message = message.replace(command.toString().toLowerCase(), "").trim();
        if (message.isEmpty()) {
            lastMessage.setResponse("Не верное количество параметров");
            return null;
        }

        Channel channel = channelRepo.findByChatName(message);
        if (channel == null) {
            lastMessage.setResponse("Канал не найден");
            return null;
        }
        return channel;
    }

    private void onPersonalMessage(LastMessage lastMessage, BaseBotProperties properties) {
        lastMessage.setResponse(TextUtil.nextAutoMessage(ResponseTypes.PERSONAL) + " Для связи: " + properties.getAdminMail());
    }

    private static Map<String, String> getLine1Param(LastMessage lastMessage) {
        Map<String, String> data = TextUtil.line1param(lastMessage.getReceivedMessage());
        if (data.containsKey(ERROR)) {
            lastMessage.setError(true);
            lastMessage.setResponse(data.get(ERROR));
        }
        return data;
    }

    private static Map<String, String> getLine2Param(LastMessage lastMessage) {
        Map<String, String> data = TextUtil.line2param(lastMessage.getReceivedMessage());
        if (data.containsKey(ERROR)) {
            lastMessage.setError(true);
            lastMessage.setResponse(data.get(ERROR));
        }
        return data;
    }

    private boolean isPingPong(LastMessage lastMessage) {
        if (lastMessage.getReceivedMessage().equalsIgnoreCase("ping")) {
            lastMessage.setResponse("pong");
            log.info(">>>>> Ping-pong action by {}! :)", lastMessage.getUserName());
            return true;
        }
        return false;
    }
}