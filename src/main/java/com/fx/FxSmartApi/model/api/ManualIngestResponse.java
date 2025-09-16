package com.fx.FxSmartApi.model.api;

public class ManualIngestResponse {
    private String symbol;
    private String timeframe;
    private int lookbackBarsUsed;
    private int inserted;

    public ManualIngestResponse(String symbol, String timeframe, int lookbackBarsUsed, int inserted) {
        this.symbol = symbol; this.timeframe = timeframe;
        this.lookbackBarsUsed = lookbackBarsUsed; this.inserted = inserted;
    }
    public String getSymbol() { return symbol; }
    public String getTimeframe() { return timeframe; }
    public int getLookbackBarsUsed() { return lookbackBarsUsed; }
    public int getInserted() { return inserted; }
}