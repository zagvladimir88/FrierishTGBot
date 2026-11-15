package com.zagvladimir.fraerish.fun;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PickAndPollHandlerTest {

    private final PollHandler pollHandler = new PollHandler();

    @Test
    void picksOneOfChoicesSeparatedByCommasOrWordOr() {
        PickHandler handler = new PickHandler(fixedIndex(1));

        assertThat(handler.handle(new CommandRequest(1, "бар, кино, дома"))).isEqualTo(BotReply.text("🎲 Выбираю: кино"));
        assertThat(handler.handle(new CommandRequest(1, "пицца или суши"))).isEqualTo(BotReply.text("🎲 Выбираю: суши"));
    }

    @Test
    void needsAtLeastTwoDifferentChoices() {
        PickHandler handler = new PickHandler(fixedIndex(0));

        assertThat(handler.handle(new CommandRequest(1, ""))).isEqualTo(BotReply.text(PickHandler.USAGE));
        assertThat(handler.handle(new CommandRequest(1, "бар, Бар"))).isEqualTo(BotReply.text(PickHandler.USAGE));
    }

    @Test
    void buildsPollFromQuestionAndOptions() {
        assertThat(pollHandler.handle(new CommandRequest(1, "Куда идём? Бар, Кино, Дома")))
                .isEqualTo(new BotReply.Poll("Куда идём?", List.of("Бар", "Кино", "Дома")));
        assertThat(pollHandler.handle(new CommandRequest(1, "Ужин, пицца, суши")))
                .isEqualTo(new BotReply.Poll("Ужин", List.of("пицца", "суши")));
    }

    @Test
    void rejectsPollsWithoutEnoughOrWithTooManyOptions() {
        assertThat(pollHandler.handle(new CommandRequest(1, "Куда идём? Бар"))).isEqualTo(BotReply.text(PollHandler.USAGE));
        assertThat(pollHandler.handle(new CommandRequest(1, ""))).isEqualTo(BotReply.text(PollHandler.USAGE));
        String thirteen = String.join(", ", IntStream.rangeClosed(1, 13).mapToObj(i -> "вариант " + i).toList());
        assertThat(pollHandler.handle(new CommandRequest(1, "Что? " + thirteen)))
                .isEqualTo(BotReply.text("В опросе может быть не больше 12 вариантов"));
    }

    private static RandomGenerator fixedIndex(int index) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return index;
            }

            @Override
            public int nextInt(int bound) {
                return index;
            }
        };
    }
}