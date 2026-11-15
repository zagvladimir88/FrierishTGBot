package com.zagvladimir.fraerish.image;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandHandler;
import com.zagvladimir.fraerish.command.CommandRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ImageSearchHandler implements CommandHandler {

    static final String USAGE = "Incorrect query param";
    static final String NOTHING_FOUND = "Ничего не нашлось";

    private final ImageSearchClient imageSearchClient;
    private final int maxImages;

    public ImageSearchHandler(ImageSearchClient imageSearchClient, SearxngProperties properties) {
        this.imageSearchClient = imageSearchClient;
        this.maxImages = properties.maxImages();
    }

    @Override
    public String command() {
        return "/g";
    }

    @Override
    public BotReply handle(CommandRequest request) {
        if (!request.hasArgs()) {
            return BotReply.text(USAGE);
        }
        List<String> urls = imageSearchClient.findImageUrls(request.args(), maxImages);
        return urls.isEmpty() ? BotReply.text(NOTHING_FOUND) : BotReply.photos(urls);
    }
}