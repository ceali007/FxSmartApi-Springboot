// src/main/java/com/fx/FxSmartApi/service/market/IngestRunner.java
package com.fx.FxSmartApi.service.market;

import com.fx.FxSmartApi.config.IngestProperties;

import com.fx.FxSmartApi.model.entity.StrategyTimeframe;
import com.fx.FxSmartApi.model.entity.SymbolDoc;
import com.fx.FxSmartApi.service.repository.StrategyTimeframeRepository;
import com.fx.FxSmartApi.service.repository.SymbolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IngestRunner {
    private static final Logger log = LoggerFactory.getLogger(IngestRunner.class);

    private final IngestProperties props;
    private final SymbolRepository symbolRepo;
    private final StrategyTimeframeRepository tfRepo;
    private final TwelveDataCandleProvider provider;

    public IngestRunner(IngestProperties props,
                        SymbolRepository symbolRepo,
                        StrategyTimeframeRepository tfRepo,
                        TwelveDataCandleProvider provider) {
        this.props = props;
        this.symbolRepo = symbolRepo;
        this.tfRepo = tfRepo;
        this.provider = provider;
    }

    // Tüm aktif semboller × tüm TF’ler
    public IngestReport runOnce() {
        List<SymbolDoc> actives = symbolRepo.findByIsActiveTrue();
        List<StrategyTimeframe> tfs = tfRepo.findAll();

        int total = 0;
        List<Item> items = new ArrayList<>();
        for (SymbolDoc s : actives) {
            for (StrategyTimeframe tf : tfs) {
                int lookback = tf.getRequiredBars() > 0 ? tf.getRequiredBars() : props.getDefaultRequiredBars();
                int n = 0;
                try {
                    n = provider.fetchAndUpsert(s.getVendorCode(), tf.getInterval(), lookback);
                } catch (Exception e) {
                    log.warn("Ingest failed {} {} -> {}", s.getCode(), tf.getInterval(), e.toString());
                }
                items.add(new Item(s.getCode(), tf.getInterval(), lookback, n));
                total += n;
            }
        }
        return new IngestReport(total, items);
    }

    // Tek sembol + TF
    public int runFor(String symbolCode, String interval, Integer lookbackOverride) {
        SymbolDoc sym = symbolRepo.findByCode(symbolCode)
                .orElseThrow(() -> new IllegalArgumentException("Symbol not found: " + symbolCode));

        int lookback = (lookbackOverride != null) ? lookbackOverride :
                tfRepo.findByInterval(interval)
                        .map(tf -> Math.max(tf.getRequiredBars(), 1))
                        .orElse(props.getDefaultRequiredBars());

        return provider.fetchAndUpsert(sym.getVendorCode(), interval, lookback);
    }

    // ---- DTO'lar ----
    public static class IngestReport {
        private final int totalInserted;
        private final List<Item> items;
        public IngestReport(int totalInserted, List<Item> items) { this.totalInserted = totalInserted; this.items = items; }
        public int getTotalInserted() { return totalInserted; }
        public List<Item> getItems() { return items; }
    }
    public static class Item {
        private final String symbol; private final String timeframe;
        private final int lookbackUsed; private final int inserted;
        public Item(String symbol, String timeframe, int lookbackUsed, int inserted) {
            this.symbol = symbol; this.timeframe = timeframe; this.lookbackUsed = lookbackUsed; this.inserted = inserted;
        }
        public String getSymbol() { return symbol; }
        public String getTimeframe() { return timeframe; }
        public int getLookbackUsed() { return lookbackUsed; }
        public int getInserted() { return inserted; }
    }
}
