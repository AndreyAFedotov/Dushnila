package com.iceekb.dushnila.properties;

import com.iceekb.dushnila.message.enums.MessageValidationError;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public abstract class LastMessage {
    protected Long channelTgId;
    protected Long userTgId;
    protected String receivedMessage;
    protected boolean isPersonal;
    protected boolean isPersonalFromAdmin;
    protected boolean isValid;
    protected List<MessageValidationError> validationErrors;
    protected InlineKeyboardMarkup menu;
    protected String response;
    protected boolean isError;

    protected Boolean checkIsPersonal(long chatTgId, long userTgId) {
        return chatTgId == userTgId;
    }

    protected Boolean checkIsPersonalFromAdmin(long chatTgId, long userTgId, String botAdmin) {
        return chatTgId == userTgId && userTgId == Long.parseLong(botAdmin);
    }
}
