package com.iceekb.dushnila.message.responses;

import com.iceekb.dushnila.message.enums.ResponseTypes;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@Data
public class AutoResponseService {

    public static final List<String> PUBLIC_DATA = List.of(
            "Не \"%s\", а \"%s\"! \uD83D\uDC40 ",
            "Хм.. \"%s\" не верно, верно \"%s\". \uD83D\uDE44",
            "Вообще-то не \"%s\", а \"%s\"! \uD83E\uDD13 ",
            "Но... ведь правильно \"%s\", а не \"%s\"... \uD83E\uDD14 ",
            "\"%s\" - ошибка. Запоминаем: \"%s\".",
            "\"%s\"? *вздох* Ладно... \"%s\". \uD83D\uDE2E\u200D\uD83D\uDCA8 ",
            "\"%s\" — это смело. Но правильно \"%s\".",
            "Ой, \"%s\" — это сильно. Лучше \"%s\". \uD83D\uDCA9 "
    );

    public static final List<String> PERSONAL_DATA = List.of(
            "Личка только для админа! :)",
            "Ты не админ!",
            "Сегодня вход только по приглашениям ;)"
    );

    private final Random random = new Random();
    private final Map<ResponseTypes, List<String>> responsesCache = new EnumMap<>(ResponseTypes.class);
    private final Map<ResponseTypes, List<String>> workingCopies = new EnumMap<>(ResponseTypes.class);

    @PostConstruct
    public void init() {
        responsesCache.put(ResponseTypes.PUBLIC, PUBLIC_DATA);
        responsesCache.put(ResponseTypes.PERSONAL, PERSONAL_DATA);
        resetWorkingCopies();
    }

    public String getMessage(ResponseTypes responseType) {
        return getRandomMessage(responseType);
    }

    private String getRandomMessage(ResponseTypes responseType) {
        List<String> workingCopy = workingCopies.get(responseType);
        if (workingCopy.isEmpty()) {
            workingCopy = new ArrayList<>(responsesCache.get(responseType));
            workingCopies.put(responseType, workingCopy);
        }

        int randomIndex = random.nextInt(workingCopy.size());
        String message = workingCopy.get(randomIndex);
        workingCopy.remove(randomIndex);

        return message;
    }

    private void resetWorkingCopies() {
        responsesCache.forEach((type, list) ->
                workingCopies.put(type, new ArrayList<>(list))
        );
    }
}
