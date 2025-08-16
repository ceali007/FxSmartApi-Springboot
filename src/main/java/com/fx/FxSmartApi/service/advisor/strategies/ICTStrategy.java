package com.fx.FxSmartApi.service.advisor.strategies;

import com.fx.FxSmartApi.common.LevelsService;
import com.fx.FxSmartApi.common.SymbolUtils;
import com.fx.FxSmartApi.model.Candle;
import com.fx.FxSmartApi.model.Signal;
import com.fx.FxSmartApi.model.StrategyConfig;
import com.fx.FxSmartApi.model.enums.SignalSide;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ICTStrategy implements Strategy {
    private final String symbol;
    private final String timeframe;
    public ICTStrategy(String symbol, String timeframe) {
        this.symbol = symbol; this.timeframe = timeframe;
    }

    @Override public String name() { return "ICT"; }

    private double pipTol(double price) { return price > 10 ? 0.02 : 0.0002; }

    @Override
    public Signal generate(List<Candle> w, StrategyConfig cfg) {
        if (w == null || w.size() < 15) return Signal.flat(); // 1H için 20 öneriyorduk; min guard

        Candle last = w.get(w.size() - 1);
        double eh = equalHigh(w, pipTol(last.getClose()));
        double el = equalLow(w,  pipTol(last.getClose()));

        boolean sweptHigh = eh > 0 && last.getHigh() > eh;
        boolean sweptLow  = el > 0 && last.getLow()  < el;

        boolean bullFvg = hasBullFvg(w);
        boolean bearFvg = hasBearFvg(w);

        // BUY
        if ((sweptLow && bullFvg) || sweptLow || bullFvg) {
            double entry = last.getClose();
            double slCandidate = el > 0 ? el : last.getLow();
            if (slCandidate >= entry) {
                double ps = SymbolUtils.pipSize(symbol);
                slCandidate = entry - 10 * ps;
            }
            double ps = SymbolUtils.pipSize(symbol);
            double riskPips = Math.max(1, Math.min(1000, Math.abs(entry - slCandidate) / ps));

            LevelsService.Levels levels = LevelsService.compute(symbol, entry, true, riskPips, cfg.getRiskReward());

            Map<String,Object> meta = new HashMap<>();
            meta.put("tf", timeframe);
            meta.put("riskPips", riskPips);
            meta.put("sweptLow", el);
            meta.put("bullFvg", bullFvg);

            return new Signal(SignalSide.BUY, levels.sl(), levels.tp(), 0.56, name(), meta);
        }

        // SELL
        if ((sweptHigh && bearFvg) || sweptHigh || bearFvg) {
            double entry = last.getClose();
            double slCandidate = eh > 0 ? eh : last.getHigh();
            if (slCandidate <= entry) {
                double ps = SymbolUtils.pipSize(symbol);
                slCandidate = entry + 10 * ps;
            }
            double ps = SymbolUtils.pipSize(symbol);
            double riskPips = Math.max(1, Math.min(1000, Math.abs(slCandidate - entry) / ps));

            LevelsService.Levels levels = LevelsService.compute(symbol, entry, false, riskPips, cfg.getRiskReward());

            Map<String,Object> meta = new HashMap<>();
            meta.put("tf", timeframe);
            meta.put("riskPips", riskPips);
            meta.put("sweptHigh", eh);
            meta.put("bearFvg", bearFvg);

            return new Signal(SignalSide.SELL, levels.sl(), levels.tp(), 0.56, name(), meta);
        }

        return Signal.flat();
    }

    // ---- helpers ----
    private double equalHigh(List<Candle> w, double tol) {
        double ref = 0.0;
        int start = Math.max(1, w.size() - 12);
        for (int i = start; i < w.size(); i++) {
            double h1 = w.get(i - 1).getHigh();
            double h2 = w.get(i).getHigh();
            if (Math.abs(h1 - h2) <= tol) ref = (h1 + h2) / 2.0;
        }
        return ref;
    }

    private double equalLow(List<Candle> w, double tol) {
        double ref = 0.0;
        int start = Math.max(1, w.size() - 12);
        for (int i = start; i < w.size(); i++) {
            double l1 = w.get(i - 1).getLow();
            double l2 = w.get(i).getLow();
            if (Math.abs(l1 - l2) <= tol) ref = (l1 + l2) / 2.0;
        }
        return ref;
    }

    private boolean hasBullFvg(List<Candle> w) {
        if (w.size() < 3) return false;
        var a = w.get(w.size() - 3);
        var c = w.get(w.size() - 1);
        return a.getHigh() < c.getLow();
    }

    private boolean hasBearFvg(List<Candle> w) {
        if (w.size() < 3) return false;
        var a = w.get(w.size() - 3);
        var c = w.get(w.size() - 1);
        return a.getLow() > c.getHigh();
    }
}