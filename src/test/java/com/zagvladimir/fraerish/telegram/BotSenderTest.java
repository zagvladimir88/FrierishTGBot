package com.zagvladimir.fraerish.telegram;

import com.zagvladimir.fraerish.command.BotReply;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
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