package com.iceekb.dushnila.service;

import com.iceekb.dushnila.jpa.entity.Channel;
import com.iceekb.dushnila.jpa.repo.IgnoreRepo;
import com.iceekb.dushnila.jpa.repo.PointRepo;
import com.iceekb.dushnila.jpa.repo.ReactionRepo;
import com.iceekb.dushnila.message.enums.ChannelApproved;
import com.iceekb.dushnila.jpa.repo.ChannelRepo;
import com.iceekb.dushnila.message.enums.AdminCommand;
import com.iceekb.dushnila.properties.BaseBotProperties;
import com.iceekb.dushnila.properties.LastMessageButton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class AdminService {

    private final ChannelRepo channelRepo;
    private final IgnoreRepo ignoreRepo;
    private final PointRepo pointRepo;
    private final ReactionRepo reactionRepo;

    public LastMessageButton onUpdate(Update update, BaseBotProperties properties) {
        LastMessageButton lastMessage = new LastMessageButton(update, properties);

        if (lastMessage.isValid()) {
            handleValidMessage(lastMessage, properties);
        }

        return lastMessage;
    }

    private void handleValidMessage(LastMessageButton lastMessage, BaseBotProperties properties) {
        var msg = lastMessage.getReceivedMessage();

        switch (getCommand(msg)) {
            case CHANNELS -> getChannelsList(lastMessage);
            case UPTIME -> getUptime(lastMessage, properties);
            case APPROVE -> getApprovedListByStatus(
                    lastMessage,
                    List.of(ChannelApproved.WAITING, ChannelApproved.REJECTED),
                    "Список не одобренных",
                    AdminCommand.APPROVE_ADD
            );
            case APPROVE_ADD -> addChannelToApproved(lastMessage, msg);
            case DAPPROVE -> getApprovedListByStatus(
                    lastMessage,
                    List.of(ChannelApproved.APPROVED),
                    "Список одобренных",
                    AdminCommand.DAPPROVE_ADD
            );
            case DAPPROVE_ADD -> deleteChannelFromApproved(lastMessage, msg);
            case DELETE_CHANNEL -> getApprovedListByStatus(
                    lastMessage,
                    List.of(ChannelApproved.REJECTED),
                    "Список отклонённых",
                    AdminCommand.DELETE_CHANNEL_ADD
            );
            case DELETE_CHANNEL_ADD -> deleteChannelFromDb(lastMessage, msg);
            default -> {
                // do nothing
            }
        }
    }

    private void deleteChannelFromDb(LastMessageButton lastMessage, String msg) {
        try {
            Long channelId = Long.parseLong(msg.split("#:#")[1]);
            Channel channel = channelRepo.findById(channelId).orElse(null);

            if (channel == null) {
                lastMessage.setError(true);
                return;
            }

            ignoreRepo.deleteAllByChannelId(channelId);
            reactionRepo.deleteAllByChannelId(channelId);
            pointRepo.deleteAllByChannelId(channelId);
            channelRepo.deleteById(channelId);

            lastMessage.setResponse("Канал удалён: " + channel.getChatName());
        } catch (NumberFormatException e) {
            log.error("Unexpected error during deleteChannelFromDb: {}", e.getMessage(), e);
        }
    }

    private void deleteChannelFromApproved(LastMessageButton lastMessage, String msg) {
        try {
            Long channelId = Long.parseLong(msg.split("#:#")[1]);
            Channel channel = channelRepo.findById(channelId).orElse(null);

            if (channel == null) {
                lastMessage.setError(true);
                return;
            }

            channel.setApproved(ChannelApproved.REJECTED);
            channelRepo.save(channel);
            lastMessage.setResponse("Канал отклонён: " + channel.getChatName());
        } catch (NumberFormatException e) {
            log.error("Unexpected error during deleteChannelFromApproved: {}", e.getMessage(), e);
        }
    }

    private void addChannelToApproved(LastMessageButton lastMessage, String msg) {
        Long channelId = Long.parseLong(msg.split("#:#")[1]);
        Channel channel = channelRepo.findById(channelId).orElse(null);

        if (channel == null) {
            lastMessage.setError(true);
            return;
        }

        channel.setApproved(ChannelApproved.APPROVED);
        channelRepo.save(channel);
        lastMessage.setResponse("Канал одобрен: " + channel.getChatName());
    }

    private void getApprovedListByStatus(LastMessageButton lastMessage, List<ChannelApproved> approvedList, String msg, AdminCommand command) {
        List<Channel> channels = channelRepo.findByApproved(approvedList);

        InlineKeyboardMarkup markupInline = channels.stream()
                .map(ch -> InlineKeyboardButton.builder()
                        .text(ch.getChatName())
                        .callbackData(command + "#:#" + ch.getId().toString())
                        .build())
                .map(InlineKeyboardRow::new)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        InlineKeyboardMarkup::new
                ));

        lastMessage.setMenu(markupInline);
        lastMessage.setResponse(msg + ":" + (markupInline.getKeyboard().isEmpty() ? " пусто" : StringUtils.EMPTY));
    }

    private void getUptime(LastMessageButton lastMessage, BaseBotProperties properties) {
        Duration uptime = Duration.between(properties.getStartTime(), LocalDateTime.now());
        String uptimeStr = setUptimeLine(uptime);
        lastMessage.setResponse(uptimeStr);
    }

    private String setUptimeLine(Duration uptime) {
        long days = uptime.toDaysPart();
        long hours = uptime.toHoursPart();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();

        return String.format("Bot uptime: %03d days %02d:%02d:%02d", days, hours, minutes, seconds);
    }

    private AdminCommand getCommand(String command) {
        try {
            return AdminCommand.valueOf(command.toUpperCase());
        } catch (IllegalArgumentException e) {
            try {
                List<String> commands = Arrays.asList(command.split("#:#"));
                return AdminCommand.valueOf(commands.get(0).toUpperCase());
            } catch (IllegalArgumentException e1) {
                log.error("Invalid admin command: {}", command);
                return AdminCommand.UNKNOWN;
            }
        }
    }

    private void getChannelsList(LastMessageButton lastMessage) {
        List<Channel> channels = channelRepo.findAll();

        String ch = channels.stream()
                .map(channel -> channel.getChatName() + " - " + channel.getApproved().getDesc())
                .collect(Collectors.joining("\n"));
        lastMessage.setResponse(ch);
    }
}
