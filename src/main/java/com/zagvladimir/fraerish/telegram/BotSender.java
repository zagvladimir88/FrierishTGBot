package com.zagvladimir.fraerish.telegram;

import com.zagvladimir.fraerish.command.BotReply;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotSender {

    private static final int FORBIDDEN = 403;
    private static final int MAX_MEDIA_GROUP_SIZE = 10;

    private final TelegramClient telegramClient;

    public SendResult send(long chatId, BotReply reply) {
        try {
            switch (reply) {
                case BotReply.Text text -> sendText(chatId, text.text());
                case BotReply.Photos photos -> sendPhotos(chatId, photos.urls());
            }
            return SendResult.SENT;
        } catch (TelegramApiRequestException e) {
            if (isForbidden(e)) {
                log.info("Chat {} is not available for the bot: {}", chatId, e.getApiResponse());
                return SendResult.BLOCKED;
            }
            log.error("Failed to send reply to chat {}: {}", chatId, e.getMessage());
            return SendResult.FAILED;
        } catch (TelegramApiException e) {
            log.error("Failed to send reply to chat {}: {}", chatId, e.getMessage());
            return SendResult.FAILED;
        }
    }

    private void sendText(long chatId, String text) throws TelegramApiException {
        telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).build());
    }

    private void sendPhotos(long chatId, List<String> urls) throws TelegramApiException {
        try {
            if (urls.size() == 1) {
                telegramClient.execute(SendPhoto.builder().chatId(chatId).photo(new InputFile(urls.getFirst())).build());
            } else {
                List<InputMediaPhoto> medias = urls.stream().limit(MAX_MEDIA_GROUP_SIZE).map(InputMediaPhoto::new).toList();
                telegramClient.execute(SendMediaGroup.builder().chatId(chatId).medias(medias).build());
            }
        } catch (TelegramApiRequestException e) {
            if (isForbidden(e)) {
                throw e;
            }
            log.warn("Telegram could not attach images for chat {}, sending links: {}", chatId, e.getMessage());
            sendText(chatId, String.join("\n", urls));
        }
    }

    private static boolean isForbidden(TelegramApiRequestException e) {
        return e.getErrorCode() != null && e.getErrorCode() == FORBIDDEN;
    }
}