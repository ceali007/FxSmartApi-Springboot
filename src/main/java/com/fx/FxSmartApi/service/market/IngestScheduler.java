// src/main/java/com/fx/FxSmartApi/service/market/IngestScheduler.java
package com.fx.FxSmartApi.service.market;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(prefix = "fx.ingest", name = "enabled", havingValue = "true")
@Component
@EnableScheduling
public class IngestScheduler {
    private static final Logger log = LoggerFactory.getLogger(IngestScheduler.class);
    private final IngestRunner runner;

    public IngestScheduler(IngestRunner runner) { this.runner = runner; }

    @Scheduled(fixedDelayString = "${fx.ingest.fixedDelayMs:60000}")
    public void run() {
        var rep = runner.runOnce();
        if (rep.getTotalInserted() > 0) {
            log.info("Ingest tick inserted={} items={}", rep.getTotalInserted(), rep.getItems().size());
        }
    }
}
