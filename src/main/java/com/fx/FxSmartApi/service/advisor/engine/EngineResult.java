package com.fx.FxSmartApi.service.advisor.engine;

import com.fx.FxSmartApi.model.Signal;

import java.time.Instant;

public record EngineResult(
        SymbolTf key,
        String strategy,
        Signal signal,
        Instant candleCloseUtc
) {
    public String id() {
        return strategy + "|" + key.symbol() + "|" + key.timeframe() + "|" + candleCloseUtc.toString();
    }
}