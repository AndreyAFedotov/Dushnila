package com.iceekb.dushnila.service;

import com.iceekb.dushnila.jpa.entity.Channel;
import com.iceekb.dushnila.jpa.entity.Ignore;
import com.iceekb.dushnila.jpa.entity.Reaction;
import com.iceekb.dushnila.jpa.entity.User;
import com.iceekb.dushnila.jpa.repo.ChannelRepo;
import com.iceekb.dushnila.jpa.repo.IgnoreRepo;
import com.iceekb.dushnila.jpa.repo.PointRepo;
import com.iceekb.dushnila.jpa.repo.ReactionRepo;
import com.iceekb.dushnila.jpa.repo.UserRepo;
import com.iceekb.dushnila.message.enums.ChannelApproved;
import com.iceekb.dushnila.properties.BaseBotProperties;
import com.iceekb.dushnila.properties.LastMessageTxt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("SameParameterValue")
@ExtendWith(MockitoExtension.class)
class MessagesServiceCommandsTest {

    @Mock private ChannelRepo channelRepo;
    @Mock private UserRepo userRepo;
    @Mock private PointRepo pointRepo;
    @Mock private ReactionRepo reactionRepo;
    @Mock private IgnoreRepo ignoreRepo;
    @Mock private IgnoreFilterService ignoreFilterService;
    @Mock private ReactionService reactionService;
    @Mock private SpellerService spellerService;
    @Mock private AutoResponseService responseService;
    @Mock private PlatformTransactionManager transactionManager;

    private MessagesService service;
    private BaseBotProperties props;
    private Channel channel;
    private User user;

    @BeforeEach
    void setUp() {
        service = new MessagesService(
                channelRepo,
                userRepo,
                pointRepo,
                reactionRepo,
                ignoreRepo,
                ignoreFilterService,
                reactionService,
                spellerService,
                responseService,
                transactionManager
        );

        props = BaseBotProperties.builder()
                .botAdmin("999")
                .adminMail("admin@example.com")
                .startTime(LocalDateTime.now())
                .build();

        channel = Channel.builder()
                .id(1L)
                .tgId(2L)
                .chatName("Test Chat")
                .messageCount(0L)
                .approved(ChannelApproved.APPROVED)
                .firstMessage(LocalDateTime.now())
                .lastMessage(LocalDateTime.now())
                .build();

        user = User.builder()
                .id(10L)
                .tgId(20L)
                .nickName("bob")
                .firstMessage(LocalDateTime.now())
                .lastMessage(LocalDateTime.now())
                .build();

        when(channelRepo.findByTgId(anyLong())).thenReturn(channel);
        when(userRepo.findByTgId(anyLong())).thenReturn(user);
    }

    private static Update textUpdate(long chatId, long userId, String chatTitle, String text, int messageId) {
        Update update = mock(Update.class);
        Message msg = mock(Message.class);
        org.telegram.telegrambots.meta.api.objects.User from = mock(org.telegram.telegrambots.meta.api.objects.User.class);
        Chat chat = mock(Chat.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(msg);
        when(msg.hasText()).thenReturn(true);
        when(msg.getText()).thenReturn(text);
        when(msg.getChatId()).thenReturn(chatId);
        when(msg.getMessageId()).thenReturn(messageId);
        when(msg.getFrom()).thenReturn(from);
        when(msg.getChat()).thenReturn(chat);
        when(chat.getTitle()).thenReturn(chatTitle);
        when(from.getId()).thenReturn(userId);
        when(from.getUserName()).thenReturn("bob");
        when(from.getFirstName()).thenReturn("Bob");
        when(from.getLastName()).thenReturn("B.");

        return update;
    }

    @Test
    void cignore_addsIgnore() {
        when(ignoreRepo.findByWordAndChatId("hello", 1L)).thenReturn(null);

        Update upd = textUpdate(2L, 20L, "Test Chat", "/cignore \"hello\"", 1);
        LastMessageTxt lm = service.onUpdate(upd, props);

        assertEquals("Добавлено: hello", lm.getResponse());
        verify(ignoreRepo).save(any(Ignore.class));
        verify(ignoreFilterService, never()).filterMessage(anyLong(), any());
    }

    @Test
    void cignore_alreadyConfigured() {
        when(ignoreRepo.findByWordAndChatId("hello", 1L)).thenReturn(Ignore.builder().id(7L).word("hello").build());

        Update upd = textUpdate(2L, 20L, "Test Chat", "/cignore \"hello\"", 1);
        LastMessageTxt lm = service.onUpdate(upd, props);

        assertEquals("Уже настроено", lm.getResponse());
        verify(ignoreRepo, never()).save(any(Ignore.class));
    }

    @Test
    void dignore_notFound() {
        when(ignoreRepo.findByWordAndChatId("hello", 1L)).thenReturn(null);

        Update upd = textUpdate(2L, 20L, "Test Chat", "/dignore \"hello\"", 1);
        LastMessageTxt lm = service.onUpdate(upd, props);

        assertEquals("Не обнаружено", lm.getResponse());
        verify(ignoreRepo, never()).deleteById(anyLong());
    }

    @Test
    void dignore_deletes() {
        when(ignoreRepo.findByWordAndChatId("hello", 1L)).thenReturn(Ignore.builder().id(7L).word("hello").build());

        Update upd = textUpdate(2L, 20L, "Test Chat", "/dignore \"hello\"", 1);
        LastMessageTxt lm = service.onUpdate(upd, props);

        assertEquals("Удалено: hello", lm.getResponse());
        verify(ignoreRepo).deleteById(7L);
    }

    @Test
    void lignore_empty() {
        when(ignoreRepo.findAllByChatId(1L)).thenReturn(List.of());

        Update upd = textUpdate(2L, 20L, "Test Chat", "/lignore \"x\"", 1);
        LastMessageTxt lm = service.onUpdate(upd, props);

        assertEquals("Список пуст", lm.getResponse());
    }

    @Test
    void lignore_lists() {
        when(ignoreRepo.findAllByChatId(1L)).thenReturn(List.of(
                Ignore.builder().word("a").build(),
                Ignore.builder().word("b").build()
        ));

        Update upd = textUpdate(2L, 20L, "Test Chat", "/lignore \"x\"", 1);
        LastMessageTxt lm = service.onUpdate(upd, props);

        assertNotNull(lm.getResponse());
        assertTrue(lm.getResponse().contains("Игнор"));
        assertTrue(lm.getResponse().contains("a"));
        assertTrue(lm.getResponse().contains("b"));
    }

    @Test
    void creplace_createsNew() {
        when(reactionRepo.findByTextFromAndChanelId("from", 1L)).thenReturn(null);

        Update upd = textUpdate(2L, 20L, "Test Chat", "/creplace \"from\" \"to\"", 1);
        LastMessageTxt lm = service.onUpdate(upd, props);

        assertNotNull(lm.getResponse());
        assertTrue(lm.getResponse().startsWith("Добавлена замена:"));
        verify(reactionRepo).save(any(Reaction.class));
        verify(reactionService, never()).applyReaction(any());
        verify(spellerService, never()).speller(any());
    }

    @Test
    void creplace_updatesExisting() {
        Reaction existing = Reaction.builder().id(5L).channel(channel).user(user).textFrom("from").textTo("old").build();
        when(reactionRepo.findByTextFromAndChanelId("from", 1L)).thenReturn(existing);

        Update upd = textUpdate(2L, 20L, "Test Chat", "/creplace \"from\" \"new\"", 1);
        LastMessageTxt lm = service.onUpdate(upd, props);

        assertEquals("Обновлена замена: \"from\" на \"new\"", lm.getResponse());
        assertEquals("new", existing.getTextTo());
        verify(reactionRepo).save(existing);
    }

    @Test
    void dreplace_notFound() {
        when(reactionRepo.findByTextFromAndChanelId("from", 1L)).thenReturn(null);

        Update upd = textUpdate(2L, 20L, "Test Chat", "/dreplace \"from\"", 1);
        LastMessageTxt lm = service.onUpdate(upd, props);

        assertEquals("Не обнаружено", lm.getResponse());
        verify(reactionRepo, never()).deleteById(anyLong());
    }

    @Test
    void dreplace_deletes() {
        when(reactionRepo.findByTextFromAndChanelId("from", 1L)).thenReturn(Reaction.builder().id(5L).textFrom("from").textTo("to").build());

        Update upd = textUpdate(2L, 20L, "Test Chat", "/dreplace \"from\"", 1);
        LastMessageTxt lm = service.onUpdate(upd, props);

        assertEquals("Удалено", lm.getResponse());
        verify(reactionRepo).deleteById(5L);
    }

    @Test
    void lreplace_empty() {
        when(reactionRepo.findAllByCatTgId(2L)).thenReturn(List.of());

        Update upd = textUpdate(2L, 20L, "Test Chat", "/lreplace \"x\"", 1);
        LastMessageTxt lm = service.onUpdate(upd, props);

        assertEquals("Список пуст", lm.getResponse());
    }

    @Test
    void lreplace_lists() {
        when(reactionRepo.findAllByCatTgId(2L)).thenReturn(List.of(
                Reaction.builder().textFrom("a").textTo("A").build(),
                Reaction.builder().textFrom("b").textTo("B").build()
        ));

        Update upd = textUpdate(2L, 20L, "Test Chat", "/lreplace \"x\"", 1);
        LastMessageTxt lm = service.onUpdate(upd, props);

        assertNotNull(lm.getResponse());
        assertTrue(lm.getResponse().contains("Пары"));
        assertTrue(lm.getResponse().contains("a -> A"));
        assertTrue(lm.getResponse().contains("b -> B"));
    }
}


