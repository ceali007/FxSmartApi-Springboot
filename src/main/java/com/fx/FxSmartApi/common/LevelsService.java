package com.fx.FxSmartApi.common;

public class LevelsService {
    public static record Levels(double tp, double sl) {}

    public static Levels compute(String symbol, double entry, boolean isBuy, double riskPips, double rr) {
        double ps = SymbolUtils.pipSize(symbol);
        double risk = riskPips * ps;
        double reward = riskPips * rr * ps;

        double tp = isBuy ? entry + reward : entry - reward;
        double sl = isBuy ? entry - risk   : entry + risk;

        // normalize tick (ps)
        tp = Math.round(tp / ps) * ps;
        sl = Math.round(sl / ps) * ps;

        // direction guard
        if (isBuy && !(tp > entry && sl < entry)) { tp = entry + reward; sl = entry - risk; }
        if (!isBuy && !(tp < entry && sl > entry)) { tp = entry - reward; sl = entry + risk; }

        return new Levels(tp, sl);
    }
}