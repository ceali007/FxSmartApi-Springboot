package com.fx.FxSmartApi.service.advisor.strategies;

import com.fx.FxSmartApi.model.Candle;
import com.fx.FxSmartApi.model.Signal;
import com.fx.FxSmartApi.model.StrategyConfig;

import java.util.List;

public interface Strategy {
    String name();
    Signal generate(List<Candle> candles, StrategyConfig cfg);
}