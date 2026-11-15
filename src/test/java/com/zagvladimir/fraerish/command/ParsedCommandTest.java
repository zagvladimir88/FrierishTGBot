package com.zagvladimir.fraerish.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ParsedCommandTest {

    private static final String BOT_NAME = "FraerishBot";

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "/w Минск|/w|Минск",
            "/w   Нижний Новгород  |/w|Нижний Новгород",
            "/start|/start|''",
            "/W Лондон|/w|Лондон",
            "/w@FraerishBot Минск|/w|Минск",
            "/w@fraerishbot Минск|/w|Минск",
            "/exchange USD EUR|/exchange|USD EUR"
    })
    void parsesCommandAndArguments(String text, String expectedName, String expectedArgs) {
        assertThat(ParsedCommand.parse(text, BOT_NAME)).contains(new ParsedCommand(expectedName, expectedArgs));
    }

    @Test
    void keepsLineBreaksInsideArguments() {
        assertThat(ParsedCommand.parse("/c\nghbdtn\nvbh", BOT_NAME)).contains(new ParsedCommand("/c", "ghbdtn\nvbh"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "просто текст", "w Минск", "/w@OtherBot Минск"})
    void ignoresNonCommandsAndCommandsForOtherBots(String text) {
        assertThat(ParsedCommand.parse(text, BOT_NAME)).isEmpty();
    }
}