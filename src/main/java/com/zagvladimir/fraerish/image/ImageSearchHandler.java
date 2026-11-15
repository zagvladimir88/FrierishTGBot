package com.zagvladimir.fraerish.image;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ImageSearchHandler implements CommandHandler {

    static final String USAGE = "Incorrect query param";
    static final String NOTHING_FOUND = "Ничего не нашлось";

    private final ImageSearchClient imageSearchClient;
    private final PhotoFetcher photoFetcher;
    private final SearxngProperties properties;

    @Override
    public String command() {
        return "/g";
    }

    @Override
    public BotReply handle(CommandRequest request) {
        if (!request.hasArgs()) {
            return BotReply.text(USAGE);
        }
        List<ImageCandidate> candidates = imageSearchClient.search(request.args(), properties.candidates());
        List<BotReply.Photo> photos = photoFetcher.fetch(candidates, properties.maxImages());
        return photos.isEmpty() ? BotReply.text(NOTHING_FOUND) : BotReply.photos(photos);
    }
}