package com.fx.FxSmartApi.service.advisor.engine;

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

    // Default config – ileride dışarıdan parametrelenebilir
    private final StrategyConfig cfg = new StrategyConfig(1.5);

    private final StrategyTimeframeService strategyTimeframeService;

    /**
     * Strateji kayıtları: her yeni strateji burada register edilecek
     */
    private final List<Function<SymbolTf, Strategy>> registry = List.of(
            job -> new ICTStrategy()
            // ileride: job -> new SMCStrategy(), job -> new MomentumStrategy()
    );

    public StrategyEngine(CandleProvider data,
                          StrategyTimeframeService strategyTimeframeService) {
        this.data = data;
        this.strategyTimeframeService = strategyTimeframeService;
    }

    public List<EngineResult> runFor(SymbolTf symbolTf, Instant closeAlignUtc) {
        int need = strategyTimeframeService.getRequiredBars(symbolTf.timeframe());

        List<Candle> candles =
                data.fetchCandles(symbolTf.symbol(), symbolTf.timeframe(), need, closeAlignUtc);

        if (candles == null || candles.size() < need) {
            var nowClose = (closeAlignUtc != null) ? closeAlignUtc : Instant.now();
            var flats = new ArrayList<EngineResult>();
            for (var f : registry) {
                var s = f.apply(symbolTf);
                flats.add(new EngineResult(symbolTf, s.name(), Signal.flat(nowClose), nowClose));
            }
            return flats;
        }

        Instant lastClose = candles.get(candles.size() - 1).getTime();

        var out = new ArrayList<EngineResult>();
        for (var f : registry) {
            var s = f.apply(symbolTf);

            // ICTStrategy gibi stratejiler M1, M5, M15, H4, D1 candle setleri bekliyor
            // Basit versiyonda elimizde sadece tek timeframe var.
            // İleride multi-timeframe fetch eklenmeli.
            List<Signal> sigs = s.evaluate(
                    symbolTf.symbol(),
                    candles, // M1
                    candles, // M5
                    candles, // M15
                    candles, // H4
                    candles, // D1
                    cfg
            );

            if (sigs.isEmpty()) {
                out.add(new EngineResult(symbolTf, s.name(), Signal.flat(lastClose), lastClose));
            } else {
                for (Signal sig : sigs) {
                    out.add(new EngineResult(symbolTf, s.name(), sig, lastClose));
                }
            }
        }
        return out;
    }
}
