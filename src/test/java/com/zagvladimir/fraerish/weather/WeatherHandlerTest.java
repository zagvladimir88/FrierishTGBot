package com.zagvladimir.fraerish.weather;

import com.zagvladimir.fraerish.command.BotReply;
import com.zagvladimir.fraerish.command.CommandRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class WeatherHandlerTest {

    private final RestClient.Builder restClientBuilder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    private final WeatherHandler handler = new WeatherHandler(
            new OpenWeatherClient(restClientBuilder, new OpenWeatherProperties("https://weather.test", "secret")));

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void formatsCurrentWeather() {
        expectCity("Минск").andRespond(withSuccess("""
                {"name": "Минск",
                 "weather": [{"id": 804, "main": "Clouds", "description": "пасмурно", "icon": "04d"}],
                 "main": {"temp": -2.5, "feels_like": -6.1, "humidity": 80}}""", MediaType.APPLICATION_JSON));

        BotReply reply = handler.handle(new CommandRequest(1, "Минск"));

        assertThat(reply).isEqualTo(BotReply.text("Минск пасмурно ☁️ \nТемпература -2.5 ощущается как -6.1"));
    }

    @Test
    void sendsWholeCityNameAndLeavesEmojiEmptyForUnknownCondition() {
        expectCity("Нижний Новгород").andRespond(withSuccess("""
                {"name": "Нижний Новгород",
                 "weather": [{"main": "Mist", "description": "дымка"}],
                 "main": {"temp": 1.0, "feels_like": -1.0}}""", MediaType.APPLICATION_JSON));

        BotReply reply = handler.handle(new CommandRequest(1, "Нижний Новгород"));

        assertThat(reply).isEqualTo(BotReply.text("Нижний Новгород дымка  \nТемпература 1.0 ощущается как -1.0"));
    }

    @Test
    void reportsUnknownCity() {
        expectCity("Абырвалг").andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(handler.handle(new CommandRequest(1, "Абырвалг"))).isEqualTo(BotReply.text(WeatherHandler.CITY_NOT_FOUND));
    }

    @Test
    void propagatesServiceFailures() {
        expectCity("Минск").andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> handler.handle(new CommandRequest(1, "Минск"))).isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void asksForCityWhenArgumentsAreMissing() {
        assertThat(handler.handle(new CommandRequest(1, ""))).isEqualTo(BotReply.text(WeatherHandler.USAGE));
    }

    private ResponseActions expectCity(String city) {
        return server.expect(request -> {
                    assertThat(request.getURI().getHost()).isEqualTo("weather.test");
                    assertThat(request.getURI().getPath()).isEqualTo("/data/2.5/weather");
                    assertThat(request.getURI().getQuery()).isEqualTo("q=" + city + "&appid=secret&units=metric&lang=ru");
                })
                .andExpect(method(GET));
    }
}