package com.zagvladimir.fraerish.weather;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

final class ForecastSummary {

    private static final int HOURS_PER_SLOT = 3;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private ForecastSummary() {
    }

    static Optional<String> describe(Forecast forecast) {
        List<Forecast.Slot> slots = forecast.slots();
        if (slots.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("ближайшие %d ч: %s, %s".formatted(
                slots.size() * HOURS_PER_SLOT,
                temperatureRange(slots),
                precipitation(slots, ZoneOffset.ofTotalSeconds(forecast.timezoneOffsetSeconds()))));
    }

    private static String temperatureRange(List<Forecast.Slot> slots) {
        long min = slots.stream().mapToLong(slot -> Math.round(slot.main().temp())).min().orElseThrow();
        long max = slots.stream().mapToLong(slot -> Math.round(slot.main().temp())).max().orElseThrow();
        return min == max ? "около %d°C".formatted(min) : "от %d до %d°C".formatted(min, max);
    }

    private static String precipitation(List<Forecast.Slot> slots, ZoneOffset offset) {
        List<Forecast.Slot> wet = slots.stream()
                .filter(slot -> Precipitation.of(slot.condition().main()).isPresent())
                .toList();
        if (wet.isEmpty()) {
            return "без осадков";
        }
        Precipitation kind = Precipitation.strongest(wet.stream()
                .map(slot -> Precipitation.of(slot.condition().main()).orElseThrow())
                .toList()).orElseThrow();
        String start = Instant.ofEpochSecond(wet.getFirst().dt()).atOffset(offset).format(TIME);
        long chance = Math.round(wet.stream().mapToDouble(Forecast.Slot::pop).max().orElse(0) * 100);
        String description = "с %s возмож%s %s".formatted(start, kind == Precipitation.THUNDERSTORM ? "на" : "ен", kind.description());
        return chance > 0 ? description + " (%d%%)".formatted(chance) : description;
    }
}