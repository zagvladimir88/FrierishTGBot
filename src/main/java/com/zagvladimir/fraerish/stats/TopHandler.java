package com.zagvladimir.fraerish.stats;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(8)
@Component
@RequiredArgsConstructor
public class TopHandler implements CommandHandler {

    static final String GROUPS_ONLY = "Статистика считается только в группах";
    static final String USAGE = "Укажите число дней от 1 до 365: /top 30";
    private static final int DEFAULT_DAYS = 7;
    private static final int MAX_DAYS = 365;

    private final ChatStats chatStats;

    @Override
    public String command() {
        return "/top";
    }

    @Override
    public String description() {
        return "Кто больше всех писал: /top или /top 30";
    }

    @Override
    public BotReply handle(CommandRequest request) {
        if (!request.context().groupChat()) {
            return BotReply.text(GROUPS_ONLY);
        }
        int days;
        try {
            days = request.hasArgs() ? Integer.parseInt(request.args().strip()) : DEFAULT_DAYS;
        } catch (NumberFormatException e) {
            return BotReply.text(USAGE);
        }
        if (days < 1 || days > MAX_DAYS) {
            return BotReply.text(USAGE);
        }
        return BotReply.text(chatStats.report(request.chatId(), days, "📊 Активность за " + daysLabel(days)));
    }

    private static String daysLabel(int days) {
        if (days == 1) {
            return "сегодня";
        }
        int lastTwo = days % 100;
        int last = days % 10;
        String word = lastTwo >= 11 && lastTwo <= 14 ? "дней" : last == 1 ? "день" : last >= 2 && last <= 4 ? "дня" : "дней";
        return days + " " + word;
    }
}