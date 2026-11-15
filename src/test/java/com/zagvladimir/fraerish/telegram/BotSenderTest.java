package com.zagvladimir.fraerish.telegram;

import com.zagvladimir.fraerish.command.BotReply;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.objects.ApiResponse;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BotSenderTest {

    private static final long CHAT_ID = 42L;
    private static final BotReply.Photo CAT = new BotReply.Photo("image-1.jpg", new byte[]{1});
    private static final BotReply.Photo DOG = new BotReply.Photo("image-2.png", new byte[]{2});

    private final TelegramClient telegramClient = mock(TelegramClient.class);
    private final BotSender sender = new BotSender(telegramClient);

    @Test
    void sendsTextMessage() throws TelegramApiException {
        assertThat(sender.send(CHAT_ID, BotReply.text("привет"))).isEqualTo(SendResult.SENT);

        SendMessage sent = captureSentText();
        assertThat(sent.getChatId()).isEqualTo(String.valueOf(CHAT_ID));
        assertThat(sent.getText()).isEqualTo("привет");
    }

    @Test
    void uploadsSinglePhotoBecauseAlbumNeedsAtLeastTwo() throws TelegramApiException {
        assertThat(sender.send(CHAT_ID, BotReply.photos(List.of(CAT)))).isEqualTo(SendResult.SENT);

        ArgumentCaptor<SendPhoto> captor = ArgumentCaptor.forClass(SendPhoto.class);
        verify(telegramClient).execute(captor.capture());
        assertThat(captor.getValue().getPhoto().isNew()).isTrue();
        assertThat(captor.getValue().getPhoto().getMediaName()).isEqualTo("image-1.jpg");
        verify(telegramClient, never()).execute(any(SendMediaGroup.class));
    }

    @Test
    void uploadsSeveralPhotosAsAlbum() throws TelegramApiException {
        assertThat(sender.send(CHAT_ID, BotReply.photos(List.of(CAT, DOG)))).isEqualTo(SendResult.SENT);

        ArgumentCaptor<SendMediaGroup> captor = ArgumentCaptor.forClass(SendMediaGroup.class);
        verify(telegramClient).execute(captor.capture());
        assertThat(captor.getValue().getMedias())
                .allMatch(InputMedia::isNewMedia)
                .extracting(InputMedia::getMediaName)
                .containsExactly("image-1.jpg", "image-2.png");
    }

    @Test
    void sendsPhotosOneByOneWhenAlbumIsRejected() throws TelegramApiException {
        doThrow(apiError(400, "Bad Request: PHOTO_INVALID_DIMENSIONS")).when(telegramClient).execute(any(SendMediaGroup.class));

        assertThat(sender.send(CHAT_ID, BotReply.photos(List.of(CAT, DOG)))).isEqualTo(SendResult.SENT);

        verify(telegramClient, times(2)).execute(any(SendPhoto.class));
        verify(telegramClient, never()).execute(any(SendMessage.class));
    }

    @Test
    void apologisesWhenTelegramRejectsEveryPhoto() throws TelegramApiException {
        doThrow(apiError(400, "Bad Request: PHOTO_INVALID_DIMENSIONS")).when(telegramClient).execute(any(SendMediaGroup.class));
        doThrow(apiError(400, "Bad Request: PHOTO_INVALID_DIMENSIONS")).when(telegramClient).execute(any(SendPhoto.class));

        assertThat(sender.send(CHAT_ID, BotReply.photos(List.of(CAT, DOG)))).isEqualTo(SendResult.SENT);

        assertThat(captureSentText().getText()).isEqualTo(BotSender.PHOTOS_REJECTED);
    }

    @Test
    void reportsBlockedChat() throws TelegramApiException {
        doThrow(apiError(403, "Forbidden: bot was blocked by the user")).when(telegramClient).execute(any(SendMessage.class));

        assertThat(sender.send(CHAT_ID, BotReply.text("привет"))).isEqualTo(SendResult.BLOCKED);
    }

    @Test
    void reportsBlockedChatForPhotosWithoutRetrying() throws TelegramApiException {
        doThrow(apiError(403, "Forbidden: bot was blocked by the user")).when(telegramClient).execute(any(SendMediaGroup.class));

        assertThat(sender.send(CHAT_ID, BotReply.photos(List.of(CAT, DOG)))).isEqualTo(SendResult.BLOCKED);
        verify(telegramClient, never()).execute(any(SendPhoto.class));
        verify(telegramClient, never()).execute(any(SendMessage.class));
    }

    @Test
    void sendsMenuWithInlineButtons() throws TelegramApiException {
        BotReply.Menu menu = new BotReply.Menu("настройки", List.of(
                List.of(new BotReply.Button("🔔 Включить", "s:on")),
                List.of(new BotReply.Button("🕘 Время", "s:time"), new BotReply.Button("🏙 Города", "s:cities"))));

        assertThat(sender.send(CHAT_ID, menu)).isEqualTo(SendResult.SENT);

        SendMessage sent = captureSentText();
        assertThat(sent.getText()).isEqualTo("настройки");
        InlineKeyboardMarkup keyboard = (InlineKeyboardMarkup) sent.getReplyMarkup();
        assertThat(keyboard.getKeyboard()).hasSize(2);
        assertThat(keyboard.getKeyboard().get(1)).extracting(InlineKeyboardButton::getCallbackData).containsExactly("s:time", "s:cities");
    }

    @Test
    void sendsPromptThatAsksForReply() throws TelegramApiException {
        assertThat(sender.send(CHAT_ID, new BotReply.Prompt("🏙 Напишите города"))).isEqualTo(SendResult.SENT);

        assertThat(captureSentText().getReplyMarkup()).isInstanceOf(ForceReplyKeyboard.class);
    }

    @Test
    void editsMenuInPlaceAndIgnoresUnchangedMessage() throws TelegramApiException {
        BotReply.Menu menu = new BotReply.Menu("настройки", List.of(List.of(new BotReply.Button("« Назад", "s:menu"))));

        sender.editMenu(CHAT_ID, 99, menu);

        ArgumentCaptor<EditMessageText> captor = ArgumentCaptor.forClass(EditMessageText.class);
        verify(telegramClient).execute(captor.capture());
        assertThat(captor.getValue().getMessageId()).isEqualTo(99);
        assertThat(captor.getValue().getText()).isEqualTo("настройки");

        doThrow(apiError(400, "Bad Request: message is not modified")).when(telegramClient).execute(any(EditMessageText.class));
        sender.editMenu(CHAT_ID, 99, menu);
    }

    @Test
    void answersButtonPress() throws TelegramApiException {
        sender.answerButton("query-1", "Сводка включена");

        ArgumentCaptor<AnswerCallbackQuery> captor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(telegramClient).execute(captor.capture());
        assertThat(captor.getValue().getCallbackQueryId()).isEqualTo("query-1");
        assertThat(captor.getValue().getText()).isEqualTo("Сводка включена");
    }

    @Test
    void uploadsVideoAsReplyWithSizeForProperPreview() throws TelegramApiException {
        BotReply.Video video = new BotReply.Video("video.mp4", new byte[]{1, 2}, 1080, 1920, 15);

        assertThat(sender.send(CHAT_ID, video, 7)).isEqualTo(SendResult.SENT);

        ArgumentCaptor<SendVideo> captor = ArgumentCaptor.forClass(SendVideo.class);
        verify(telegramClient).execute(captor.capture());
        SendVideo sent = captor.getValue();
        assertThat(sent.getVideo().isNew()).isTrue();
        assertThat(sent.getVideo().getMediaName()).isEqualTo("video.mp4");
        assertThat(sent.getWidth()).isEqualTo(1080);
        assertThat(sent.getHeight()).isEqualTo(1920);
        assertThat(sent.getDuration()).isEqualTo(15);
        assertThat(sent.getSupportsStreaming()).isTrue();
        assertThat(sent.getReplyToMessageId()).isEqualTo(7);
    }

    @Test
    void repliesWithTextToMessage() throws TelegramApiException {
        sender.send(CHAT_ID, BotReply.text("не вышло"), 7);

        assertThat(captureSentText().getReplyToMessageId()).isEqualTo(7);
    }

    @Test
    void showsVideoUploadStatus() throws TelegramApiException {
        sender.showUploadingVideo(CHAT_ID);

        ArgumentCaptor<SendChatAction> captor = ArgumentCaptor.forClass(SendChatAction.class);
        verify(telegramClient).execute(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("upload_video");
    }

    @Test
    void reportsOtherFailures() throws TelegramApiException {
        doThrow(new TelegramApiException("network is down")).when(telegramClient).execute(any(SendMessage.class));

        assertThat(sender.send(CHAT_ID, BotReply.text("привет"))).isEqualTo(SendResult.FAILED);
    }

    private SendMessage captureSentText() throws TelegramApiException {
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(captor.capture());
        return captor.getValue();
    }

    private static TelegramApiRequestException apiError(int code, String description) {
        return new TelegramApiRequestException("Error sending", new ApiResponse<>(false, code, description, null, null));
    }
}