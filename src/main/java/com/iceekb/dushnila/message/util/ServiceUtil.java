package com.iceekb.dushnila.message.util;

import com.iceekb.dushnila.jpa.entity.Channel;
import com.iceekb.dushnila.jpa.entity.Ignore;
import com.iceekb.dushnila.jpa.entity.Point;
import com.iceekb.dushnila.jpa.entity.Reaction;
import com.iceekb.dushnila.jpa.entity.User;
import com.iceekb.dushnila.jpa.enums.ChannelApproved;
import com.iceekb.dushnila.message.enums.ChatCommand;
import com.iceekb.dushnila.message.enums.MessageValidationError;
import com.iceekb.dushnila.properties.LastMessageTxt;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class ServiceUtil {
    public static final String TO = "to";
    public static final String FROM = "from";
    public static final String PARAM = "param";

    private ServiceUtil() {

    }

    public static Channel channelAnalysis(Channel channel, LastMessageTxt lastMessage) {
        channel.setLastMessage(LocalDateTime.now());
        channel.setMessageCount(channel.getMessageCount() + 1);
        if (!Objects.equals(channel.getChatName(), lastMessage.getChannelName())) {
            channel.setChatName(lastMessage.getChannelName());
        }

        if (channel.getApproved() == ChannelApproved.REJECTED || channel.getApproved() == ChannelApproved.WAITING) {
            lastMessage.setChannel(channel);
            lastMessage.getValidationErrors().add(MessageValidationError.CHANNEL_NOT_APPROVED);
            lastMessage.setError(true);
            return channel;
        }

        lastMessage.setChannel(channel);

        return channel;
    }

    public static Channel createNewChannel(LastMessageTxt lastMessage) {
        Channel channel = Channel.builder()
                .tgId(checkChannelTgId(lastMessage.getChannelTgId()))
                .chatName(lastMessage.getChannelName())
                .messageCount(1L)
                .firstMessage(LocalDateTime.now())
                .lastMessage(LocalDateTime.now())
                .approved(ChannelApproved.WAITING)
                .build();

        lastMessage.setChannel(channel);
        lastMessage.getValidationErrors().add(MessageValidationError.CHANNEL_NOT_APPROVED);
        lastMessage.setError(true);

        return channel;
    }

    public static Long checkChannelTgId(Long channelTgId) {
        if (channelTgId == null) {
            return null;
        }

        String channelIdStr = String.valueOf(channelTgId);
        if (channelIdStr.startsWith("-100")) {
            channelIdStr = channelIdStr.substring(4); // Удаляем префикс "-100"
        }

        return Long.parseLong(channelIdStr.replace("-", ""));
    }

    public static User createNewUser(LastMessageTxt lastMessage) {
        var now = LocalDateTime.now();
        User user = User.builder()
                .tgId(lastMessage.getUserTgId())
                .nickName(lastMessage.getUserName())
                .firstMessage(now)
                .lastMessage(now)
                .build();

        lastMessage.setUser(user);
        return user;
    }

    public static User userAnalysis(User user, LastMessageTxt lastMessage) {
        user.setLastMessage(LocalDateTime.now());
        if (!Objects.equals(user.getNickName(), lastMessage.getUserName())) {
            user.setNickName(lastMessage.getUserName());
        }
        lastMessage.setUser(user);
        return user;
    }

    public static Reaction createNewReaction(LastMessageTxt lastMessage, Map<String, String> data) {
        Reaction newReaction = Reaction.builder()
                .channel(lastMessage.getChannel())
                .textFrom(data.get(FROM))
                .textTo(data.get(TO))
                .user(lastMessage.getUser())
                .createdOn(LocalDateTime.now())
                .build();
        lastMessage.setResponse(
                String.format("Добавлена замена: \"%s\" на \"%s\"",
                        newReaction.getTextFrom(),
                        newReaction.getTextTo()
                ));

        log.info("Replace created <{} -> {}> for channel <{}> by user <{}>",
                newReaction.getTextFrom(),
                newReaction.getTextTo(),
                lastMessage.getChannel().getChatName(),
                lastMessage.getUser().getNickName());

        return newReaction;
    }

    public static ChatCommand getCommand(LastMessageTxt lastMessage) {
        String receivedMessage = lastMessage.getReceivedMessage();
        if (receivedMessage.startsWith("/")) {
            String[] parts = receivedMessage.split(" ", 2); // Разбиваем сообщение на две части: команда и остальная часть
            String commandStr = parts[0].toUpperCase().replace("/", "");

            try {
                return ChatCommand.valueOf(commandStr);
            } catch (IllegalArgumentException e) {
                log.error("Unknown command <{}>", commandStr);
                lastMessage.setResponse("Неизвестная команда");
                lastMessage.setError(true);
            }
        }
        return null;
    }

    public static Point createNewPoint(LastMessageTxt lastMessage) {
        return Point.builder()
                .channel(lastMessage.getChannel())
                .user(lastMessage.getUser())
                .pointCount(1L)
                .build();
    }

    public static Ignore createNewIgnore(LastMessageTxt lastMessage, Map<String, String> data) {
        return Ignore.builder()
                .channel(lastMessage.getChannel())
                .word(data.get(PARAM))
                .user(lastMessage.getUser())
                .createdOn(LocalDateTime.now())
                .build();
    }
}