package com.zagvladimir.fraerish.layout;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import org.springframework.stereotype.Component;

@Component
public class LayoutHandler implements CommandHandler {

    static final String USAGE = "Напишите текст после команды: /c ghbdtn";

    @Override
    public String command() {
        return "/c";
    }

    @Override
    public boolean acceptsEditedMessages() {
        return true;
    }

    @Override
    public BotReply handle(CommandRequest request) {
        return BotReply.text(request.hasArgs() ? KeyboardLayoutConverter.toCyrillic(request.args()) : USAGE);
    }
}