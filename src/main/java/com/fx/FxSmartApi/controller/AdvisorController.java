// src/main/java/com/fxsmart/api/advisor/AdvisorController.java
package com.fx.FxSmartApi.controller;


import com.fx.FxSmartApi.common.CandleClock;
import com.fx.FxSmartApi.service.advisor.engine.EngineResult;
import com.fx.FxSmartApi.service.advisor.engine.StrategyEngine;
import com.fx.FxSmartApi.service.advisor.engine.SymbolTf;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/advisor")
public class AdvisorController {
    private final StrategyEngine engine;

    public AdvisorController(StrategyEngine engine) {
        this.engine = engine;
    }

    @GetMapping("/signal")
    public List<EngineResult> getSignal(
            @RequestParam String symbol,
            @RequestParam String timeframe
    ) {
        Instant close = CandleClock.alignToClosed(Instant.now(), timeframe, 3);
        return engine.runFor(new SymbolTf(symbol.toUpperCase(), timeframe), close);
    }

    @GetMapping("/batch")
    public List<EngineResult> getBatch(
            @RequestParam List<String> symbols,
            @RequestParam String timeframe
    ) {
        Instant close = CandleClock.alignToClosed(Instant.now(), timeframe, 3);
        return symbols.stream()
                .flatMap(sym -> engine.runFor(new SymbolTf(sym.toUpperCase(), timeframe), close).stream())
                .toList();
    }
}
