package com.fx.FxSmartApi.service.advisor.strategies;

import com.fx.FxSmartApi.common.LevelsService;
import com.fx.FxSmartApi.common.TimeframeUtils;
import com.fx.FxSmartApi.model.Candle;
import com.fx.FxSmartApi.model.Signal;
import com.fx.FxSmartApi.model.StrategyConfig;
import com.fx.FxSmartApi.model.enums.SignalSide;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.fx.FxSmartApi.service.advisor.strategies.IctPrimitives.*;

public class ICTStrategy implements Strategy {

    @Override
    public List<Signal> evaluate(String symbol,
                                 List<Candle> m1,
                                 List<Candle> m5,
                                 List<Candle> m15,
                                 List<Candle> h4,
                                 List<Candle> d1,
                                 StrategyConfig cfg) {

        // Güvenlik kontrolleri
        if (m1 == null || m1.size() < 100 || m15 == null || m15.size() < 50) return List.of();

        // 1) Killzone filtresi (M15 son bar saatine göre)
        Candle last = m15.get(m15.size() - 1);
        Instant lastCloseUtc = last.getTime(); // Candle.time = Instant (UTC, close-time)

        boolean inNy = TimeframeUtils.inNewYorkAM(
                lastCloseUtc,               // <-- Instant overload
                cfg.nyTz,
                cfg.nyStartHour, cfg.nyStartMin,
                cfg.nyEndHour, cfg.nyEndMin
        );
        if (!inNy) return List.of(); // Killzone dışında sinyal üretme

        // 2) Referans çizgileri (M1 + referans gün: lastCloseUtc)
        Double midOpen  = cfg.useMidnightOpen ? LevelsService.midnightOpen(m1, cfg.nyTz, lastCloseUtc) : null;
        Double open0830 = cfg.useOpen0830     ? LevelsService.open0830(m1, cfg.nyTz, lastCloseUtc)     : null;

        // Bölge filtreleri
        boolean discount = (midOpen != null) && last.getClose() < midOpen;
        boolean premium  = (midOpen != null) && last.getClose() > midOpen;

        // 3) BOS tespiti (M15)
        Optional<Bos> bosOpt = findBos(m15, cfg.swingLeft, cfg.swingRight);
        if (bosOpt.isEmpty()) return List.of();
        Bos bos = bosOpt.get();

        // 4) OB ve FVG (M15)
        Optional<OB> obOpt = lastObBefore(m15, bos.index, bos.up);
        List<Fvg> fvgs = detectFvg(m15);

        // 5) OTE bölgesi — dealing range: son swing low/high (basit seçim)
        int n = m15.size();
        int from = Math.max(0, n - 120);
        int lastSwingH = -1, lastSwingL = -1;
        for (int i = from + cfg.swingLeft; i < n - cfg.swingRight; i++) {
            if (isSwingHigh(m15, i, cfg.swingLeft, cfg.swingRight)) lastSwingH = i;
            if (isSwingLow (m15, i, cfg.swingLeft, cfg.swingRight)) lastSwingL = i;
        }
        if (lastSwingH < 0 || lastSwingL < 0) return List.of();

        double swingHigh = m15.get(lastSwingH).getHigh();
        double swingLow  = m15.get(lastSwingL).getLow();

        // 6) Giriş mantığı
        List<Signal> out = new ArrayList<>();

        // ==== LONG setup ====
        if (bos.up && discount) {
            double px = last.getClose();

            boolean inOte = inOteLong(
                    px,
                    Math.min(swingLow, swingHigh),
                    Math.max(swingLow, swingHigh),
                    cfg.oteMin, cfg.oteMax
            );

            if (inOte) {
                // SL: pratikte LTF swing altı; basitçe swingLow
                double sl = swingLow;
                // TP1: yakın iç likidite – son local high
                double tp1 = swingHigh;
                double risk = Math.abs(px - sl);
                double reward = Math.abs(tp1 - px);

                if (risk > 0 && (reward / risk) >= cfg.rrMin) {
                    Signal s = new Signal();
                    s.setSymbol(symbol);
                    s.setTimeframe("M15");
                    s.setTime(lastCloseUtc);            // <-- Instant
                    s.setSide(SignalSide.BUY);
                    s.setEntry(px);
                    s.setSl(sl);
                    s.getTp().add(tp1);
                    s.setRR(risk, reward);              // "1:2.x" + rrValue

                    s.addReason("NY Killzone");
                    s.addReason("BOS up (M15)");
                    if (obOpt.isPresent()) s.addReason("Retest near OB");
                    if (open0830 != null) s.addReason(px < open0830 ? "Below 08:30 open (discount)" : "Above 08:30 open");
                    s.addReason(String.format("OTE %.0f%%-%.0f%%", cfg.oteMin * 100, cfg.oteMax * 100));

                    out.add(s);
                }
            }
        }

        // ==== SHORT setup ====
        if (!bos.up && premium) {
            double px = last.getClose();

            boolean inOte = inOteShort(
                    px,
                    Math.max(swingLow, swingHigh),
                    Math.min(swingLow, swingHigh),
                    cfg.oteMin, cfg.oteMax
            );

            if (inOte) {
                double sl = swingHigh;
                double tp1 = swingLow;
                double risk = Math.abs(sl - px);
                double reward = Math.abs(px - tp1);

                if (risk > 0 && (reward / risk) >= cfg.rrMin) {
                    Signal s = new Signal();
                    s.setSymbol(symbol);
                    s.setTimeframe("M15");
                    s.setTime(lastCloseUtc);            // <-- Instant
                    s.setSide(SignalSide.SELL);
                    s.setEntry(px);
                    s.setSl(sl);
                    s.getTp().add(tp1);
                    s.setRR(risk, reward);

                    s.addReason("NY Killzone");
                    s.addReason("BOS down (M15)");
                    if (obOpt.isPresent()) s.addReason("Retest near OB");
                    if (open0830 != null) s.addReason(px > open0830 ? "Above 08:30 open (premium)" : "Below 08:30 open");
                    s.addReason(String.format("OTE %.0f%%-%.0f%%", cfg.oteMin * 100, cfg.oteMax * 100));

                    out.add(s);
                }
            }
        }

        return out;
    }
}
