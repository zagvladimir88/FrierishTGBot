package org.zagvladimir.service;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.zagvladimir.model.Chat;
import org.zagvladimir.repository.ChatRepository;

import java.util.List;

import static org.zagvladimir.model.RabbitQueue.EXCHANGE_QUEUE;
import static org.zagvladimir.model.RabbitQueue.WEATHER_QUEUE;
import static org.zagvladimir.model.Status.ACTIVE;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class ScheduleService {

    private final RabbitTemplate rabbitTemplate;
    private final ChatRepository chatRepository;

    @Scheduled(cron = "0 00 11 * * *")
    public void scheduledTask11() {
        sendMessagesWithText("/exchange USD", EXCHANGE_QUEUE);
    }

    @Scheduled(cron = "0 30 11 * * *")
    public void scheduledTask1130() {
        sendMessagesWithText("/w Минск", WEATHER_QUEUE);
    }

    private void sendMessagesWithText(String text, String rabbitQueue) {
        List<Chat> allChats = chatRepository.findAllByStatus(ACTIVE);

        allChats.forEach(chat -> {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chat.getChatId());
            sendMessage.setText(text);
            produceAnswer(rabbitQueue, sendMessage);
        });
    }

    private void produceAnswer(String rabbitQueue, SendMessage sendMessage) {
        rabbitTemplate.convertAndSend(rabbitQueue, sendMessage);
    }
}