package com.fx.FxSmartApi.model;

public class StrategyConfig {
    public boolean useMidnightOpen = true;
    public boolean useOpen0830 = true;
    public String nyTz = "America/New_York";

    public double oteMin = 0.62;
    public double oteMax = 0.79;
    public double rrMin  = 1.5;

    // killzone (NY-AM)
    public int nyStartHour = 8, nyStartMin = 30;
    public int nyEndHour   = 11, nyEndMin   = 0;

    // toleranslar
    public int swingLeft = 2, swingRight = 2;
    public double equalTol = 0.0002;   // FX için ~2 pip, XAU/XAG/indeks için enstrümana göre ölçekleyin
}
