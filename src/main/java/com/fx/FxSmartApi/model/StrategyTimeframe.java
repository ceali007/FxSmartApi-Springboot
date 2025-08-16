package com.fx.FxSmartApi.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "strategy_timeframe")
public class StrategyTimeframe {
    @Id
    private String id;
    private String interval;
    private int requiredBars;

    // getters & setters


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
}

