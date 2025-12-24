package com.iceekb.dushnila.service;

import com.iceekb.dushnila.TestUtils;
import com.iceekb.dushnila.jpa.entity.Reaction;
import com.iceekb.dushnila.jpa.repo.ReactionRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest extends TestUtils {

    @Mock private ReactionRepo reactionRepo;

    private ReactionService createService() {
        return new ReactionService(reactionRepo);
    }

    @Test
    void phraseOverridesWord() {
        ReactionService service = createService();

        Reaction phrase = Reaction.builder().textFrom("Очень плохо").textTo("PHRASE").build();
        Reaction word = Reaction.builder().textFrom("плохо").textTo("WORD").build();
        when(reactionRepo.findAllByChannelId(1L)).thenReturn(List.of(phrase, word));

        var msg = createMessage();
        msg.getChannel().setId(1L);
        msg.setReceivedMessage("Это ОЧЕНЬ плохо!!");
        msg.setResponse(null);

        service.applyReaction(msg);

        assertEquals("PHRASE", msg.getResponse());
    }

    @Test
    void phraseWithHyphenOrDashMatches_prefersMoreSpecific() {
        ReactionService service = createService();

        Reaction phrasePlain = Reaction.builder().textFrom("мама анархия").textTo("PLAIN").build();
        Reaction phraseDash = Reaction.builder().textFrom("мама - анархия").textTo("DASH").build();
        when(reactionRepo.findAllByChannelId(1L)).thenReturn(List.of(phrasePlain, phraseDash));

        var msg1 = createMessage();
        msg1.getChannel().setId(1L);
        msg1.setReceivedMessage("Мама-анархия");
        msg1.setResponse(null);
        service.applyReaction(msg1);
        assertEquals("DASH", msg1.getResponse());

        var msg2 = createMessage();
        msg2.getChannel().setId(1L);
        msg2.setReceivedMessage("мама — анархия"); // em-dash
        msg2.setResponse(null);
        service.applyReaction(msg2);
        assertEquals("DASH", msg2.getResponse());

        var msg3 = createMessage();
        msg3.getChannel().setId(1L);
        msg3.setReceivedMessage("мама анархия");
        msg3.setResponse(null);
        service.applyReaction(msg3);
        assertEquals("PLAIN", msg3.getResponse());
    }

    @Test
    void wordBoundaryDoesNotMatchSubword() {
        ReactionService service = createService();

        Reaction cat = Reaction.builder().textFrom("кот").textTo("CAT").build();
        when(reactionRepo.findAllByChannelId(1L)).thenReturn(List.of(cat));

        var msg1 = createMessage();
        msg1.getChannel().setId(1L);
        msg1.setReceivedMessage("котик");
        msg1.setResponse(null);
        service.applyReaction(msg1);
        assertNull(msg1.getResponse());

        var msg2 = createMessage();
        msg2.getChannel().setId(1L);
        msg2.setReceivedMessage("котик кот");
        msg2.setResponse(null);
        service.applyReaction(msg2);
        assertEquals("CAT", msg2.getResponse());
    }
}


