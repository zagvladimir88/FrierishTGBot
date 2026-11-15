package com.zagvladimir.fraerish;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import com.zagvladimir.fraerish.subscription.ChatRepository;
import com.zagvladimir.fraerish.subscription.StartHandler;
import com.zagvladimir.fraerish.subscription.StopHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@SpringBootTest(properties = {
        "bot.token=test-token",
        "bot.name=TestBot",
        "bot.polling-enabled=false",
        "integration.open-weather.api-key=test",
        "spring.datasource.url=jdbc:h2:mem:fraerish;DB_CLOSE_DELAY=-1"
})
class FraerishBotApplicationTest {

    @Autowired
    private ApplicationContext context;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private StartHandler startHandler;
    @Autowired
    private StopHandler stopHandler;
    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void registersEveryCommandHandler() {
        assertThat(context.getBeansOfType(CommandHandler.class).values())
                .extracting(CommandHandler::command)
                .containsExactlyInAnyOrder("/start", "/stop", "/w", "/exchange", "/g", "/c");
    }

    @Test
    void startAndStopManageDailyDigestSubscription() {
        BotReply help = startHandler.handle(new CommandRequest(100L, ""));
        startHandler.handle(new CommandRequest(100L, ""));
        startHandler.handle(new CommandRequest(200L, ""));

        assertThat(help).isInstanceOf(BotReply.Text.class);
        assertThat(chatRepository.findActiveChatIds()).containsExactly(100L, 200L);
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM chats WHERE chat_id = 100").query(Long.class).single()).isEqualTo(1L);

        stopHandler.handle(new CommandRequest(100L, ""));
        assertThat(chatRepository.findActiveChatIds()).containsExactly(200L);

        startHandler.handle(new CommandRequest(100L, ""));
        assertThat(chatRepository.findActiveChatIds()).containsExactly(100L, 200L);
    }
}