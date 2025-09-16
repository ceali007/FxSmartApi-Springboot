package com.fx.FxSmartApi.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Document("signals")
@CompoundIndex(name="ux_symbol_strategy_tf_t",
        def="{'symbol':1,'strategy':1,'timeframe':1,'tAligned':1}", unique=true)
public class SignalDoc {
    @Id
    private String id;

    private String symbol;
    private String strategy;
    private String timeframe;

    /** TF’e hizalanmış kapanış zamanı (UTC) */
    private Instant tAligned;

    /** buy/sell/flat */
    private String side;

    private Double entry;
    private Double sl;
    private List<Double> tp;     // çoklu TP desteği

    private Double risk;
    private Double reward;
    private Double rr;           // 2.0 => "1:2"

    private Instant createdAt;
    private Instant updatedAt;

    /** TTL alanı (Mongo 4.4+ için) */
    private Date expireAt;

    // --- getters & setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }

    public Instant gettAligned() { return tAligned; }
    public void settAligned(Instant tAligned) { this.tAligned = tAligned; }

    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }

    public Double getEntry() { return entry; }
    public void setEntry(Double entry) { this.entry = entry; }

    public Double getSl() { return sl; }
    public void setSl(Double sl) { this.sl = sl; }

    public List<Double> getTp() { return tp; }
    public void setTp(List<Double> tp) { this.tp = tp; }

    public Double getRisk() { return risk; }
    public void setRisk(Double risk) { this.risk = risk; }

    public Double getReward() { return reward; }
    public void setReward(Double reward) { this.reward = reward; }

    public Double getRr() { return rr; }
    public void setRr(Double rr) { this.rr = rr; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Date getExpireAt() { return expireAt; }
    public void setExpireAt(Date expireAt) { this.expireAt = expireAt; }
}