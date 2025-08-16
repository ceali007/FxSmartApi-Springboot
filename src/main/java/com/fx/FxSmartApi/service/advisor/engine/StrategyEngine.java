package com.fx.FxSmartApi.service.advisor.engine;

import com.fx.FxSmartApi.common.TimeframeUtils;
import com.fx.FxSmartApi.model.Candle;
import com.fx.FxSmartApi.model.Signal;
import com.fx.FxSmartApi.model.StrategyConfig;
import com.fx.FxSmartApi.service.advisor.strategies.ICTStrategy;
import com.fx.FxSmartApi.service.advisor.strategies.Strategy;
import com.fx.FxSmartApi.service.data.StrategyTimeframeService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Service
public class StrategyEngine {

    private final CandleProvider data;
    // @Bean istemediğin için default config & registry’yi içeride kuruyoruz
    private final StrategyConfig cfg = new StrategyConfig(1.5);
    private final StrategyTimeframeService strategyTimeframeService;
    private final List<Function<SymbolTf, Strategy>> registry = List.of(
            job -> new ICTStrategy(job.symbol(), job.timeframe())
            // ileride: job -> new SMCStrategy(...), job -> new MomentumStrategy(...)
    );

    // >>> Tek constructor: Spring 4.3+ otomatik constructor injection yapar
    public StrategyEngine(CandleProvider data,
                          StrategyTimeframeService strategyTimeframeService) {
        this.data = data;
        this.strategyTimeframeService = strategyTimeframeService;
    }

    public List<EngineResult> runFor(SymbolTf symbolTf, Instant closeAlignUtc) {
        int need = strategyTimeframeService.getRequiredBars(symbolTf.timeframe());

        List<Candle> candles = data.fetchCandles(symbolTf.symbol(), symbolTf.timeframe(), need, closeAlignUtc);

        if (candles == null || candles.size() < need) {
            var nowClose = (closeAlignUtc != null) ? closeAlignUtc : Instant.now();
            var flats = new ArrayList<EngineResult>();
            for (var f : registry) {
                var s = f.apply(symbolTf);
                flats.add(new EngineResult(symbolTf, s.name(), Signal.flat(), nowClose));
            }
            return flats;
        }

        Instant lastClose = candles.get(candles.size() - 1).getTime();

        var out = new ArrayList<EngineResult>();
        for (var f : registry) {
            var s = f.apply(symbolTf);
            var sig = s.generate(candles, cfg);
            out.add(new EngineResult(symbolTf, s.name(), sig, lastClose));
        }
        return out;
    }
}