package com.fx.FxSmartApi.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "strategy_timeframe")
@CompoundIndex(name = "sym_interval_unique", def = "{'symbol': 1, 'interval': 1}", unique = true)
public class StrategyTimeframe {
    @Id
    private String id;
    private String interval;          // "1min", "5min", "15min", "30min", "1h", "4h", "1day"
    private int requiredBars;         // normal/gündüz ingest
    private int requiredBarsExtended; // gece backfill hedefi

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public int getRequiredBars() {
        return requiredBars;
    }

    public void setRequiredBars(int requiredBars) {
        this.requiredBars = requiredBars;
    }

    public int getRequiredBarsExtended() {
        return requiredBarsExtended;
    }

    public void setRequiredBarsExtended(int requiredBarsExtended) {
        this.requiredBarsExtended = requiredBarsExtended;
    }
}
