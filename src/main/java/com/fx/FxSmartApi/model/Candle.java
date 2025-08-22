package com.fx.FxSmartApi.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class Candle {
    private Instant time; // kapanış anı (UTC)
    private double open, high, low, close, volume;

    public Candle() {}
    public Candle(Instant time, double open, double high, double low, double close, double volume) {
        this.time = time; this.open = open; this.high = high; this.low = low; this.close = close; this.volume = volume;
    }

    public Instant getTime() { return time; }
    public double getOpen() { return open; }
    public double getHigh() { return high; }
    public double getLow()  { return low; }
    public double getClose(){ return close; }
    public double getVolume(){ return volume; }

    public void setTime(Instant time) { this.time = time; }
    public void setOpen(double open) { this.open = open; }
    public void setHigh(double high) { this.high = high; }
    public void setLow(double low) { this.low = low; }
    public void setClose(double close) { this.close = close; }
    public void setVolume(double volume) { this.volume = volume; }

    /** Barın kapanış zamanını epoch millis olarak döndürür. */
    public static long closeMillis(Candle c) {
        Instant t = c.getTime();               // Instant (UTC, close time)
        return t.toEpochMilli();
    }

    /** Barın kapanış zamanını verilen TZ'de döndürür. */
    public static ZonedDateTime closeZdt(Candle c, String tz) {
        return c.getTime().atZone(ZoneId.of(tz));
    }
}