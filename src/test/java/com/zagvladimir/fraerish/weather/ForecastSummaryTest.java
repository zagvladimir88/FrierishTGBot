package com.zagvladimir.fraerish.weather;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ForecastSummaryTest {

    private static final long MORNING_UTC = 1794808800L;
    private static final int MINSK_OFFSET = 3 * 3600;
    private static final long THREE_HOURS = 3 * 3600L;

    @Test
    void reportsTemperatureRangeAndDryWeather() {
        Forecast forecast = forecast(slot(0, -1.4, "Clouds", 0), slot(1, 0.6, "Clear", 0), slot(2, 1.4, "Clouds", 0), slot(3, 0.2, "Clouds", 0));

        assertThat(ForecastSummary.describe(forecast)).contains("ближайшие 12 ч: от -1 до 1°C, без осадков");
    }

    @Test
    void reportsFirstLocalTimeOfRainWithHighestChance() {
        Forecast forecast = forecast(slot(0, 2, "Clouds", 0.1), slot(1, 4, "Clouds", 0.2), slot(2, 3, "Rain", 0.7), slot(3, 2, "Drizzle", 0.4));

        assertThat(ForecastSummary.describe(forecast)).contains("ближайшие 12 ч: от 2 до 4°C, с 15:00 возможен дождь (70%)");
    }

    @Test
    void namesSleetThunderstormAndSnow() {
        assertThat(ForecastSummary.describe(forecast(slot(0, 0, "Rain", 0.5), slot(1, 0, "Snow", 0.6))))
                .contains("ближайшие 6 ч: около 0°C, с 09:00 возможен дождь со снегом (60%)");
        assertThat(ForecastSummary.describe(forecast(slot(0, 20, "Rain", 0.5), slot(1, 22, "Thunderstorm", 0.9))))
                .contains("ближайшие 6 ч: от 20 до 22°C, с 09:00 возможна гроза (90%)");
        assertThat(ForecastSummary.describe(forecast(slot(0, -3, "Snow", 0))))
                .contains("ближайшие 3 ч: около -3°C, с 09:00 возможен снег");
    }

    @Test
    void returnsNothingWithoutSlots() {
        assertThat(ForecastSummary.describe(new Forecast(null, null))).isEmpty();
    }

    private static Forecast forecast(Forecast.Slot... slots) {
        return new Forecast(List.of(slots), new Forecast.City(MINSK_OFFSET));
    }

    private static Forecast.Slot slot(int index, double temperature, String condition, double chance) {
        return new Forecast.Slot(MORNING_UTC + index * THREE_HOURS,
                new CurrentWeather.Temperature(temperature, temperature),
                List.of(new CurrentWeather.Condition(condition, condition.toLowerCase())),
                chance);
    }
}