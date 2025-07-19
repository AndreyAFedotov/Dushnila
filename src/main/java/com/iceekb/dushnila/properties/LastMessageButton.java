package com.iceekb.dushnila.properties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;

@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@Slf4j
public class LastMessageButton extends LastMessage {

    private CallbackQuery query;

    public LastMessageButton(Update update, BaseBotProperties properties) {
        this.query = update.getCallbackQuery();
        validationErrors = new ArrayList<>();
        channelTgId = query.getMessage().getChatId();
        userTgId = query.getFrom().getId();
        receivedMessage = query.getData();
        isPersonal = checkIsPersonal(channelTgId, userTgId);
        isPersonalFromAdmin = checkIsPersonalFromAdmin(channelTgId, userTgId, properties.getBotAdmin());
        isValid = true;
    }
}
