// src/main/java/com/fx/FxSmartApi/config/IngestProperties.java
package com.fx.FxSmartApi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fx.ingest")
public class IngestProperties {
    private long fixedDelayMs = 60000;
    private int safetyRpm = 110;
    private int defaultRequiredBars = 150;

    public long getFixedDelayMs() { return fixedDelayMs; }
    public void setFixedDelayMs(long fixedDelayMs) { this.fixedDelayMs = fixedDelayMs; }

    public int getSafetyRpm() { return safetyRpm; }
    public void setSafetyRpm(int safetyRpm) { this.safetyRpm = safetyRpm; }

    public int getDefaultRequiredBars() { return defaultRequiredBars; }
    public void setDefaultRequiredBars(int defaultRequiredBars) { this.defaultRequiredBars = defaultRequiredBars; }
}
