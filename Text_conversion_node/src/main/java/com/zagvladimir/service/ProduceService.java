package com.zagvladimir.service;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.HashMap;
import java.util.Map;

import static org.zagvladimir.model.RabbitQueue.ANSWER_TEXT_QUEUE;

@Service
@RequiredArgsConstructor
public class ProduceService {

    private final RabbitTemplate rabbitTemplate;
    private final Map<Character, String> layoutMap = createLayoutMap();

    public void processMessage(SendMessage message) {

        if (message == null) {
            return;
        }

        if (message.getText().startsWith("/c") && message.getText().length() > 3) {
            String messageForConversion = message.getText().substring(3);
            String text = convertLayout(messageForConversion);
            SendMessage sendMessage = createMessage(message, text);
            produceAnswer(sendMessage);
        }
    }

    private Map<Character, String> createLayoutMap() {
        Map<Character, String> res = new HashMap<>();
        String[] layoutPairs = {
                "aф", "bи", "cс", "dв", "eу", "fа", "gп", "hр", "iш", "jо", "kл",
                "lд", "mь", "nт", "oщ", "pз", "qй", "rк", "sы", "tе", "uг", "vм",
                "wц", "xч", "yн", "zя", "[х",
                "AФ", "BИ", "CС", "DВ", "EУ", "FА", "GП", "HР", "IШ", "JО", "KЛ",
                "LД", "MЬ", "NТ", "OЩ", "PЗ", "QЙ", "RК", "SЫ", "TЕ", "UГ", "VМ",
                "WЦ", "XЧ", "YН", "ZЯ", "{Х", "&?", "?,"
        };

        for (String pair : layoutPairs) {
            res.put(pair.charAt(0), pair.substring(1));
        }

        return res;
    }

    private String convertLayout(String input) {
        StringBuilder result = new StringBuilder();

        for (char c : input.toCharArray()) {
            result.append(layoutMap.getOrDefault(c, String.valueOf(c)));
        }

        return result.toString();
    }

    private SendMessage createMessage(SendMessage message, String text) {
        message.setText(text);
        return message;
    }

    private void produceAnswer(SendMessage sendMessage) {
        rabbitTemplate.convertAndSend(ANSWER_TEXT_QUEUE, sendMessage);
    }
}