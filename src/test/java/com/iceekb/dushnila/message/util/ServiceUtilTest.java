package com.iceekb.dushnila.message.util;

import com.iceekb.dushnila.TestUtils;
import com.iceekb.dushnila.jpa.entity.Channel;
import com.iceekb.dushnila.jpa.entity.Ignore;
import com.iceekb.dushnila.jpa.entity.Point;
import com.iceekb.dushnila.jpa.entity.Reaction;
import com.iceekb.dushnila.jpa.entity.User;
import com.iceekb.dushnila.jpa.enums.ChannelApproved;
import com.iceekb.dushnila.message.enums.ChatCommand;
import com.iceekb.dushnila.message.enums.MessageValidationError;
import com.iceekb.dushnila.properties.LastMessageTxt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ServiceUtilTest extends TestUtils {

    @ParameterizedTest
    @CsvSource({
            "ChannelChatName, MessageChannelName, APPROVED",
            "ChannelChatName, MessageChannelName, REJECTED",
            "ChannelChatName, MessageChannelName, WAITING"
    })
    void channelAnalysisTest(String channelName, String messageChannelName, ChannelApproved approved) {
        Channel channel = createChanel();
        LastMessageTxt lastMessage = createMessage();
        channel.setChatName(channelName);
        channel.setApproved(approved);
        lastMessage.setChannelName(messageChannelName);

        ServiceUtil.channelAnalysis(channel, lastMessage);

        if (channel.getApproved() == ChannelApproved.REJECTED || channel.getApproved() == ChannelApproved.WAITING) {
            assertEquals(channel.getApproved(), approved);
            assertTrue(lastMessage.isError());
            assertEquals(1, lastMessage.getValidationErrors().size());
            assertEquals(MessageValidationError.CHANNEL_NOT_APPROVED, lastMessage.getValidationErrors().get(0));
        }
        assertEquals(channel.getChatName(), lastMessage.getChannelName());
        assertEquals(1, channel.getMessageCount());
        assertEquals(LocalDate.now(), channel.getLastMessage().toLocalDate());
    }

    @Test
    void userAnalysisTest() {
        User user = createUser();
        LastMessageTxt lastMessage = createMessage();
        lastMessage.setUser(null);
        ServiceUtil.userAnalysis(user, lastMessage);

        assertEquals(user, lastMessage.getUser());
        assertEquals(LocalDate.now(), user.getLastMessage().toLocalDate());
        assertEquals(lastMessage.getUserName(), user.getNickName());

    }

    @Test
    void createNewChannelTest() {
        LastMessageTxt lastMessage = createMessage();
        lastMessage.setChannel(null);
        Channel channel = ServiceUtil.createNewChannel(lastMessage);

        assertNotNull(channel.getTgId());
        assertEquals(lastMessage.getChannelName(), channel.getChatName());
        assertEquals(1L, channel.getMessageCount());
        assertEquals(LocalDate.now(), channel.getLastMessage().toLocalDate());
        assertEquals(LocalDate.now(), channel.getFirstMessage().toLocalDate());
        assertEquals(ChannelApproved.WAITING, channel.getApproved());
        assertEquals(channel, lastMessage.getChannel());
        assertEquals(MessageValidationError.CHANNEL_NOT_APPROVED, lastMessage.getValidationErrors().get(0));
        assertTrue(lastMessage.isError());
    }

    @Test
    void createNewUserTest() {
        LastMessageTxt lastMessage = createMessage();
        lastMessage.setUser(null);
        User user = ServiceUtil.createNewUser(lastMessage);

        assertNotNull(user);
        assertEquals(lastMessage.getUser(), user);
        assertEquals(lastMessage.getUserTgId(), user.getTgId());
        assertEquals(lastMessage.getUserName(), user.getNickName());
        assertEquals(LocalDate.now(), user.getLastMessage().toLocalDate());
        assertEquals(LocalDate.now(), user.getFirstMessage().toLocalDate());
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0",
            "-10010000, 10000",
            "10010000, 10010000"
    })
    void checkChannelTgIdTest(Long incoming, Long real) {
        if (incoming == 0L) incoming = null;
        if (real == 0L) real = null;

        Long result = ServiceUtil.checkChannelTgId(incoming);
        assertEquals(real, result);
    }

    @ParameterizedTest
    @CsvSource({
            "one, two",
            "111, 222"
    })
    void createNewReactionTest(String from, String to) {
        Map<String, String> data = Map.of("from", from, "to", to);
        LastMessageTxt lastMessage = createMessage();

        Reaction reaction = ServiceUtil.createNewReaction(lastMessage, data);
        assertNotNull(reaction);
        assertEquals(reaction.getTextFrom(), from);
        assertEquals(reaction.getTextTo(), to);
        assertEquals(reaction.getChannel(), lastMessage.getChannel());
        assertEquals(reaction.getUser(), lastMessage.getUser());
        assertEquals(reaction.getCreatedOn().toLocalDate(), LocalDate.now());
        assertTrue(lastMessage.getResponse().contains("Добавлена замена"));
    }

    @ParameterizedTest
    @CsvSource({
            "/creplace test string, CREPLACE, false",
            "/dreplace test string, DREPLACE, false",
            "/stat test string, STAT, false",
            "/help test string, HELP, false",
            "/cignore test string, CIGNORE, false",
            "/dignore test string, DIGNORE, false",
            "/lignore test string, LIGNORE, false",
            "/lreplace test string, LREPLACE, false",
            "/lreplaceErr test string, LREPLACE, true",
            "test, CREPLACE, true"
    })
    void getCommandTest(String message, ChatCommand command, Boolean isUnknown) {
        LastMessageTxt lastMessage = createMessage();
        lastMessage.setReceivedMessage(message);
        ChatCommand chatCommand = ServiceUtil.getCommand(lastMessage);

        if (isUnknown) {
            assertNull(chatCommand);
        } else {
            assertNotNull(chatCommand);
            assertEquals(command, chatCommand);
        }
    }

    @Test
    void createNewPointTest() {
        LastMessageTxt lastMessage = createMessage();
        Point point = ServiceUtil.createNewPoint(lastMessage);

        assertNotNull(point);
        assertEquals(lastMessage.getChannel(), point.getChannel());
        assertEquals(lastMessage.getUser(), point.getUser());
        assertEquals(1L, point.getPointCount());
    }

    @Test
    void createNewIgnoreTest() {
        LastMessageTxt lastMessage = createMessage();
        Map<String, String> data = Map.of("param", "test");
        Ignore ignore = ServiceUtil.createNewIgnore(lastMessage, data);

        assertNotNull(ignore);
        assertEquals(lastMessage.getChannel(), ignore.getChannel());
        assertEquals(lastMessage.getUser(), ignore.getUser());
        assertEquals("test", ignore.getWord());
        assertEquals(LocalDate.now(), ignore.getCreatedOn().toLocalDate());
    }

}