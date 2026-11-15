package com.zagvladimir.fraerish.weather;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

enum Precipitation {

    THUNDERSTORM("гроза"),
    SLEET("дождь со снегом"),
    SNOW("снег"),
    RAIN("дождь");

    private final String description;

    Precipitation(String description) {
        this.description = description;
    }

    String description() {
        return description;
    }

    static Optional<Precipitation> of(String condition) {
        return switch (condition.toLowerCase(Locale.ROOT)) {
            case "thunderstorm" -> Optional.of(THUNDERSTORM);
            case "snow" -> Optional.of(SNOW);
            case "rain", "drizzle" -> Optional.of(RAIN);
            default -> Optional.empty();
        };
    }

    static Optional<Precipitation> strongest(Collection<Precipitation> kinds) {
        Set<Precipitation> present = Set.copyOf(kinds);
        if (present.contains(THUNDERSTORM)) {
            return Optional.of(THUNDERSTORM);
        }
        if (present.contains(RAIN) && present.contains(SNOW)) {
            return Optional.of(SLEET);
        }
        return present.stream().findFirst();
    }
}