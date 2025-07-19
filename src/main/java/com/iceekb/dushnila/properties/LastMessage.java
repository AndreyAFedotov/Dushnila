package com.iceekb.dushnila.properties;

import com.iceekb.dushnila.jpa.entity.Channel;
import com.iceekb.dushnila.jpa.entity.User;
import com.iceekb.dushnila.message.enums.MessageValidationError;
import com.iceekb.dushnila.message.enums.UpdateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Slf4j
public class LastMessage {
    private Channel channel;
    private User user;
    private Long channelTgId;
    private String channelName;
    private Long userTgId;
    private String userName;
    private Integer messageId;
    private String receivedMessage;
    private boolean isPersonal;
    private boolean isPersonalFromAdmin;
    private String response;
    private boolean isValid;
    private List<MessageValidationError> validationErrors;
    private boolean isError;

    public LastMessage(Update update, BaseBotProperties properties) {
        validationErrors = new ArrayList<>();
        if (!isTextMessage(update)) {
            isValid = false;
            return;
        } else {
            isValid = true;
        }

        isError = false;
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

    private boolean isTextMessage(Update update) {
        return getUpdateType(update) == UpdateType.TEXT_MESSAGE;
    }

    private UpdateType getUpdateType(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            return UpdateType.TEXT_MESSAGE;
        }
        return UpdateType.OTHER;
    }

    private Boolean checkIsPersonal(long chatTgId, long userTgId) {
        return chatTgId == userTgId;
    }

    private Boolean checkIsPersonalFromAdmin(long chatTgId, long userTgId, String botAdmin) {
        return chatTgId == userTgId && userTgId == Long.parseLong(botAdmin);
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