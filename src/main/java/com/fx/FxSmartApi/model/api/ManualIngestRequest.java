package com.fx.FxSmartApi.model.api;

public class ManualIngestRequest {
    private String symbol;
    private String timeframe;
    private Integer lookbackBars;

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
    public Integer getLookbackBars() { return lookbackBars; }
    public void setLookbackBars(Integer lookbackBars) { this.lookbackBars = lookbackBars; }
}