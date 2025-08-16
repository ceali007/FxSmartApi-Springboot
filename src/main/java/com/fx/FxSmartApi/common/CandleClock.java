package com.fx.FxSmartApi.common;

import java.time.Duration;
import java.time.Instant;

public class CandleClock {
    public static Duration tfToDuration(String tf) {
        var t = tf.toLowerCase().replace(" ", "");
        return switch (t) {
            case "1m","m1","1min" -> Duration.ofMinutes(1);
            case "5m","m5" -> Duration.ofMinutes(5);
            case "15m","m15" -> Duration.ofMinutes(15);
            case "30m","m30" -> Duration.ofMinutes(30);
            case "1h","h1" -> Duration.ofHours(1);
            case "4h","h4" -> Duration.ofHours(4);
            case "1d","1day","d1" -> Duration.ofDays(1);
            default -> throw new IllegalArgumentException("Unsupported timeframe: " + tf);
        };
    }

    /** nowUtc anını timeframe’e göre kapanmış mum kapanışına hizalar. */
    public static Instant alignToClosed(Instant nowUtc, String timeframe, int graceSeconds) {
        var dur = tfToDuration(timeframe);
        long ms = nowUtc.toEpochMilli();
        long mod = ms % dur.toMillis();
        var aligned = Instant.ofEpochMilli(ms - mod);
        if (Duration.between(aligned, nowUtc).getSeconds() < graceSeconds) {
            aligned = aligned.minus(dur);
        }
        return aligned;
    }
}