package com.zagvladimir.fraerish.telegram;

import org.telegram.telegrambots.meta.api.objects.User;

import java.util.stream.Collectors;
import java.util.stream.Stream;

final class Users {

    private Users() {
    }

    static String displayName(User user) {
        String name = Stream.of(user.getFirstName(), user.getLastName())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        if (!name.isBlank()) {
            return name;
        }
        return user.getUserName() != null ? user.getUserName() : "id" + user.getId();
    }
}