package com.fx.FxSmartApi.service.advisor.engine;

import com.fx.FxSmartApi.model.dto.Candle;

import java.time.Instant;
import java.util.List;

public interface CandleProvider {
    List<Candle> fetchCandles(String symbol, String timeframe, int limit, Instant untilCloseUtc);
}