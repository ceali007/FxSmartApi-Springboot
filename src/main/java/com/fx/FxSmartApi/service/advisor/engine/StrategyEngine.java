package com.fx.FxSmartApi.service.advisor.engine;

import com.fx.FxSmartApi.config.FxProperties;
import com.fx.FxSmartApi.model.api.AdvisorExecuteRequest;
import com.fx.FxSmartApi.model.dto.Candle;
import com.fx.FxSmartApi.model.dto.Signal;
import com.fx.FxSmartApi.model.dto.StrategyConfig;
import com.fx.FxSmartApi.service.advisor.strategies.ICTStrategy;
import com.fx.FxSmartApi.service.advisor.strategies.Strategy;
import com.fx.FxSmartApi.service.data.StrategyTimeframeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StrategyEngine {

    private static final Logger log = LoggerFactory.getLogger(StrategyEngine.class);

    private final CandleProvider data;

    // Default config – ileride dışarıdan parametrelenebilir
    private final StrategyConfig cfg = new StrategyConfig(1.5);

    private final StrategyTimeframeService strategyTimeframeService;

    /** Sonuçların yazılacağı port (upsert vs.) */
    private final SignalSync signalSync; // <-- NEW
    private final FxProperties fxProps;

    /**
     * Strateji kayıtları: her yeni strateji burada register edilecek
     */
    private final List<Function<SymbolTf, Strategy>> registry = List.of(
            job -> new ICTStrategy()
            // ileride: job -> new SMCStrategy(), job -> new MomentumStrategy()
    );

    public StrategyEngine(CandleProvider data,
                          StrategyTimeframeService strategyTimeframeService,
                          @Qualifier("signalUpsertService") SignalSync signalSync,  // veya "mongoSignalSync"
                          FxProperties fxProps) {
        this.data = data;
        this.strategyTimeframeService = strategyTimeframeService;
        this.signalSync = signalSync;
        this.fxProps = fxProps;
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

            // Basit: tek TF seti ile besliyoruz (ileride multi-TF eklenecek)
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

    // ====================== NEW: API Execute Akışı ======================


    /**
     * /api/advisor/execute akışı:
     * - Zaman/TZ resolve (default fx.tz => America/New_York)
     * - manual/auto moduna göre TF aday listesi
     * - Her (symbol,strategy,tf) için close zamanını TF’e "floor"la, runFor(...) çağır
     * - EngineResult’ları SignalSync ile idempotent upsert et
     */
    public void executeFromApi(AdvisorExecuteRequest req) {
        Logger log = LoggerFactory.getLogger(StrategyEngine.class);

        // --- 1) Zaman ve TZ (default: fx.tz) ---
        ZoneId zone = ZoneId.of(
                (req != null && req.getTz() != null && !req.getTz().isBlank())
                        ? req.getTz()
                        : fxProps.zoneId().getId()
        );

        ZonedDateTime atZdt;
        try {
            atZdt = (req != null && req.getAt() != null && !req.getAt().isBlank())
                    ? LocalDateTime.parse(req.getAt()).atZone(zone)      // "yyyy-MM-ddTHH:mm:ss"
                    : ZonedDateTime.now(zone);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid 'at' format. Use ISO local, e.g. 2025-08-30T13:00:00");
        }

        // --- 2) Symbols/Strategies ---
        List<String> symbols = (req != null && req.getSymbols() != null)
                ? req.getSymbols().stream().filter(Objects::nonNull).toList()
                : Collections.emptyList();
        if (symbols.isEmpty()) {
            throw new IllegalArgumentException("symbols required");
        }

        Set<String> requestedStrategies = (req != null && req.getStrategies() != null)
                ? req.getStrategies().stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new))
                : Collections.emptySet();

        boolean manual = (req != null && "manual".equalsIgnoreCase(req.getMode()));
        List<String> manualTFs = (req != null && req.getTimeframes() != null)
                ? req.getTimeframes().stream().filter(Objects::nonNull).toList()
                : Collections.emptyList();

        // --- 3) Registry’den strategy name -> factory haritası oluştur ---
        // Not: registry List<Function<SymbolTf, Strategy>> mevcut (ICTStrategy vb.)
        Map<String, Function<SymbolTf, Strategy>> available = new LinkedHashMap<>();
        for (Function<SymbolTf, Strategy> f : registry) {
            // sadece isim almak için temp instance
            Strategy tmp = f.apply(new SymbolTf("_", "_"));
            available.put(tmp.name(), job -> f.apply(job));
        }
        if (!requestedStrategies.isEmpty()) {
            available.keySet().retainAll(requestedStrategies);
            if (available.isEmpty()) {
                throw new IllegalArgumentException("No matching strategies for " + requestedStrategies);
            }
        }

        // --- 4) Çalıştırma döngüsü ---
        for (String symbol : symbols) {
            for (Map.Entry<String, Function<SymbolTf, Strategy>> e : available.entrySet()) {
                String strategyName = e.getKey();

                // Auto: stratejiye göre tercihli TF seti; Manual: kullanıcıdan gelen TF listesi
                List<String> candidateTFs = manual ? manualTFs : preferredTFs(strategyName);
                if (candidateTFs.isEmpty()) {
                    log.info("No candidate TFs for strategy={} (mode={})", strategyName, manual ? "manual" : "auto");
                    continue;
                }

                for (String tf : candidateTFs) {
                    // 4.a) 'at' değerini TF’e göre aşağı yuvarla (NY zonunda),
                    //      ve bu anı UTC Instant’a çevirip runFor’a ver.
                    ZonedDateTime alignedNy = floorToTimeframe(atZdt, tf, zone);
                    Instant closeAlignUtc = alignedNy.toInstant();

                    var results = runFor(new SymbolTf(symbol, tf), closeAlignUtc);

                    // 4.b) Sonuçları signals’a upsert et
                    for (EngineResult er : results) {
                        try {
                            signalSync.upsert(er);
                        } catch (Exception ex) {
                            log.error("Signal upsert failed: symbol={} strategy={} tf={} err={}",
                                    er.key().symbol(), er.strategy(), er.key().timeframe(), ex.getMessage(), ex);
                        }
                    }

                    // Not: İlk uygun TF’de durmak istersen burada 'break;' koyabilirsin.
                    // Şimdilik tüm TF adaylarını çalıştırıyoruz.
                }
            }
        }
    }

    /* ===================== Yardımcılar ===================== */

    /** Stratejiye göre tercihli TF seti (auto mod için) */
    private List<String> preferredTFs(String strategyName) {
        if ("ICT".equalsIgnoreCase(strategyName) || "ICTStrategy".equalsIgnoreCase(strategyName)) {
            return List.of("15m", "5m", "1m");
        }
        // default fallback
        return List.of("5m", "15m");
    }

    /** Zamanı verilen timeframe'e göre aşağı yuvarlar (ör. 13:07 -> 13:00 @15m) */
    private static ZonedDateTime floorToTimeframe(ZonedDateTime zdt, String tf, ZoneId zone) {
        int minutes = parseTfMinutes(tf);
        if (minutes <= 0) return zdt.withSecond(0).withNano(0);

        // zdt zaten doğru zoneda (fx.tz); aynı zonda floor uygula
        int totalMin = zdt.getHour() * 60 + zdt.getMinute();
        int floored = (totalMin / minutes) * minutes;
        int h = floored / 60;
        int m = floored % 60;
        return zdt.withHour(h).withMinute(m).withSecond(0).withNano(0);
    }

    /** "1m","5m","15m","30m","1h","4h","1d" -> dakika değeri */
    private static int parseTfMinutes(String tf) {
        if (tf == null) return 0;
        String s = tf.trim().toLowerCase(Locale.ROOT);
        if (s.endsWith("m")) {
            return Integer.parseInt(s.substring(0, s.length() - 1));
        } else if (s.endsWith("h")) {
            int hours = Integer.parseInt(s.substring(0, s.length() - 1));
            return hours * 60;
        } else if (s.endsWith("d")) {
            int days = Integer.parseInt(s.substring(0, s.length() - 1));
            return days * 24 * 60;
        }
        return 0;
    }

    // ---------------------- Helpers ----------------------

    /** Basit “yeni örnek yarat” — stratejileri parametresiz new’leyebildiğimiz sürece yeterli. */
    private Strategy newInstanceLike(Strategy sample, SymbolTf job) {
        // Şimdilik yalnızca ICTStrategy var; parametresiz ctor ile yeni instance
        if (sample instanceof ICTStrategy) {
            return new ICTStrategy();
        }
        // Yeni stratejileri burada eşle:
        // if (sample instanceof SMCStrategy) return new SMCStrategy();
        return new ICTStrategy(); // fallback
    }

    private static List<String> nullSafeList(List<String> in) {
        return (in == null) ? Collections.emptyList() : in.stream().filter(Objects::nonNull).toList();
    }

    private static String optionalOr(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }

    /** Verilen ZDT’yi timeframe’e göre aşağı yuvarlar (floor) */
    private static ZonedDateTime floorToTimeframe(ZonedDateTime zdt, String tf) {
        int minutes = parseTfMinutes(tf); // 1m,5m,15m,1h,...
        if (minutes <= 0) return zdt;

        int totalMinutes = zdt.getHour() * 60 + zdt.getMinute();
        int floored = (totalMinutes / minutes) * minutes;
        int h = floored / 60;
        int m = floored % 60;

        return zdt.withHour(h).withMinute(m).withSecond(0).withNano(0);
    }


}
