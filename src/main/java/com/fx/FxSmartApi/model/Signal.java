package com.fx.FxSmartApi.model;

import com.fx.FxSmartApi.model.enums.SignalSide;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Signal {

    private String symbol;
    private String timeframe;
    private Instant time;              // UTC close-time
    private SignalSide side;           // buy/sell/flat
    private double entry;
    private double sl;
    private List<Double> tp = new ArrayList<>();

    private double risk;
    private double reward;
    private double rr;                 // 2.0 => "1:2"

    private List<String> reasons = new ArrayList<>();

    // --- getters/setters (kısaltıldı) ---
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
    public Instant getTime() { return time; }
    public void setTime(Instant time) { this.time = time; }
    public SignalSide getSide() { return side; }
    public void setSide(SignalSide side) { this.side = side; }
    public double getEntry() { return entry; }
    public void setEntry(double entry) { this.entry = entry; }
    public double getSl() { return sl; }
    public void setSl(double sl) { this.sl = sl; }
    public List<Double> getTp() { return tp; }
    public void setTp(List<Double> tp) { this.tp = tp; }
    public double getRisk() { return risk; }
    public double getReward() { return reward; }
    public double getRr() { return rr; }
    public List<String> getReasons() { return reasons; }
    public void addReason(String reason) { this.reasons.add(reason); }

    public void setRR(double risk, double reward) {
        this.risk = risk;
        this.reward = reward;
        this.rr = (risk > 0) ? (reward / risk) : 0.0;
    }

    /** Strateji sinyal üretmediyse "flat" durumunu temsil eden yardımcı */
    public static Signal flat(Instant t) {
        Signal s = new Signal();
        s.setTime(t);
        s.setSide(SignalSide.FLAT);   // enum’da flat yoksa bu satırı kaldır, sadece reason bırak
        s.addReason("flat");
        return s;
    }

    @Override
    public String toString() {
        return "Signal{" +
                "symbol='" + symbol + '\'' +
                ", timeframe='" + timeframe + '\'' +
                ", time=" + time +
                ", side=" + side +
                ", entry=" + entry +
                ", sl=" + sl +
                ", tp=" + tp +
                ", rr=" + String.format("1:%.2f", rr) +
                ", reasons=" + reasons +
                '}';
    }

    public void setRisk(double risk) {
        this.risk = risk;
    }

    public void setReward(double reward) {
        this.reward = reward;
    }

    public void setRr(double rr) {
        this.rr = rr;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }
}
