package com.zagvladimir.fraerish.settings;

import com.zagvladimir.fraerish.command.BotReply;

public record CallbackOutcome(String toast, BotReply.Menu edit, BotReply message) {

    public static CallbackOutcome toast(String toast) {
        return new CallbackOutcome(toast, null, null);
    }

    public static CallbackOutcome edit(BotReply.Menu menu) {
        return new CallbackOutcome(null, menu, null);
    }

    public static CallbackOutcome edit(BotReply.Menu menu, String toast) {
        return new CallbackOutcome(toast, menu, null);
    }

    public static CallbackOutcome message(BotReply message) {
        return new CallbackOutcome(null, null, message);
    }
}