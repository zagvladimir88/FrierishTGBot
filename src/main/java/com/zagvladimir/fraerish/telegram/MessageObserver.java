package com.zagvladimir.fraerish.telegram;

import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface MessageObserver {

    void observe(Message message);
}