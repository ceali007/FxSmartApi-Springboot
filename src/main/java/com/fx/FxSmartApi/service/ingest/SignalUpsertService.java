package com.fx.FxSmartApi.service.ingest;

import com.fx.FxSmartApi.mapper.SignalMapper;
import com.fx.FxSmartApi.service.advisor.engine.EngineResult;
import com.fx.FxSmartApi.service.advisor.engine.SignalSync;
import com.fx.FxSmartApi.service.repository.SignalRepository;
import com.mongodb.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class SignalUpsertService implements SignalSync {

    private final SignalRepository repo;

    public SignalUpsertService(SignalRepository repo) {
        this.repo = repo;
    }

    // örnek retention (gün) – istediğin gibi ayarla
    private static final Map<String, Integer> RETENTION_DAYS = Map.of(
            "1m", 7,
            "5m", 14,
            "15m", 30,
            "1h", 180
    );

    private static int daysFor(String timeframe) {
        if (timeframe == null) return 90;
        String key = timeframe.trim().toLowerCase();
        return RETENTION_DAYS.getOrDefault(key, 90);
    }

    @Override
    public void upsert(EngineResult er) {
        var doc = SignalMapper.toDoc(er);          // EngineResult -> SignalDoc
        int days = daysFor(doc.getTimeframe());
        var expireAt = Date.from(doc.gettAligned().plusSeconds(days * 24L * 3600L));

        doc.setUpdatedAt(Instant.now());
        if (doc.getCreatedAt() == null) doc.setCreatedAt(Instant.now());
        // expireAt alanını SignalDoc’a ekleyin (TTL index ile)
        // doc.setExpireAt(expireAt);

        // Unique key: (symbol, strategy, timeframe, tAligned)
        try {
            repo.upsertUnique(doc);                 // MongoTemplate upsert
        } catch (DuplicateKeyException ignore) {
            // idempotent
        }
    }
}