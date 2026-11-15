package com.zagvladimir.fraerish.subscription;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StopHandler implements CommandHandler {

    static final String STOPPED = "Ежедневная рассылка отключена. Включить снова: /start";

    private final ChatRepository chatRepository;

    @Override
    public String command() {
        return "/stop";
    }

    @Override
    public BotReply handle(CommandRequest request) {
        chatRepository.deactivate(request.chatId());
        return BotReply.text(STOPPED);
    }
}