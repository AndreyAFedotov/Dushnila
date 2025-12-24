package com.iceekb.dushnila.properties;

import com.iceekb.dushnila.jpa.entity.Channel;
import com.iceekb.dushnila.jpa.entity.User;
import com.iceekb.dushnila.message.enums.MessageValidationError;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@Slf4j
public class LastMessageTxt extends LastMessage {
    private Channel channel;
    private User user;
    private String channelName;
    private String userName;
    private Integer messageId;

    public LastMessageTxt(Update update, BaseBotProperties properties) {
        validationErrors = new ArrayList<>();
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            isValid = false;
            return;
        } else {
            isValid = true;
        }

        isError = false;
        menu = null;
        channelTgId = update.getMessage().getChatId();
        userTgId = update.getMessage().getFrom().getId();
        channelName = update.getMessage().getChat().getTitle();
        userName = collectUserName(update);
        messageId = update.getMessage().getMessageId();
        receivedMessage = update.getMessage().getText();
        isPersonal = checkIsPersonal(channelTgId, userTgId);
        isPersonalFromAdmin = checkIsPersonalFromAdmin(channelTgId, userTgId, properties.getBotAdmin());
        checkData();
    }

    private void checkData() {
        if (userTgId == null) {
            validationErrors.add(MessageValidationError.USER_ID);
        }
        if (channelTgId == null) {
            validationErrors.add(MessageValidationError.CHAT_ID);
        }
        if (channelName == null && userTgId != null && !isPersonalFromAdmin && !isPersonal) {
            validationErrors.add(MessageValidationError.CHAT_NAME);
        }
        if (receivedMessage == null) {
            validationErrors.add(MessageValidationError.EMPTY_MESSAGE);
        }
        if (!validationErrors.isEmpty()) {
            isValid = false;
        }
    }

    private String collectUserName(Update update) {
        String nickName = update.getMessage().getFrom().getUserName();
        String first = update.getMessage().getFrom().getFirstName();
        String last = update.getMessage().getFrom().getLastName();

        if (StringUtils.isNotBlank(nickName)) {
            if (StringUtils.isNotBlank(first) || StringUtils.isNotBlank(last)) {
                String fullName = Stream.of(first, last)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.joining(" "));
                return String.format("%s (%s)", nickName, fullName);
            }
            return nickName;
        }

        return Stream.of(first, last)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(" "));
    }
}