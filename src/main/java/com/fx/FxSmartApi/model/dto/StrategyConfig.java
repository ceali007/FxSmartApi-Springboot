package com.fx.FxSmartApi.model.dto;

/**
 * ICT (ve diğer) stratejiler için çalışma parametreleri.
 * Not: Alanlar bilinçli olarak public bırakıldı (hızlı kullanım için).
 */
public class StrategyConfig {

    // --- Referans çizgileri ---
    public boolean useMidnightOpen = true;    // 00:00 NY open
    public boolean useOpen0830     = true;    // 08:30 NY open
    public String  nyTz            = "America/New_York";

    // --- OTE ve R/R ---
    public double oteMin = 0.62;   // 62%
    public double oteMax = 0.79;   // 79%
    public double rrMin  = 1.5;    // minimum kabul edilecek R/R (örn. 1:1.5)

    // --- Killzone (NY-AM) ---
    public int nyStartHour = 8, nyStartMin = 30; // 08:30
    public int nyEndHour   = 11, nyEndMin   = 0; // 11:00

    // --- Toleranslar / yapı tespiti ---
    public int    swingLeft  = 2, swingRight = 2; // swing high/low penceresi
    public double equalTol   = 0.0002;            // equal highs/lows toleransı (sembole göre ölçekle)

    public StrategyConfig() { }

    public StrategyConfig(double rrMin) {
        this.rrMin = rrMin;
    }
}
