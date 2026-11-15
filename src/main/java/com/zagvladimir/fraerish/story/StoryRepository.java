package com.zagvladimir.fraerish.story;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StoryRepository {

    static final int NO_ANSWER = -1;
    private static final String OPTION_SEPARATOR = "\n";

    private final JdbcClient jdbcClient;

    public Optional<StoryGame> findOpenGame(long chatId) {
        return jdbcClient.sql("SELECT * FROM story_games WHERE chat_id = :chatId AND answer_index IS NULL ORDER BY id DESC LIMIT 1")
                .param("chatId", chatId)
                .query(StoryRepository::map)
                .optional();
    }

    public Optional<StoryGame> findByPollId(String pollId) {
        return jdbcClient.sql("SELECT * FROM story_games WHERE poll_id = :pollId")
                .param("pollId", pollId)
                .query(StoryRepository::map)
                .optional();
    }

    public void createGame(long chatId, int messageId, String pollId, List<String> options) {
        jdbcClient.sql("INSERT INTO story_games (chat_id, message_id, poll_id, options) VALUES (:chatId, :messageId, :pollId, :options)")
                .param("chatId", chatId)
                .param("messageId", messageId)
                .param("pollId", pollId)
                .param("options", String.join(OPTION_SEPARATOR, options))
                .update();
    }

    public void closeGame(long gameId, int answerIndex) {
        jdbcClient.sql("UPDATE story_games SET answer_index = :answer, closed_at = CURRENT_TIMESTAMP WHERE id = :id")
                .param("answer", answerIndex)
                .param("id", gameId)
                .update();
    }

    @Transactional
    public void saveVote(long gameId, long userId, String userName, int optionIndex) {
        removeVote(gameId, userId);
        jdbcClient.sql("INSERT INTO story_votes (game_id, user_id, user_name, option_index) VALUES (:gameId, :userId, :name, :option)")
                .param("gameId", gameId)
                .param("userId", userId)
                .param("name", userName)
                .param("option", optionIndex)
                .update();
    }

    public void removeVote(long gameId, long userId) {
        jdbcClient.sql("DELETE FROM story_votes WHERE game_id = :gameId AND user_id = :userId")
                .param("gameId", gameId)
                .param("userId", userId)
                .update();
    }

    public List<String> votersFor(long gameId, int optionIndex) {
        return jdbcClient.sql("SELECT user_name FROM story_votes WHERE game_id = :gameId AND option_index = :option ORDER BY user_name")
                .param("gameId", gameId)
                .param("option", optionIndex)
                .query(String.class)
                .list();
    }

    public List<StoryScore> leaderboard(long chatId, int limit) {
        return jdbcClient.sql("""
                        SELECT MAX(v.user_name) AS name,
                               SUM(CASE WHEN v.option_index = g.answer_index THEN 1 ELSE 0 END) AS correct,
                               COUNT(*) AS played
                        FROM story_votes v JOIN story_games g ON g.id = v.game_id
                        WHERE g.chat_id = :chatId AND g.answer_index >= 0
                        GROUP BY v.user_id
                        ORDER BY correct DESC, played, name
                        LIMIT :limit""")
                .param("chatId", chatId)
                .param("limit", limit)
                .query((row, number) -> new StoryScore(row.getString("name"), row.getLong("correct"), row.getLong("played")))
                .list();
    }

    public Optional<List<String>> options(long chatId) {
        return jdbcClient.sql("SELECT options FROM story_options WHERE chat_id = :chatId")
                .param("chatId", chatId)
                .query(String.class)
                .optional()
                .map(StoryRepository::split);
    }

    @Transactional
    public void saveOptions(long chatId, List<String> options) {
        jdbcClient.sql("DELETE FROM story_options WHERE chat_id = :chatId").param("chatId", chatId).update();
        jdbcClient.sql("INSERT INTO story_options (chat_id, options) VALUES (:chatId, :options)")
                .param("chatId", chatId)
                .param("options", String.join(OPTION_SEPARATOR, options))
                .update();
    }

    public Optional<Integer> players(long chatId) {
        return jdbcClient.sql("SELECT players FROM story_players WHERE chat_id = :chatId")
                .param("chatId", chatId)
                .query(Integer.class)
                .optional();
    }

    @Transactional
    public void savePlayers(long chatId, Integer players) {
        jdbcClient.sql("DELETE FROM story_players WHERE chat_id = :chatId").param("chatId", chatId).update();
        if (players != null) {
            jdbcClient.sql("INSERT INTO story_players (chat_id, players) VALUES (:chatId, :players)")
                    .param("chatId", chatId)
                    .param("players", players)
                    .update();
        }
    }

    public int countVotes(long gameId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM story_votes WHERE game_id = :gameId")
                .param("gameId", gameId)
                .query(Integer.class)
                .single();
    }

    public boolean hasVoted(long gameId, long userId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM story_votes WHERE game_id = :gameId AND user_id = :userId")
                .param("gameId", gameId)
                .param("userId", userId)
                .query(Integer.class)
                .single() > 0;
    }

    public void startConfirmation(long gameId, int confirmMessageId, int voters) {
        jdbcClient.sql("UPDATE story_games SET confirm_message_id = :messageId, voters_snapshot = :voters WHERE id = :id")
                .param("messageId", confirmMessageId)
                .param("voters", voters)
                .param("id", gameId)
                .update();
    }

    @Transactional
    public void saveConfirmation(long gameId, long userId, int optionIndex) {
        jdbcClient.sql("DELETE FROM story_confirmations WHERE game_id = :gameId AND user_id = :userId")
                .param("gameId", gameId)
                .param("userId", userId)
                .update();
        jdbcClient.sql("INSERT INTO story_confirmations (game_id, user_id, option_index) VALUES (:gameId, :userId, :option)")
                .param("gameId", gameId)
                .param("userId", userId)
                .param("option", optionIndex)
                .update();
    }

    public Map<Integer, Long> confirmationCounts(long gameId) {
        return jdbcClient.sql("SELECT option_index, COUNT(*) AS total FROM story_confirmations WHERE game_id = :gameId GROUP BY option_index")
                .param("gameId", gameId)
                .query((row, number) -> Map.entry(row.getInt("option_index"), row.getLong("total")))
                .list()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static StoryGame map(ResultSet row, int number) throws SQLException {
        return new StoryGame(
                row.getLong("id"),
                row.getLong("chat_id"),
                row.getInt("message_id"),
                row.getString("poll_id"),
                split(row.getString("options")),
                row.getObject("answer_index", Integer.class),
                row.getObject("confirm_message_id", Integer.class),
                row.getObject("voters_snapshot", Integer.class));
    }

    private static List<String> split(String options) {
        return Arrays.asList(options.split(OPTION_SEPARATOR));
    }
}