package com.zagvladimir.fraerish.exchange;

import java.math.BigDecimal;
import java.util.Optional;

public interface CryptoPriceClient {

    Optional<BigDecimal> usdPrice(String symbol);
}