
package com.fx.FxSmartApi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;

@ConfigurationProperties(prefix = "fx")
public class FxProperties {
    /** Örn: "America/New_York" */
    private String tz = "America/New_York";

    private Ingest ingest = new Ingest();

    public String getTz() { return tz; }
    public void setTz(String tz) { this.tz = tz; }
    public ZoneId zoneId() { return ZoneId.of(tz); }

    public Ingest getIngest() { return ingest; }
    public void setIngest(Ingest ingest) { this.ingest = ingest; }

    public static class Ingest {
        private long fixedDelayMs = 60000;
        private int safetyRpm = 50;
        private int maxConcurrent = 1;
        private int outputSize = 1;
        private boolean enabled = true;

        public long getFixedDelayMs() { return fixedDelayMs; }
        public void setFixedDelayMs(long v) { this.fixedDelayMs = v; }
        public int getSafetyRpm() { return safetyRpm; }
        public void setSafetyRpm(int v) { this.safetyRpm = v; }
        public int getMaxConcurrent() { return maxConcurrent; }
        public void setMaxConcurrent(int v) { this.maxConcurrent = v; }
        public int getOutputSize() { return outputSize; }
        public void setOutputSize(int v) { this.outputSize = v; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
