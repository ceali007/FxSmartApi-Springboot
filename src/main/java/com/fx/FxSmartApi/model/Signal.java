package com.fx.FxSmartApi.model;

import com.fx.FxSmartApi.model.enums.SignalSide;

import java.util.Map;

public class Signal {
    private SignalSide side = SignalSide.FLAT;
    private Double stopLoss;
    private Double takeProfit;
    private double confidence = 0.0;
    private String strategy = "NA";
    private Map<String, Object> meta;

    public Signal() {}
    public Signal(SignalSide side, Double sl, Double tp, double conf, String strategy, Map<String,Object> meta) {
        this.side = side; this.stopLoss = sl; this.takeProfit = tp; this.confidence = conf; this.strategy = strategy; this.meta = meta;
    }

    public static Signal flat() { return new Signal(); }

    public SignalSide getSide() { return side; }
    public Double getStopLoss() { return stopLoss; }
    public Double getTakeProfit() { return takeProfit; }
    public double getConfidence() { return confidence; }
    public String getStrategy() { return strategy; }
    public Map<String, Object> getMeta() { return meta; }

    public void setSide(SignalSide side) { this.side = side; }
    public void setStopLoss(Double stopLoss) { this.stopLoss = stopLoss; }
    public void setTakeProfit(Double takeProfit) { this.takeProfit = takeProfit; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public void setMeta(Map<String, Object> meta) { this.meta = meta; }
}