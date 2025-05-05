package com.iceekb.dushnila.message.util;

import com.iceekb.dushnila.message.enums.ResponseTypes;
import com.iceekb.dushnila.message.responses.PersonalResponses;
import com.iceekb.dushnila.message.responses.SpellerResponses;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class TextUtil {
    public static final String FORMAT_ERROR = "Ошибочный формат команды";
    public static final String ERROR = "error";
    public static final String HELP_MESSAGE = """
            Команды бота:\s
            /help - данная справка
            /stat - статистика группы
            /cignore - игнор слова (/cignore "слово")
            /dignore - удалить игнор (/dignore "слово")
            /lignore - список игнора
            /creplace - замена (/creplace "слово" "на слово/фраза")
            /dreplace - удалить замену (/dreplace "слово")
            /lreplace - список замены
            """;
    public static final String ADMIN_MENU = """
            Команды бота:\s
            /approve channelName - одобрить канал
            /dapprove channelName - удалить одобрение
            /channels - список каналов
            /uptime - Uptime
            """;

    static Random random = new Random();

    private TextUtil() {
    }

    public static Map<String, String> line2param(String text) {
        text = text.replace("«", "\"");
        text = text.replace("»", "\"");
        List<String> data = List.of(text.split("\""));
        if (data.size() != 4) {
            return Map.of(ERROR, FORMAT_ERROR);
        }
        String command = data.get(0).replace("/", "").toUpperCase().trim();
        String from = data.get(1).trim();
        String to = data.get(3).trim();
        if (from.contains(" ")) {
            return Map.of(ERROR, "Преобразуем только слово, не фразу...");
        }
        if (command.isEmpty() || from.isEmpty() || to.isEmpty()) {
            return Map.of(ERROR, FORMAT_ERROR);
        }
        return Map.of(
                "command", command,
                "from", from.toLowerCase(),
                "to", to
        );
    }

    public static Map<String, String> line1param(String text) {
        text = text.replace("«", "\"");
        text = text.replace("»", "\"");
        List<String> data = List.of(text.split("\""));
        if (data.size() != 2) {
            return Map.of(ERROR, FORMAT_ERROR);
        }
        String command = data.get(0).replace("/", "").toUpperCase();
        command = command.trim();
        String param = data.get(1).trim();
        if (command.isEmpty() || param.isEmpty()) {
            return Map.of(ERROR, FORMAT_ERROR);
        }
        return Map.of(
                "command", command,
                "param", param.toLowerCase()
        );
    }

    public static String nextAutoMessage(ResponseTypes type) {
        return switch (type) {
            case SPELLER -> getRandomMessage(SpellerResponses.data);
            case PERSONAL -> getRandomMessage(PersonalResponses.data);
        };
    }

    private static String getRandomMessage(List<String> messages) {
        int size = messages.size();
        int rnd = random.nextInt(size);
        return messages.get(rnd);
    }
}
