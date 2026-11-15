package com.zagvladimir.fraerish.telegram;

import com.zagvladimir.fraerish.command.BotReply;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.polls.StopPoll;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.polls.input.InputPollOption;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotSender {

    static final String PHOTOS_REJECTED = "Не получилось отправить картинки, попробуйте другой запрос";

    private static final String MESSAGE_NOT_MODIFIED = "message is not modified";
    private static final int FORBIDDEN = 403;
    private static final int MAX_MEDIA_GROUP_SIZE = 10;

    private final TelegramClient telegramClient;

    public SendResult send(long chatId, BotReply reply) {
        return send(chatId, reply, null);
    }

    public SendResult send(long chatId, BotReply reply, Integer replyToMessageId) {
        try {
            switch (reply) {
                case BotReply.Text text -> telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text(text.text())
                        .replyToMessageId(replyToMessageId)
                        .build());
                case BotReply.None none -> {
                }
                case BotReply.Poll poll -> telegramClient.execute(publicPoll(chatId, poll));
                case BotReply.Video video -> telegramClient.execute(SendVideo.builder()
                        .chatId(chatId)
                        .video(new InputFile(new ByteArrayInputStream(video.content()), video.fileName()))
                        .width(video.width())
                        .height(video.height())
                        .duration(video.duration())
                        .supportsStreaming(true)
                        .replyToMessageId(replyToMessageId)
                        .build());
                case BotReply.Photos photos -> sendPhotos(chatId, photos.photos());
                case BotReply.Menu menu -> telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text(menu.text())
                        .replyMarkup(keyboard(menu))
                        .build());
                case BotReply.Prompt prompt -> telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text(prompt.text())
                        .replyMarkup(ForceReplyKeyboard.builder().forceReply(true).build())
                        .build());
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

    public Optional<SentPoll> sendTrackedPoll(long chatId, BotReply.Poll poll) {
        try {
            Message message = telegramClient.execute(publicPoll(chatId, poll));
            return Optional.of(new SentPoll(message.getMessageId(), message.getPoll().getId()));
        } catch (TelegramApiException e) {
            log.error("Failed to send poll to chat {}: {}", chatId, e.getMessage());
            return Optional.empty();
        }
    }

    public void stopPoll(long chatId, int messageId) {
        try {
            telegramClient.execute(StopPoll.builder().chatId(chatId).messageId(messageId).build());
        } catch (TelegramApiException e) {
            log.warn("Failed to close poll {} in chat {}: {}", messageId, chatId, e.getMessage());
        }
    }

    private static SendPoll publicPoll(long chatId, BotReply.Poll poll) {
        return SendPoll.builder()
                .chatId(chatId)
                .question(poll.question())
                .options(poll.options().stream().map(InputPollOption::new).toList())
                .isAnonymous(false)
                .build();
    }

    public void editMenu(long chatId, int messageId, BotReply.Menu menu) {
        try {
            telegramClient.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(menu.text())
                    .replyMarkup(keyboard(menu))
                    .build());
        } catch (TelegramApiException e) {
            if (!e.getMessage().contains(MESSAGE_NOT_MODIFIED)) {
                log.error("Failed to update menu in chat {}: {}", chatId, e.getMessage());
            }
        }
    }

    public void showUploadingVideo(long chatId) {
        try {
            telegramClient.execute(SendChatAction.builder().chatId(chatId).action(ActionType.UPLOAD_VIDEO.toString()).build());
        } catch (TelegramApiException e) {
            log.debug("Failed to show upload status in chat {}: {}", chatId, e.getMessage());
        }
    }

    public void answerButton(String callbackQueryId, String toast) {
        try {
            telegramClient.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackQueryId).text(toast).build());
        } catch (TelegramApiException e) {
            log.warn("Failed to answer button press {}: {}", callbackQueryId, e.getMessage());
        }
    }

    private static InlineKeyboardMarkup keyboard(BotReply.Menu menu) {
        return new InlineKeyboardMarkup(menu.keyboard().stream()
                .map(row -> new InlineKeyboardRow(row.stream()
                        .map(button -> InlineKeyboardButton.builder().text(button.text()).callbackData(button.data()).build())
                        .toList()))
                .toList());
    }

    private void sendText(long chatId, String text) throws TelegramApiException {
        telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).build());
    }

    private void sendPhotos(long chatId, List<BotReply.Photo> photos) throws TelegramApiException {
        if (photos.size() == 1) {
            sendOneByOne(chatId, photos);
            return;
        }
        try {
            sendAlbum(chatId, photos);
        } catch (TelegramApiRequestException e) {
            if (isForbidden(e)) {
                throw e;
            }
            log.warn("Telegram rejected the album for chat {}, sending photos one by one: {}", chatId, e.getMessage());
            sendOneByOne(chatId, photos);
        }
    }

    private void sendAlbum(long chatId, List<BotReply.Photo> photos) throws TelegramApiException {
        List<InputMediaPhoto> medias = photos.stream()
                .limit(MAX_MEDIA_GROUP_SIZE)
                .map(photo -> new InputMediaPhoto(new ByteArrayInputStream(photo.content()), photo.fileName()))
                .toList();
        telegramClient.execute(SendMediaGroup.builder().chatId(chatId).medias(medias).build());
    }

    private void sendOneByOne(long chatId, List<BotReply.Photo> photos) throws TelegramApiException {
        int sent = 0;
        for (BotReply.Photo photo : photos) {
            try {
                telegramClient.execute(SendPhoto.builder()
                        .chatId(chatId)
                        .photo(new InputFile(new ByteArrayInputStream(photo.content()), photo.fileName()))
                        .build());
                sent++;
            } catch (TelegramApiRequestException e) {
                if (isForbidden(e)) {
                    throw e;
                }
                log.warn("Telegram rejected photo {} for chat {}: {}", photo.fileName(), chatId, e.getMessage());
            }
        }
        if (sent == 0) {
            sendText(chatId, PHOTOS_REJECTED);
        }
    }

    private static boolean isForbidden(TelegramApiRequestException e) {
        return e.getErrorCode() != null && e.getErrorCode() == FORBIDDEN;
    }
}