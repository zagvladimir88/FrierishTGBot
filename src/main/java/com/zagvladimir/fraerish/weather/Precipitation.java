package com.zagvladimir.fraerish.weather;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

enum Precipitation {

    THUNDERSTORM("возможна гроза", "⛈️"),
    SLEET("возможен дождь со снегом", "🌨️"),
    SNOW("возможен снег", "❄️"),
    RAIN("возможен дождь", "🌧️");

    private final String possible;
    private final String emoji;

    Precipitation(String possible, String emoji) {
        this.possible = possible;
        this.emoji = emoji;
    }

    String possible() {
        return possible;
    }

    String emoji() {
        return emoji;
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

    static Precipitation strongestOf(List<Forecast.Slot> wetSlots) {
        return strongest(wetSlots.stream().map(slot -> of(slot.condition().main()).orElseThrow()).toList()).orElseThrow();
    }

    static boolean expectedIn(Forecast.Slot slot) {
        return of(slot.condition().main()).isPresent();
    }

    static long chancePercent(List<Forecast.Slot> wetSlots) {
        return Math.round(wetSlots.stream().mapToDouble(Forecast.Slot::pop).max().orElse(0) * 100);
    }
}