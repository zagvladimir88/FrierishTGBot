package com.zagvladimir.fraerish.command;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ParsedCommand(String name, String args) {

    private static final Pattern COMMAND = Pattern.compile("^(/[^\\s@]+)(?:@(\\S+))?(?:\\s+(.*))?$", Pattern.DOTALL);

    public static Optional<ParsedCommand> parse(String text, String botName) {
        if (text == null) {
            return Optional.empty();
        }
        Matcher matcher = COMMAND.matcher(text.strip());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String mention = matcher.group(2);
        if (mention != null && !mention.equalsIgnoreCase(botName)) {
            return Optional.empty();
        }
        String args = matcher.group(3) == null ? "" : matcher.group(3).strip();
        return Optional.of(new ParsedCommand(matcher.group(1).toLowerCase(Locale.ROOT), args));
    }
}