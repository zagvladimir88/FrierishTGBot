package com.zagvladimir.fraerish.exchange;

import java.math.BigDecimal;

public sealed interface Quote {

    String code();

    default boolean known() {
        return this instanceof Fiat || this instanceof Crypto;
    }

    record Fiat(String code, int scale, BigDecimal rateInByn) implements Quote {
    }

    record Crypto(String code, BigDecimal priceInUsd, BigDecimal priceInByn) implements Quote {
    }

    record Unknown(String code) implements Quote {
    }

    record Unavailable(String code) implements Quote {
    }
}