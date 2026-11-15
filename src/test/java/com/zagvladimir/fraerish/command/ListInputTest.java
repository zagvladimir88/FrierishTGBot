package com.zagvladimir.fraerish.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListInputTest {

    @Test
    void splitsCitiesByCommaKeepingSpacesInsideNames() {
        assertThat(ListInput.cities("минск,брест, Нижний   Новгород ;  MINSK , Минск"))
                .containsExactly("минск", "брест", "Нижний Новгород", "MINSK");
    }

    @Test
    void ignoresEmptyCityItems() {
        assertThat(ListInput.cities(" , ,, ")).isEmpty();
    }

    @Test
    void splitsCurrenciesByCommaOrSpaceAndIgnoresCase() {
        assertThat(ListInput.currencies("usd,EUR , btc eth;Usd")).containsExactly("USD", "EUR", "BTC", "ETH");
        assertThat(ListInput.currencies("USD, EUR, BTC")).containsExactly("USD", "EUR", "BTC");
        assertThat(ListInput.currencies("   ")).isEmpty();
    }

    @Test
    void keepsFirstSpellingOfDuplicates() {
        assertThat(ListInput.distinctIgnoringCase(List.of("Минск", "минск", "Брест"))).containsExactly("Минск", "Брест");
    }
}