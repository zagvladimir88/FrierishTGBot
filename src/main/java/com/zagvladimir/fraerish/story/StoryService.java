package com.zagvladimir.fraerish.story;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.BotReply.Button;
import com.zagvladimir.fraerish.command.ChatContext;
import com.zagvladimir.fraerish.settings.CallbackOutcome;
import com.zagvladimir.fraerish.settings.SettingsAccess;
import com.zagvladimir.fraerish.telegram.ChatMembers;
import com.zagvladimir.fraerish.telegram.BotSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class StoryService {

    static final String QUESTION = "📸 Что в новой сторис?";
    static final List<String> DEFAULT_OPTIONS = List.of("Пейзаж", "Звёздное небо", "Качалка", "Селфи", "Дорожка в качалке", "Рилс");
    static final String ALL_VOTED = "st:done";
    static final String CONFIRM = "st:c:";
    static final int MAX_OPTIONS = 12;
    static final int TOP_SIZE = 10;
    static final int MAX_PLAYERS = 100;
    private static final int CONFIRM_BUTTONS_PER_ROW = 2;
    private static final List<String> MEDALS = List.of("🥇", "🥈", "🥉");

    private final StoryRepository repository;
    private final BotSender botSender;
    private final SettingsAccess settingsAccess;
    private final ChatMembers chatMembers;

    public BotReply start(long chatId) {
        if (repository.findOpenGame(chatId).isPresent()) {
            return BotReply.text("Угадайка уже идёт — проголосуйте и выберите, что было на самом деле");
        }
        List<String> options = options(chatId);
        List<List<Button>> buttons = List.of(List.of(new Button("🙋 Все проголосовали", ALL_VOTED)));
        return botSender.sendTrackedPoll(chatId, new BotReply.Poll(QUESTION, options), buttons)
                .map(poll -> {
                    repository.createGame(chatId, poll.messageId(), poll.pollId(), options);
                    return BotReply.none();
                })
                .orElseGet(() -> BotReply.text("Не получилось создать опрос, попробуйте ещё раз"));
    }

    public void vote(String pollId, long userId, String userName, List<Integer> optionIds) {
        repository.findByPollId(pollId).filter(StoryGame::open).filter(game -> !game.confirming()).ifPresent(game -> {
            if (optionIds == null || optionIds.isEmpty()) {
                repository.removeVote(game.id(), userId);
                return;
            }
            repository.saveVote(game.id(), userId, userName, optionIds.getFirst());
            int expected = expectedVoters(game.chatId());
            if (expected > 0 && repository.countVotes(game.id()) >= expected) {
                beginConfirmation(game);
            }
        });
    }

    public CallbackOutcome onButton(ChatContext context, String data) {
        Optional<StoryGame> game = repository.findOpenGame(context.chatId());
        if (game.isEmpty()) {
            return CallbackOutcome.toast("Эта угадайка уже закончилась");
        }
        if (ALL_VOTED.equals(data)) {
            return onAllVoted(game.get());
        }
        if (data.startsWith(CONFIRM)) {
            return onConfirm(context, game.get(), data.substring(CONFIRM.length()));
        }
        return CallbackOutcome.toast(null);
    }

    public BotReply answer(ChatContext context, String answer) {
        if (!settingsAccess.canConfigure(context)) {
            return BotReply.text("Правильный ответ выбирает большинство кнопками в сообщении «Что было на самом деле?». "
                    + "/answer — только для админов, если не договорились");
        }
        Optional<StoryGame> game = repository.findOpenGame(context.chatId());
        if (game.isEmpty()) {
            return BotReply.text("Сейчас нет открытой угадайки. Начать: /story");
        }
        List<String> options = game.get().options();
        if (answer.isBlank()) {
            return BotReply.text("Напишите, что в сторис: /answer " + options.getFirst().toLowerCase(Locale.ROOT)
                    + "\nВарианты: " + String.join(", ", options));
        }
        return match(options, answer.strip())
                .map(index -> finish(game.get(), index))
                .orElseGet(() -> BotReply.text("Нет такого варианта. Варианты: " + String.join(", ", options)));
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

    public BotReply changePlayers(ChatContext context, String value) {
        if (!settingsAccess.canConfigure(context)) {
            return BotReply.text("Число игроков меняют только админы группы");
        }
        String text = value.strip().toLowerCase(Locale.ROOT);
        if (text.isEmpty() || text.equals("0") || text.equals("авто") || text.equals("auto")) {
            repository.savePlayers(context.chatId(), null);
            return BotReply.text(chatMembers.people(context.chatId())
                    .map(people -> "Считаю игроков сам: участники группы без ботов — сейчас %d".formatted(people))
                    .orElse("Считаю игроков сам, но Telegram сейчас не отдал число участников"));
        }
        int players;
        try {
            players = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return BotReply.text("Напишите число игроков: /story_players 3 (или /story_players авто)");
        }
        if (players < 1 || players > MAX_PLAYERS) {
            return BotReply.text("Игроков может быть от 1 до %d".formatted(MAX_PLAYERS));
        }
        repository.savePlayers(context.chatId(), players);
        return BotReply.text("Угадайка ждёт голосов от %d %s".formatted(players, players == 1 ? "игрока" : "игроков"));
    }

    private int expectedVoters(long chatId) {
        if (chatId > 0) {
            return 1;
        }
        return repository.players(chatId).or(() -> chatMembers.people(chatId)).orElse(0);
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

    private CallbackOutcome onAllVoted(StoryGame game) {
        if (game.confirming()) {
            return CallbackOutcome.toast("Уже выясняем правду — смотрите сообщение ниже");
        }
        if (repository.countVotes(game.id()) == 0) {
            return CallbackOutcome.toast("Ещё никто не проголосовал");
        }
        beginConfirmation(game);
        return CallbackOutcome.toast("Выясняем, что было на самом деле");
    }

    private CallbackOutcome onConfirm(ChatContext context, StoryGame game, String optionText) {
        int index;
        try {
            index = Integer.parseInt(optionText);
        } catch (NumberFormatException e) {
            return CallbackOutcome.toast(null);
        }
        if (!game.confirming() || index < 0 || index >= game.options().size()) {
            return CallbackOutcome.toast(null);
        }
        repository.saveConfirmation(game.id(), context.userId(), index);
        Map<Integer, Long> counts = repository.confirmationCounts(game.id());
        long answered = counts.values().stream().mapToLong(Long::longValue).sum();
        Optional<Integer> leader = soleLeader(counts);
        boolean decided = leader.isPresent()
                && (counts.get(leader.get()) >= game.majority() || answered >= game.votersSnapshot());
        if (decided) {
            finish(game, leader.get());
            return CallbackOutcome.toast("Решено большинством!");
        }
        if (answered >= game.votersSnapshot()) {
            finishWithoutAnswer(game, counts);
            return CallbackOutcome.toast("Большинства нет 🤥");
        }
        botSender.editMenu(game.chatId(), game.confirmMessageId(), confirmationMenu(game.options(), answered, game.votersSnapshot()));
        return CallbackOutcome.toast("Ваш ответ «%s» учтён — остальные его не видят".formatted(game.options().get(index)));
    }

    private void finishWithoutAnswer(StoryGame game, Map<Integer, Long> counts) {
        repository.closeGame(game.id(), StoryRepository.NO_ANSWER);
        boolean everyoneDifferent = counts.values().stream().allMatch(count -> count == 1);
        String verdict = everyoneDifferent
                ? "🤥 Все жулики — каждый голосует за свой вариант"
                : "🤥 Голоса разделились поровну — жулики не договорились";
        botSender.editMenu(game.chatId(), game.confirmMessageId(), new BotReply.Menu(verdict + "\nУгадайка закрыта без ответа", List.of()));
    }

    private static Optional<Integer> soleLeader(Map<Integer, Long> counts) {
        long best = counts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        List<Integer> leaders = counts.entrySet().stream()
                .filter(entry -> entry.getValue() == best)
                .map(Map.Entry::getKey)
                .toList();
        return best > 0 && leaders.size() == 1 ? Optional.of(leaders.getFirst()) : Optional.empty();
    }

    private void beginConfirmation(StoryGame game) {
        int voters = repository.countVotes(game.id());
        botSender.stopPoll(game.chatId(), game.messageId());
        botSender.sendTrackedMenu(game.chatId(), confirmationMenu(game.options(), 0, voters))
                .ifPresent(messageId -> repository.startConfirmation(game.id(), messageId, voters));
    }

    private BotReply finish(StoryGame game, int index) {
        repository.closeGame(game.id(), index);
        List<String> winners = repository.votersFor(game.id(), index);
        String guessed = winners.isEmpty() ? "😅 Никто не угадал" : "🎯 Угадали: " + String.join(", ", winners);
        if (!game.confirming()) {
            botSender.stopPoll(game.chatId(), game.messageId());
            return BotReply.text("✅ В сторис: %s\n%s".formatted(game.options().get(index), guessed));
        }
        long confirmed = repository.confirmationCounts(game.id()).getOrDefault(index, 0L);
        String result = "✅ В сторис: %s (так ответили %d из %d)\n%s"
                .formatted(game.options().get(index), confirmed, game.votersSnapshot(), guessed);
        botSender.editMenu(game.chatId(), game.confirmMessageId(), new BotReply.Menu(result, List.of()));
        return BotReply.none();
    }

    private static BotReply.Menu confirmationMenu(List<String> options, long answered, int voters) {
        String text = "🕵️ Что было на самом деле? Каждый нажмите свой вариант — что выберет большинство, то и правда. "
                + "Ваш выбор видите только вы.\nОтветили: %d из %d".formatted(answered, voters);
        List<Button> buttons = IntStream.range(0, options.size())
                .mapToObj(i -> new Button(options.get(i), CONFIRM + i))
                .toList();
        List<List<Button>> rows = new ArrayList<>();
        for (int start = 0; start < buttons.size(); start += CONFIRM_BUTTONS_PER_ROW) {
            rows.add(buttons.subList(start, Math.min(start + CONFIRM_BUTTONS_PER_ROW, buttons.size())));
        }
        return new BotReply.Menu(text, rows);
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