package com.zagvladimir.fraerish.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandRequestTest {

    @Test
    void firstArgIgnoresTheRest() {
        assertThat(new CommandRequest(1, "USD   EUR").firstArg()).isEqualTo("USD");
    }

    @Test
    void blankArgsMeanNoArgs() {
        assertThat(new CommandRequest(1, "").hasArgs()).isFalse();
        assertThat(new CommandRequest(1, "USD").hasArgs()).isTrue();
    }
}