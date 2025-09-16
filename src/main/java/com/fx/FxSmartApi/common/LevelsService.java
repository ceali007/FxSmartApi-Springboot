package com.fx.FxSmartApi.common;

import com.fx.FxSmartApi.model.dto.Candle;

import java.time.*;
import java.util.List;

public class LevelsService {

    /** 00:00 America/New_York mumunun OPEN fiyatı (referans gün) */
    public static Double midnightOpen(List<Candle> m1, String nyTz, Instant refCloseUtc) {
        if (m1 == null || m1.isEmpty()) return null;

        ZoneId zone = ZoneId.of(nyTz);
        LocalDate day = refCloseUtc.atZone(zone).toLocalDate();  // referans gün (NY tarihi)

        long start = ZonedDateTime.of(day, LocalTime.MIDNIGHT, zone)
                .toInstant().toEpochMilli();
        long end   = start + 60_000; // 1 dk aralığı

        for (Candle c : m1) {
            long t = c.getTime().toEpochMilli();
            if (t >= start && t < end) {
                return c.getOpen();
            }
        }
        return null;
    }

    /** 08:30 America/New_York mumunun OPEN fiyatı (referans gün) */
    public static Double open0830(List<Candle> m1, String nyTz, Instant refCloseUtc) {
        if (m1 == null || m1.isEmpty()) return null;

        ZoneId zone = ZoneId.of(nyTz);
        LocalDate day = refCloseUtc.atZone(zone).toLocalDate();

        long start = ZonedDateTime.of(day, LocalTime.of(8, 30), zone)
                .toInstant().toEpochMilli();
        long end   = start + 60_000;

        for (Candle c : m1) {
            long t = c.getTime().toEpochMilli();
            if (t >= start && t < end) {
                return c.getOpen();
            }
        }
        return null;
    }
}
