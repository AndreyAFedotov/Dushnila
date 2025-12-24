package com.iceekb.dushnila.service;

import com.iceekb.dushnila.TestUtils;
import com.iceekb.dushnila.jpa.entity.Channel;
import com.iceekb.dushnila.jpa.repo.ChannelRepo;
import com.iceekb.dushnila.jpa.repo.IgnoreRepo;
import com.iceekb.dushnila.jpa.repo.PointRepo;
import com.iceekb.dushnila.jpa.repo.ReactionRepo;
import com.iceekb.dushnila.message.enums.AdminCommand;
import com.iceekb.dushnila.message.enums.ChannelApproved;
import com.iceekb.dushnila.properties.BaseBotProperties;
import com.iceekb.dushnila.properties.LastMessageButton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("SameParameterValue")
@ExtendWith(MockitoExtension.class)
class AdminServiceLogicTest extends TestUtils {

    @Mock private ChannelRepo channelRepo;
    @Mock private IgnoreRepo ignoreRepo;
    @Mock private PointRepo pointRepo;
    @Mock private ReactionRepo reactionRepo;

    private AdminService createService() {
        return new AdminService(channelRepo, ignoreRepo, pointRepo, reactionRepo);
    }

    private static Update updateWithCallbackData(long chatId, long userId, String data) {
        Update update = mock(Update.class);
        CallbackQuery query = mock(CallbackQuery.class);
        Message msg = mock(Message.class);
        User user = mock(User.class);

        when(update.getCallbackQuery()).thenReturn(query);
        when(query.getMessage()).thenReturn(msg);
        when(msg.getChatId()).thenReturn(chatId);
        when(query.getFrom()).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(query.getData()).thenReturn(data);

        return update;
    }

    @Test
    void onUpdate_unknownCommand_doesNothing() {
        AdminService service = createService();

        BaseBotProperties props = BaseBotProperties.builder().botAdmin("999").startTime(LocalDateTime.now()).build();
        Update upd = updateWithCallbackData(1L, 2L, "no_such_command");

        LastMessageButton lm = service.onUpdate(upd, props);
        assertTrue(lm.isValid());
        assertNull(lm.getResponse());
        assertNull(lm.getMenu());
    }

    @Test
    void onUpdate_channels_setsResponseWithAllChannels() {
        AdminService service = createService();

        when(channelRepo.findAll()).thenReturn(List.of(
                Channel.builder().chatName("A").approved(ChannelApproved.APPROVED).build(),
                Channel.builder().chatName("B").approved(ChannelApproved.REJECTED).build()
        ));

        BaseBotProperties props = BaseBotProperties.builder().botAdmin("999").startTime(LocalDateTime.now()).build();
        Update upd = updateWithCallbackData(1L, 2L, AdminCommand.CHANNELS.name());

        LastMessageButton lm = service.onUpdate(upd, props);
        assertNotNull(lm.getResponse());
        assertTrue(lm.getResponse().contains("A"));
        assertTrue(lm.getResponse().contains("B"));
    }

    @Test
    void onUpdate_uptime_setsResponse() {
        AdminService service = createService();

        BaseBotProperties props = BaseBotProperties.builder()
                .botAdmin("999")
                .startTime(LocalDateTime.now().minusSeconds(5))
                .build();
        Update upd = updateWithCallbackData(1L, 2L, AdminCommand.UPTIME.name());

        LastMessageButton lm = service.onUpdate(upd, props);
        assertNotNull(lm.getResponse());
        assertTrue(lm.getResponse().startsWith("Bot uptime:"));
    }

    @Test
    void onUpdate_approve_buildsKeyboard() {
        AdminService service = createService();

        Channel ch = Channel.builder().id(10L).chatName("Ch").approved(ChannelApproved.WAITING).build();
        when(channelRepo.findByApproved(List.of(ChannelApproved.WAITING, ChannelApproved.REJECTED))).thenReturn(List.of(ch));

        BaseBotProperties props = BaseBotProperties.builder().botAdmin("999").startTime(LocalDateTime.now()).build();
        Update upd = updateWithCallbackData(1L, 2L, AdminCommand.APPROVE.name());

        LastMessageButton lm = service.onUpdate(upd, props);
        assertNotNull(lm.getMenu());
        assertNotNull(lm.getResponse());
    }

    @Test
    void onUpdate_approveAdd_setsApprovedAndResponds() {
        AdminService service = createService();

        Channel ch = Channel.builder().id(10L).chatName("Ch").approved(ChannelApproved.WAITING).build();
        when(channelRepo.findById(10L)).thenReturn(Optional.of(ch));

        BaseBotProperties props = BaseBotProperties.builder().botAdmin("999").startTime(LocalDateTime.now()).build();
        Update upd = updateWithCallbackData(1L, 2L, AdminCommand.APPROVE_ADD + "#:#10");

        LastMessageButton lm = service.onUpdate(upd, props);
        assertEquals("Канал одобрен: Ch", lm.getResponse());
        assertEquals(ChannelApproved.APPROVED, ch.getApproved());
    }

    @Test
    void onUpdate_dapproveAdd_setsRejectedAndResponds() {
        AdminService service = createService();

        Channel ch = Channel.builder().id(11L).chatName("Ch2").approved(ChannelApproved.APPROVED).build();
        when(channelRepo.findById(11L)).thenReturn(Optional.of(ch));

        BaseBotProperties props = BaseBotProperties.builder().botAdmin("999").startTime(LocalDateTime.now()).build();
        Update upd = updateWithCallbackData(1L, 2L, AdminCommand.DAPPROVE_ADD + "#:#11");

        LastMessageButton lm = service.onUpdate(upd, props);
        assertEquals("Канал отклонён: Ch2", lm.getResponse());
        assertEquals(ChannelApproved.REJECTED, ch.getApproved());
    }

    @Test
    void onUpdate_deleteChannel_buildsKeyboardFromRejected() {
        AdminService service = createService();

        Channel ch = Channel.builder().id(12L).chatName("DelMe").approved(ChannelApproved.REJECTED).build();
        when(channelRepo.findByApproved(List.of(ChannelApproved.REJECTED))).thenReturn(List.of(ch));

        BaseBotProperties props = BaseBotProperties.builder().botAdmin("999").startTime(LocalDateTime.now()).build();
        Update upd = updateWithCallbackData(1L, 2L, AdminCommand.DELETE_CHANNEL.name());

        LastMessageButton lm = service.onUpdate(upd, props);
        assertNotNull(lm.getMenu());
        assertNotNull(lm.getResponse());
        assertTrue(lm.getResponse().contains("Список отклонённых"));
        // callbackData должно указывать на DELETE_CHANNEL_ADD#:#id
        assertTrue(
                lm.getMenu().getKeyboard().get(0).get(0).getCallbackData()
                        .contains(AdminCommand.DELETE_CHANNEL_ADD.name() + "#:#12")
        );
    }

    @Test
    void onUpdate_deleteChannelAdd_deletesChannelAndRelatedData() {
        AdminService service = createService();

        Channel ch = Channel.builder().id(13L).chatName("Gone").approved(ChannelApproved.REJECTED).build();
        when(channelRepo.findById(13L)).thenReturn(Optional.of(ch));

        BaseBotProperties props = BaseBotProperties.builder().botAdmin("999").startTime(LocalDateTime.now()).build();
        Update upd = updateWithCallbackData(1L, 2L, AdminCommand.DELETE_CHANNEL_ADD + "#:#13");

        LastMessageButton lm = service.onUpdate(upd, props);
        assertEquals("Канал удалён: Gone", lm.getResponse());

        verify(ignoreRepo).deleteAllByChannelId(13L);
        verify(reactionRepo).deleteAllByChannelId(13L);
        verify(pointRepo).deleteAllByChannelId(13L);
        verify(channelRepo).deleteById(13L);
    }
}


