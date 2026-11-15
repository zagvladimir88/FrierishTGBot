package com.zagvladimir.fraerish.layout;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(5)
@Component
public class LayoutHandler implements CommandHandler {

    static final String USAGE = "Напишите текст после команды (/c ghbdtn) или ответьте командой /c на сообщение, набранное не в той раскладке";

    @Override
    public String command() {
        return "/c";
    }

    @Override
    public String description() {
        return "Исправить раскладку: /c ghbdtn или ответом на сообщение";
    }

    @Override
    public boolean acceptsEditedMessages() {
        return true;
    }

    @Override
    public BotReply handle(CommandRequest request) {
        if (request.hasArgs()) {
            return BotReply.text(KeyboardLayoutConverter.toCyrillic(request.args()));
        }
        return BotReply.text(request.replied().map(KeyboardLayoutConverter::toCyrillic).orElse(USAGE));
    }
}