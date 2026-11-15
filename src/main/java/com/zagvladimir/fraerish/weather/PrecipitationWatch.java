package com.zagvladimir.fraerish.weather;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

final class PrecipitationWatch {

    private static final Duration SLOT = Duration.ofHours(3);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private PrecipitationWatch() {
    }

    static Optional<PrecipitationAlert> upcoming(Forecast forecast, Instant now, Duration lookahead, double minChance) {
        Instant horizon = now.plus(lookahead);
        List<Forecast.Slot> wet = forecast.slots().stream()
                .filter(slot -> start(slot).plus(SLOT).isAfter(now) && !start(slot).isAfter(horizon))
                .filter(Precipitation::expectedIn)
                .filter(slot -> slot.pop() >= minChance || Precipitation.strongestOf(List.of(slot)) == Precipitation.THUNDERSTORM)
                .toList();
        if (wet.isEmpty()) {
            return Optional.empty();
        }
        Precipitation kind = Precipitation.strongestOf(wet);
        Instant first = start(wet.getFirst());
        String when = first.isAfter(now)
                ? "с " + first.atOffset(ZoneOffset.ofTotalSeconds(forecast.timezoneOffsetSeconds())).format(TIME)
                : "сейчас";
        long chance = Precipitation.chancePercent(wet);
        String city = forecast.cityName();
        String text = "%s %s: %s %s".formatted(kind.emoji(), city, when, kind.possible())
                + (chance > 0 ? " (%d%%)".formatted(chance) : "");
        return Optional.of(new PrecipitationAlert(city, text));
    }

    private static Instant start(Forecast.Slot slot) {
        return Instant.ofEpochSecond(slot.dt());
    }
}