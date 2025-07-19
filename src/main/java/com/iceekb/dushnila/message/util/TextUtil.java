package com.iceekb.dushnila.message.util;

import java.util.List;
import java.util.Map;

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

}
