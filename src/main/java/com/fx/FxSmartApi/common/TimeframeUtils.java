package com.fx.FxSmartApi.common;

import com.fx.FxSmartApi.model.Candle;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class TimeframeUtils {

    /** Killzone kontrolü — Instant (UTC, close-time) ile */
    public static boolean inNewYorkAM(Instant closeUtc,
                                      String tz,
                                      int sh, int sm,
                                      int eh, int em) {
        ZoneId zone = ZoneId.of(tz);
        ZonedDateTime z = closeUtc.atZone(zone);
        LocalTime t = z.toLocalTime();
        LocalTime start = LocalTime.of(sh, sm);
        LocalTime end   = LocalTime.of(eh, em);
        return !t.isBefore(start) && t.isBefore(end);
    }

    /** Geriye uyumluluk: epochMillis overload (Instant'a delege eder) */
    public static boolean inNewYorkAM(long epochMillis,
                                      String tz,
                                      int sh, int sm,
                                      int eh, int em) {
        return inNewYorkAM(Instant.ofEpochMilli(epochMillis), tz, sh, sm, eh, em);
    }

    /**
     * Basit M1 → N dakika agregasyon (close-time mantığı).
     * Çıktı Candle.time = agregat M*N dakikalık mumun KAPANIŞ zamanı (UTC).
     */
    public static List<Candle> aggregate(List<Candle> m1, int minutes) {
        if (m1 == null || m1.isEmpty() || minutes <= 1) return m1;

        final long intervalMs = minutes * 60_000L;
        List<Candle> out = new ArrayList<>();

        long currentBucket = -1L;           // bucket index (close-time / interval)
        double o = 0, h = 0, l = 0, c = 0;
        double v = 0;

        for (Candle x : m1) {
            long tMs = x.getTime().toEpochMilli();    // M1 close-time (UTC)
            long bucket = Math.floorDiv(tMs, intervalMs);

            if (currentBucket == -1L) {
                // ilk bar
                currentBucket = bucket;
                o = x.getOpen();
                h = x.getHigh();
                l = x.getLow();
                c = x.getClose();
                v = x.getVolume();
            } else if (bucket == currentBucket) {
                // aynı kovada topla
                h = Math.max(h, x.getHigh());
                l = Math.min(l, x.getLow());
                c = x.getClose();
                v += x.getVolume();
            } else {
                // önceki kovayı yaz (close-time = bucketIndex * interval)
                long closeMs = currentBucket * intervalMs;
                out.add(new Candle(Instant.ofEpochMilli(closeMs), o, h, l, c, v));

                // yeni kova başlat
                currentBucket = bucket;
                o = x.getOpen();
                h = x.getHigh();
                l = x.getLow();
                c = x.getClose();
                v = x.getVolume();
            }
        }

        // son kovayı yaz
        if (currentBucket != -1L) {
            long closeMs = currentBucket * intervalMs;
            out.add(new Candle(Instant.ofEpochMilli(closeMs), o, h, l, c, v));
        }

        return out;
        // Not: close-time'ın tam dakikaya oturması için girdideki M1'lerin close-time'ı
        // dakika sınırlarına denk gelmelidir (TwelveData M1 tipik olarak böyledir).
    }
}
