package com.iceekb.dushnila.service;

import com.iceekb.dushnila.jpa.entity.Channel;
import com.iceekb.dushnila.jpa.entity.User;
import com.iceekb.dushnila.jpa.repo.ChannelRepo;
import com.iceekb.dushnila.jpa.repo.UserRepo;
import com.iceekb.dushnila.message.util.ServiceUtil;
import com.iceekb.dushnila.properties.LastMessageTxt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Регистрация канала и пользователя по входящему сообщению.
 * Выполняется в короткой транзакции — до любых сетевых вызовов.
 */
@Service
@RequiredArgsConstructor
public class AccessService {

    private final ChannelRepo channelRepo;
    private final UserRepo userRepo;

    @Transactional
    public void checkMessageAccess(LastMessageTxt lastMessage) {
        Channel channelResult = retrieveOrCreateChannel(lastMessage);
        channelRepo.save(channelResult);

        if (!lastMessage.isError()) {
            User user = retrieveOrCreateUser(lastMessage);
            userRepo.save(user);
        }
    }

    private Channel retrieveOrCreateChannel(LastMessageTxt lastMessage) {
        Long channelTgId = ServiceUtil.checkChannelTgId(lastMessage.getChannelTgId());
        Channel channel = channelRepo.findByTgId(channelTgId);
        return (channel == null) ? ServiceUtil.createNewChannel(lastMessage) : ServiceUtil.channelAnalysis(channel, lastMessage);
    }

    private User retrieveOrCreateUser(LastMessageTxt lastMessage) {
        User user = userRepo.findByTgId(lastMessage.getUserTgId());
        return (user == null) ? ServiceUtil.createNewUser(lastMessage) : ServiceUtil.userAnalysis(user, lastMessage);
    }
}
