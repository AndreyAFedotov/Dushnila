package com.iceekb.dushnila.message;

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
import com.iceekb.dushnila.message.responses.AutoResponseService;
import com.iceekb.dushnila.message.util.ServiceUtil;
import com.iceekb.dushnila.message.util.TextUtil;
import com.iceekb.dushnila.properties.BaseBotProperties;
import com.iceekb.dushnila.properties.LastMessageTxt;
import com.iceekb.dushnila.speller.SpellerService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class MessagesService {
    private final static List<AdminCommand> OK_COMMANDS = List.of(
            AdminCommand.APPROVE,
            AdminCommand.DAPPROVE,
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

            deleteIgnoreAndUsers(lastMessage);
            if (StringUtils.isBlank(lastMessage.getReceivedMessage())) return;

            checkReaction(lastMessage);
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

    private void checkReaction(LastMessageTxt lastMessage) {
        List<Reaction> reactions = reactionRepo.findAllByChannelId(lastMessage.getChannel().getId());

        // Единая нормализация для текста сообщения и ключей реакций.
        // Важно: дефис/тире должны оставаться значимыми, иначе разные реакции
        // "мама анархия" и "мама - анархия" схлопнутся в один ключ.
        Function<String, String> normalizer = s -> {
            if (s == null) return "";
            return s.toLowerCase()
                    // Приводим любые Unicode-дефисы/тире к обычному '-'
                    .replaceAll("\\p{Pd}+", "-")
                    // Удаляем всю остальную Unicode-пунктуацию, КРОМЕ '-' (он нужен для различения реакций)
                    .replaceAll("[\\p{P}&&[^-]]", " ")
                    // Нормализуем пробелы вокруг '-': "a-b", "а -b", "a- b" => "a - b"
                    .replaceAll("\\s*-\\s*", " - ")
                    .replaceAll("\\s+", " ")
                    .trim();
        };

        if (reactions.isEmpty() || StringUtils.isBlank(lastMessage.getReceivedMessage())) {
            return;
        }

        // Нормализуем сообщение: приводим к нижнему регистру, убираем пунктуацию, нормализуем пробелы
        String originalLower = lastMessage.getReceivedMessage().toLowerCase();
        String normalisedMessage = normalizer.apply(lastMessage.getReceivedMessage());

        // Разделяем реакции на фразы и слова по НОРМАЛИЗОВАННОМУ ключу.
        List<Reaction> phraseReactions = reactions.stream()
                .filter(r -> normalizer.apply(r.getTextFrom()).contains(" "))
                .toList();

        List<Reaction> wordReactions = reactions.stream()
                .filter(r -> !normalizer.apply(r.getTextFrom()).contains(" "))
                .toList();

        // Множество для хранения позиций символов, покрытых найденными фразами
        Set<Integer> coveredPositions = new HashSet<>();
        StringJoiner result = new StringJoiner(". ");

        // Сначала обрабатываем фразы
        // Если несколько фраз нормализуются в один ключ — выбираем "лучшее" совпадение:
        // 1) точное совпадение по исходному тексту приоритетнее
        // 2) иначе совпадение по нормализованному тексту
        //noinspection ClassCanBeRecord
        class PhraseCandidate {
            final String originalKey;
            final String response;
            final int score; // 2 = exact, 1 = normalized

            PhraseCandidate(String originalKey, String response, int score) {
                this.originalKey = originalKey;
                this.response = response;
                this.score = score;
            }
        }

        Map<String, PhraseCandidate> bestByNormalized = new java.util.HashMap<>();
        for (Reaction r : phraseReactions) {
            String originalKey = (r.getTextFrom() == null) ? "" : r.getTextFrom().toLowerCase();
            String normalizedKey = normalizer.apply(r.getTextFrom());
            if (StringUtils.isBlank(normalizedKey)) continue;

            int score = 0;
            if (StringUtils.isNotBlank(originalKey) && originalLower.contains(originalKey)) {
                score = 2;
            } else if (normalisedMessage.contains(normalizedKey)) {
                score = 1;
            }
            if (score == 0) continue;

            PhraseCandidate current = bestByNormalized.get(normalizedKey);
            if (current == null
                    || score > current.score
                    || (score == current.score && originalKey.length() > current.originalKey.length())) {
                bestByNormalized.put(normalizedKey, new PhraseCandidate(originalKey, r.getTextTo(), score));
            }
        }

        bestByNormalized.forEach((phrase, cand) -> {
            int index = 0;
            boolean phraseFound = false;
            while ((index = normalisedMessage.indexOf(phrase, index)) != -1) {
                // Записываем все позиции символов этой фразы как покрытые
                for (int i = index; i < index + phrase.length(); i++) {
                    coveredPositions.add(i);
                }
                phraseFound = true;
                index += phrase.length(); // Переходим к следующему возможному вхождению
            }
            if (phraseFound) {
                result.add(cand.response);
            }
        });

        // Затем обрабатываем слова, исключая те, что были покрыты фразами
        Map<String, String> wordMap = wordReactions.stream()
                .collect(Collectors.toMap(
                        r -> normalizer.apply(r.getTextFrom()),
                        Reaction::getTextTo,
                        (existing, replacement) -> existing
                ));

        wordMap.forEach((word, value) -> {
            // Важно: \b в Java по умолчанию плохо работает с кириллицей (без UNICODE_CHARACTER_CLASS),
            // поэтому используем Unicode-границы "не буква/цифра/_" вокруг слова.
            String pattern = "(?iu)(?<![\\p{L}\\p{N}_])" + Pattern.quote(word) + "(?![\\p{L}\\p{N}_])";
            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(normalisedMessage);
            
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                
                // Проверяем, не покрыта ли эта позиция фразой
                boolean isCovered = false;
                for (int i = start; i < end; i++) {
                    if (coveredPositions.contains(i)) {
                        isCovered = true;
                        break;
                    }
                }
                
                if (!isCovered) {
                    result.add(value);
                    break; // Достаточно одного совпадения для слова
                }
            }
        });

        String response = result.toString();
        if (StringUtils.isNotBlank(response)) {
            lastMessage.setResponse(response);
        }
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

    private void deleteIgnoreAndUsers(LastMessageTxt lastMessage) {
        Long chatId = lastMessage.getChannel().getId();
        Set<String> ignoredWords = ignoreRepo.findAllByChatId(chatId).stream()
                .map(Ignore::getWord)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        String processedMessage = Arrays.stream(lastMessage.getReceivedMessage()
                        // Приводим любые Unicode-дефисы/тире к обычному '-'
                        .replaceAll("\\p{Pd}+", "-")
                        // Удаляем всю Unicode-пунктуацию, КРОМЕ @ и '-' (дефис нужен для фразовых реакций)
                        .replaceAll("[\\p{P}&&[^@-]]", " ")
                        // Нормализуем пробелы вокруг '-': "a-b", "а -b", "a- b" => "a - b"
                        .replaceAll("\\s*-\\s*", " - ")
                        .replaceAll("\\s+", " ")        // Нормализуем пробелы
                        .trim()
                        .split("\\s+"))                // Разбиваем по пробелам
                .filter(word -> !word.isEmpty())        // Удаляем пустые строки
                .filter(word -> !ignoredWords.contains(word.toLowerCase())) // Фильтруем игнорируемые слова
                .filter(word -> word.charAt(0) != '@')  // Фильтруем юзернеймы
                .collect(Collectors.joining(" "));     // Собираем обратно в строку

        lastMessage.setReceivedMessage(processedMessage);
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
            sb.append("Слова:").append("\n");
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