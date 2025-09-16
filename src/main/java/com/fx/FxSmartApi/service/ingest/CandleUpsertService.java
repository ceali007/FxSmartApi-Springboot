package com.fx.FxSmartApi.service.ingest;

import com.fx.FxSmartApi.model.entity.CandleDoc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class CandleUpsertService {

    private final MongoTemplate mongo;

    public CandleUpsertService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    // Retention günleri (TF’e göre). Her iki yazımı da destekliyoruz: 1m/1min, 5m/5min...
    private static final Map<String, Integer> RETENTION_DAYS = Map.ofEntries(
            Map.entry("1m", 1),    Map.entry("1min", 1),
            Map.entry("5m", 3),    Map.entry("5min", 3),
            Map.entry("15m", 7),   Map.entry("15min", 7),
            Map.entry("1h", 90)
    );

    private static int daysFor(String timeframe) {
        if (timeframe == null) return 30;
        String key = timeframe.trim().toLowerCase();
        return RETENTION_DAYS.getOrDefault(key, 30);
    }

    /** Tek bir mumu idempotent upsert eder. */
    public void saveOrUpdate(
            String provider, String symbol, String timeframe,
            Instant tsUtc, double open, double high, double low, double close, double volume
    ) {
        int days = daysFor(timeframe);
        Date expireAt = Date.from(tsUtc.plusSeconds(days * 24L * 3600L));
        Instant now = Instant.now();

        // Unique key: (provider, symbol, timeframe, ts)
        Query q = new Query(Criteria.where("provider").is(provider)
                .and("symbol").is(symbol)
                .and("timeframe").is(timeframe)
                .and("ts").is(tsUtc));

        Update u = new Update()
                .set("provider", provider)
                .set("symbol", symbol)
                .set("timeframe", timeframe)
                .set("ts", tsUtc)
                .set("open", open)
                .set("high", high)
                .set("low", low)
                .set("close", close)
                .set("volume", volume)
                .set("expireAt", expireAt)
                .set("updatedAt", now)
                .setOnInsert("createdAt", now);

        mongo.upsert(q, u, CandleDoc.class);
    }

    /** Batch idempotent upsert (listeyi hızlıca işler). */
    public void saveOrUpdateAll(
            String provider, String symbol, String timeframe,
            List<CandleInput> candles
    ) {
        if (candles == null || candles.isEmpty()) return;
        for (var c : candles) {
            saveOrUpdate(provider, symbol, timeframe,
                    c.tsUtc(), c.open(), c.high(), c.low(), c.close(), c.volume());
        }
    }

    /** Basit taşıyıcı record: toplu yüklemelerde kullanışlı. */
    public record CandleInput(
            Instant tsUtc, double open, double high, double low, double close, double volume) {}
}
