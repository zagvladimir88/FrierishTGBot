package com.zagvladimir.fraerish.settings;

import com.zagvladimir.fraerish.command.ChatContext;
import com.zagvladimir.fraerish.settings.SettingsMenu.PromptKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class PendingInputs {

    static final Duration WAIT_FOR = Duration.ofMinutes(5);

    private final Clock clock;
    private final Map<Key, Pending> pending = new ConcurrentHashMap<>();

    public void await(ChatContext context, PromptKind kind) {
        Instant now = clock.instant();
        pending.values().removeIf(entry -> entry.expiresAt().isBefore(now));
        pending.put(Key.of(context), new Pending(kind, now.plus(WAIT_FOR)));
    }

    public Optional<PromptKind> take(ChatContext context) {
        Pending entry = pending.remove(Key.of(context));
        return entry != null && entry.expiresAt().isAfter(clock.instant()) ? Optional.of(entry.kind()) : Optional.empty();
    }

    private record Key(long chatId, long userId) {

        static Key of(ChatContext context) {
            return new Key(context.chatId(), context.userId());
        }
    }

    private record Pending(PromptKind kind, Instant expiresAt) {
    }
}