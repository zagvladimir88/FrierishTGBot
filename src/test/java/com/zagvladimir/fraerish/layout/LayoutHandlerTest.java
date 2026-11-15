package com.zagvladimir.fraerish.layout;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.ChatContext;
import com.zagvladimir.fraerish.command.CommandRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class LayoutHandlerTest {

    private final LayoutHandler handler = new LayoutHandler();

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "ghbdtn|привет",
            "Ghbdtn? rfr ltkf&|Привет, как дела?",
            ";bpym|жизнь",
            "`krf|ёлка",
            "cgfcb,j|спасибо",
            "j,]tvysq|объемный",
            "ckjdj 2026|слово 2026",
            "уже по-русски|уже по-русски"
    })
    void convertsLatinLayoutToCyrillic(String typed, String expected) {
        assertThat(handler.handle(new CommandRequest(1, typed))).isEqualTo(BotReply.text(expected));
    }

    @Test
    void asksForTextWhenArgumentsAreMissing() {
        assertThat(handler.handle(new CommandRequest(1, ""))).isEqualTo(BotReply.text(LayoutHandler.USAGE));
        assertThat(handler.handle(new CommandRequest(ChatContext.privateChat(1), "", "  "))).isEqualTo(BotReply.text(LayoutHandler.USAGE));
    }

    @Test
    void convertsRepliedMessageWhenCommandHasNoText() {
        assertThat(handler.handle(new CommandRequest(ChatContext.privateChat(1), "", "ghbdtn? rfr ltkf&")))
                .isEqualTo(BotReply.text("привет, как дела?"));
    }

    @Test
    void prefersCommandTextOverRepliedMessage() {
        assertThat(handler.handle(new CommandRequest(ChatContext.privateChat(1), "ckjdj", "ghbdtn")))
                .isEqualTo(BotReply.text("слово"));
    }

    @Test
    void acceptsEditedMessages() {
        assertThat(handler.acceptsEditedMessages()).isTrue();
    }
}