package com.fx.FxSmartApi.common;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Set;

public class TimeframeUtils {

    private static final Set<String> SUPPORTED_INTERVALS = Set.of(
            "1min", "5min", "15min", "30min", "45min",
            "1h", "2h", "4h", "8h",
            "1day", "1week", "1month"
    );

    /**
     * Sembol normalizasyonu - EURUSD -> EUR/USD gibi
     */
    public static String normalizeSymbol(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toUpperCase();
        if (s.contains("/")) return s; // zaten doğru format
        if (s.matches("^[A-Z]{6}$")) {
            return s.substring(0, 3) + "/" + s.substring(3);
        }
        return s;
    }

    /**
     * Timeframe'i desteklenen formata çevirir, değilse default 15min döner.
     */
    public static String normalizeInterval(String tf) {
        if (tf == null) return "15min";

        // Boşluk temizleme
        tf = tf.trim().toLowerCase();

        // Kısaltmaları TwelveData formatına çevir
        switch (tf) {
            case "m1":  tf = "1min"; break;
            case "m5":  tf = "5min"; break;
            case "m15": tf = "15min"; break;
            case "m30": tf = "30min"; break;
            case "m45": tf = "45min"; break;
            case "h1":  tf = "1h"; break;
            case "h2":  tf = "2h"; break;
            case "h4":  tf = "4h"; break;
            case "h8":  tf = "8h"; break;
            case "d1":  tf = "1day"; break;
            case "w1":  tf = "1week"; break;
            case "mn1": tf = "1month"; break;
        }

        // Destekleniyorsa dön, değilse fallback
        if (SUPPORTED_INTERVALS.contains(tf)) {
            return tf;
        }
        return "15min";
    }

    /**
     * Verilen zaman damgasını, seçilen interval'e göre son kapanışa hizalar.
     */
    public static Instant alignedToClosed(Instant time, String interval) {
        interval = normalizeInterval(interval);
        ZonedDateTime zdt = time.atZone(ZoneOffset.UTC);

        switch (interval) {
            case "1min":
                return zdt.withSecond(0).withNano(0).toInstant();
            case "5min":
                return zdt.withMinute((zdt.getMinute() / 5) * 5).withSecond(0).withNano(0).toInstant();
            case "15min":
                return zdt.withMinute((zdt.getMinute() / 15) * 15).withSecond(0).withNano(0).toInstant();
            case "30min":
                return zdt.withMinute((zdt.getMinute() / 30) * 30).withSecond(0).withNano(0).toInstant();
            case "45min":
                return zdt.withMinute((zdt.getMinute() / 45) * 45).withSecond(0).withNano(0).toInstant();
            case "1h":
                return zdt.withMinute(0).withSecond(0).withNano(0).toInstant();
            case "2h":
                return zdt.withHour((zdt.getHour() / 2) * 2).withMinute(0).withSecond(0).withNano(0).toInstant();
            case "4h":
                return zdt.withHour((zdt.getHour() / 4) * 4).withMinute(0).withSecond(0).withNano(0).toInstant();
            case "8h":
                return zdt.withHour((zdt.getHour() / 8) * 8).withMinute(0).withSecond(0).withNano(0).toInstant();
            case "1day":
                return zdt.truncatedTo(ChronoUnit.DAYS).toInstant();
            case "1week":
                return zdt.with(ChronoField.DAY_OF_WEEK, 1)
                        .truncatedTo(ChronoUnit.DAYS).toInstant();
            case "1month":
                return zdt.withDayOfMonth(1)
                        .truncatedTo(ChronoUnit.DAYS).toInstant();
            default:
                return zdt.withMinute((zdt.getMinute() / 15) * 15).withSecond(0).withNano(0).toInstant();
        }
    }

    public static int requiredBars(String interval) {
        interval = normalizeInterval(interval);

        switch (interval) {
            case "1min":   return 150;
            case "5min":   return 150;
            case "15min":  return 200;
            case "30min":  return 200;
            case "45min":  return 200;
            case "1h":     return 300;
            case "2h":     return 300;
            case "4h":     return 400;
            case "8h":     return 400;
            case "1day":   return 500;
            case "1week":  return 600;
            case "1month": return 800;
            default:       return 150;
        }
    }

}
