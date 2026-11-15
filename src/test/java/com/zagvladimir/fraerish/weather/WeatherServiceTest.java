package com.zagvladimir.fraerish.weather;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WeatherServiceTest {

    private static final String MINSK_NOW = """
            {"name": "Минск",
             "weather": [{"id": 800, "main": "Clear", "description": "ясно"}],
             "main": {"temp": 0.84, "feels_like": -3.7, "humidity": 80}}""";
    private static final String MINSK_FORECAST = """
            {"cnt": 4, "city": {"name": "Минск", "timezone": 10800}, "list": [
              {"dt": 1794808800, "main": {"temp": 1.2, "feels_like": -2}, "weather": [{"main": "Clouds", "description": "облачно"}], "pop": 0},
              {"dt": 1794819600, "main": {"temp": 3.6, "feels_like": 0}, "weather": [{"main": "Clouds", "description": "облачно"}], "pop": 0.2},
              {"dt": 1794830400, "main": {"temp": 2.9, "feels_like": 0}, "weather": [{"main": "Rain", "description": "дождь"}], "pop": 0.7},
              {"dt": 1794841200, "main": {"temp": 1.0, "feels_like": -2}, "weather": [{"main": "Rain", "description": "дождь"}], "pop": 0.5}
            ]}""";

    private final RestClient.Builder restClientBuilder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    private final WeatherService service = new WeatherService(
            new OpenWeatherClient(restClientBuilder, new OpenWeatherProperties("https://weather.test", "secret")));

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void describesCurrentWeatherAndNextHours() {
        expect("/data/2.5/weather", "q=Минск&appid=secret&units=metric&lang=ru").andRespond(json(MINSK_NOW));
        expect("/data/2.5/forecast", "q=Минск&appid=secret&units=metric&lang=ru&cnt=4").andRespond(json(MINSK_FORECAST));

        assertThat(service.lookup("Минск")).isEqualTo(new CityWeather.Found("Минск", """
                Минск ясно ☀️, 1°C, ощущается как -4°C
                ближайшие 12 ч: от 1 до 4°C, с 15:00 возможен дождь (70%)"""));
    }

    @Test
    void showsCurrentWeatherWhenForecastFails() {
        expect("/data/2.5/weather", "q=Минск&appid=secret&units=metric&lang=ru").andRespond(json(MINSK_NOW));
        expect("/data/2.5/forecast", "q=Минск&appid=secret&units=metric&lang=ru&cnt=4").andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThat(service.lookup("Минск").text()).isEqualTo("Минск ясно ☀️, 1°C, ощущается как -4°C");
    }

    @Test
    void reportsUnknownCity() {
        expect("/data/2.5/weather", "q=Абырвалг&appid=secret&units=metric&lang=ru").andRespond(withStatus(HttpStatus.NOT_FOUND));

        CityWeather weather = service.lookup("Абырвалг");

        assertThat(weather).isEqualTo(new CityWeather.NotFound("Абырвалг"));
        assertThat(weather.text()).isEqualTo("Абырвалг — не нашёл такой город");
    }

    @Test
    void reportsUnavailableServiceWithoutThrowing() {
        expect("/data/2.5/weather", "q=Минск&appid=secret&units=metric&lang=ru").andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThat(service.lookup("Минск")).isEqualTo(new CityWeather.Unavailable("Минск"));
    }

    @Test
    void resolvesCanonicalCityNameWithoutForecast() {
        expect("/data/2.5/weather", "q=brest&appid=secret&units=metric&lang=ru").andRespond(json("""
                {"name": "Брест", "weather": [{"main": "Clouds", "description": "облачно"}], "main": {"temp": 3, "feels_like": 0}}"""));

        assertThat(service.resolve("brest")).isEqualTo(new CityWeather.Found("Брест", "Брест"));
    }

    private ResponseActions expect(String path, String query) {
        return server.expect(request -> {
            assertThat(request.getURI().getPath()).isEqualTo(path);
            assertThat(request.getURI().getQuery()).isEqualTo(query);
        });
    }

    private static ResponseCreator json(String body) {
        return withSuccess(body, MediaType.APPLICATION_JSON);
    }
}