package com.zagvladimir.fraerish.story;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.ChatContext;
import com.zagvladimir.fraerish.settings.SettingsAccess;
import com.zagvladimir.fraerish.telegram.BotSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class StoryService {

    static final String QUESTION = "📸 Что в новой сторис?";
    static final List<String> DEFAULT_OPTIONS = List.of("Пейзаж", "Звёздное небо", "Качалка", "Селфи", "Дорожка в качалке", "Рилс");
    static final int MAX_OPTIONS = 12;
    static final int TOP_SIZE = 10;
    private static final List<String> MEDALS = List.of("🥇", "🥈", "🥉");

    private final StoryRepository repository;
    private final BotSender botSender;
    private final SettingsAccess settingsAccess;

    public BotReply start(long chatId) {
        if (repository.findOpenGame(chatId).isPresent()) {
            return BotReply.text("Угадайка уже идёт — когда узнаете ответ, напишите /answer и вариант");
        }
        List<String> options = options(chatId);
        return botSender.sendTrackedPoll(chatId, new BotReply.Poll(QUESTION, options))
                .map(poll -> {
                    repository.createGame(chatId, poll.messageId(), poll.pollId(), options);
                    return BotReply.none();
                })
                .orElseGet(() -> BotReply.text("Не получилось создать опрос, попробуйте ещё раз"));
    }

    public BotReply answer(long chatId, String answer) {
        Optional<StoryGame> game = repository.findOpenGame(chatId);
        if (game.isEmpty()) {
            return BotReply.text("Сейчас нет открытой угадайки. Начать: /story");
        }
        List<String> options = game.get().options();
        if (answer.isBlank()) {
            return BotReply.text("Напишите, что в сторис: /answer " + options.getFirst().toLowerCase(Locale.ROOT)
                    + "\nВарианты: " + String.join(", ", options));
        }
        Optional<Integer> index = match(options, answer.strip());
        if (index.isEmpty()) {
            return BotReply.text("Нет такого варианта. Варианты: " + String.join(", ", options));
        }
        botSender.stopPoll(chatId, game.get().messageId());
        repository.closeGame(game.get().id(), index.get());
        List<String> winners = repository.votersFor(game.get().id(), index.get());
        String result = winners.isEmpty() ? "😅 Никто не угадал" : "🎯 Угадали: " + String.join(", ", winners);
        return BotReply.text("✅ В сторис: %s\n%s".formatted(options.get(index.get()), result));
    }

    public void vote(String pollId, long userId, String userName, List<Integer> optionIds) {
        repository.findByPollId(pollId).filter(StoryGame::open).ifPresent(game -> {
            if (optionIds == null || optionIds.isEmpty()) {
                repository.removeVote(game.id(), userId);
            } else {
                repository.saveVote(game.id(), userId, userName, optionIds.getFirst());
            }
        });
    }

    public BotReply top(long chatId) {
        List<StoryScore> scores = repository.leaderboard(chatId, TOP_SIZE);
        if (scores.isEmpty()) {
            return BotReply.text("Пока не сыграли ни одной угадайки. Начать: /story");
        }
        List<String> lines = new ArrayList<>();
        lines.add("🏆 Лучше всех угадывают сторис");
        for (int i = 0; i < scores.size(); i++) {
            String place = i < MEDALS.size() ? MEDALS.get(i) : (i + 1) + ".";
            StoryScore score = scores.get(i);
            lines.add("%s %s — %d из %d".formatted(place, score.name(), score.correct(), score.played()));
        }
        return BotReply.text(String.join("\n", lines));
    }

    public BotReply changeOptions(ChatContext context, List<String> options) {
        if (!settingsAccess.canConfigure(context)) {
            return BotReply.text("Варианты угадайки меняют только админы группы");
        }
        if (options.size() < 2 || options.size() > MAX_OPTIONS) {
            return BotReply.text("Нужно от 2 до %d вариантов через запятую. Сейчас: %s"
                    .formatted(MAX_OPTIONS, String.join(", ", options(context.chatId()))));
        }
        repository.saveOptions(context.chatId(), options);
        return BotReply.text("Варианты для /story: " + String.join(", ", options));
    }

    private List<String> options(long chatId) {
        return repository.options(chatId).orElse(DEFAULT_OPTIONS);
    }

    private static Optional<Integer> match(List<String> options, String answer) {
        String normalized = normalize(answer);
        Optional<Integer> exact = IntStream.range(0, options.size()).boxed()
                .filter(i -> normalize(options.get(i)).equals(normalized))
                .findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        List<Integer> partial = IntStream.range(0, options.size()).boxed()
                .filter(i -> normalize(options.get(i)).startsWith(normalized))
                .toList();
        return partial.size() == 1 ? Optional.of(partial.getFirst()) : Optional.empty();
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace('ё', 'е').strip();
    }
}