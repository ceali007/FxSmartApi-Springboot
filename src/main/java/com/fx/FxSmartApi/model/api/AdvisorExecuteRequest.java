package com.fx.FxSmartApi.model.api;

import java.util.List;

public class AdvisorExecuteRequest {

    private String at;                 // ISO local, opsiyonel; yoksa sysdate
    private String tz;                 // varsayılan "Europe/Istanbul"
    private List<String> symbols;      // opsiyonel
    private List<String> strategies;   // opsiyonel
    private List<String> timeframes;   // opsiyonel
    private String mode;               // "manual" | "auto"

    // getters/setters
    public String getAt() { return at; }
    public void setAt(String at) { this.at = at; }
    public String getTz() { return tz; }
    public void setTz(String tz) { this.tz = tz; }
    public List<String> getSymbols() { return symbols; }
    public void setSymbols(List<String> symbols) { this.symbols = symbols; }
    public List<String> getStrategies() { return strategies; }
    public void setStrategies(List<String> strategies) { this.strategies = strategies; }
    public List<String> getTimeframes() { return timeframes; }
    public void setTimeframes(List<String> timeframes) { this.timeframes = timeframes; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

}
