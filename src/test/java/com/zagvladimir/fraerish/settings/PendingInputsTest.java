package com.zagvladimir.fraerish.settings;

import com.zagvladimir.fraerish.command.ChatContext;
import com.zagvladimir.fraerish.settings.SettingsMenu.PromptKind;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class PendingInputsTest {

    private static final ChatContext ALICE = new ChatContext(-100, 1, true, false);
    private static final ChatContext BOB = new ChatContext(-100, 2, true, false);

    private final MovableClock clock = new MovableClock(Instant.parse("2026-11-16T08:00:00Z"));
    private final PendingInputs pendingInputs = new PendingInputs(clock);

    @Test
    void waitsForOneMessageFromTheSameUser() {
        pendingInputs.await(ALICE, PromptKind.CITIES);

        assertThat(pendingInputs.take(BOB)).isEmpty();
        assertThat(pendingInputs.take(ALICE)).contains(PromptKind.CITIES);
        assertThat(pendingInputs.take(ALICE)).isEmpty();
    }

    @Test
    void latestQuestionWins() {
        pendingInputs.await(ALICE, PromptKind.CITIES);
        pendingInputs.await(ALICE, PromptKind.CURRENCIES);

        assertThat(pendingInputs.take(ALICE)).contains(PromptKind.CURRENCIES);
    }

    @Test
    void forgetsQuestionAfterTimeout() {
        pendingInputs.await(ALICE, PromptKind.CITIES);
        clock.instant = clock.instant.plus(PendingInputs.WAIT_FOR).plusSeconds(1);

        assertThat(pendingInputs.take(ALICE)).isEmpty();
    }

    private static final class MovableClock extends Clock {

        private Instant instant;

        private MovableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}