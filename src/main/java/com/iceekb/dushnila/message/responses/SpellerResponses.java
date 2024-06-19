package com.iceekb.dushnila.message.responses;

import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class SpellerResponses {
    public static final List<String> data = List.of(
            "Не \"%s\", а \"%s\"!",
            "Хм.. \"%s\" не верно, верно \"%s\".",
            "Вообще-то не \"%s\", а \"%s\"!"
    );
}
