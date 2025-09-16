package com.fx.FxSmartApi.config;

import com.fx.FxSmartApi.model.entity.CandleDoc;
import com.fx.FxSmartApi.model.entity.SignalDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.time.Duration;
import java.util.List;

@Component
public class MongoTtlInitializer {

    private static final Logger log = LoggerFactory.getLogger(MongoTtlInitializer.class);
    private final MongoTemplate mongo;

    public MongoTtlInitializer(MongoTemplate mongo) { this.mongo = mongo; }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureTtlIndexesWithRetry() {
        int maxAttempts = 5; long backoffMs = 2000L;
        for (int i = 1; i <= maxAttempts; i++) {
            try {
                ensureTtlIndex(mongo.indexOps(CandleDoc.class), "ttl_candles");
                ensureTtlIndex(mongo.indexOps(SignalDoc.class), "ttl_signals");
                log.info("TTL index check completed.");
                return;
            } catch (Exception ex) {
                log.warn("TTL index setup attempt {}/{} failed: {}", i, maxAttempts, ex.getMessage());
                if (i == maxAttempts) throw ex;
                try { Thread.sleep(backoffMs * i); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); return;
                }
            }
        }
    }

    private void ensureTtlIndex(IndexOperations ops, String desiredName) {
        var list = ops.getIndexInfo();

        // 1) İstediğimiz isimde doğru index zaten var mı?
        var byName = list.stream().filter(ix -> desiredName.equals(ix.getName())).findFirst().orElse(null);
        if (byName != null) {
            boolean keyOk = byName.getIndexFields().size() == 1
                    && "expireAt".equals(byName.getIndexFields().get(0).getKey());
            boolean ttlOk = byName.getExpireAfter().isPresent() && byName.getExpireAfter().get().isZero();
            if (keyOk && ttlOk) return;             // her şey yolunda
            ops.dropIndex(desiredName);              // aynı ad yanlışsa düşür
        }

        // 2) Aynı key + TTL=0 ile VARSA (adı farklı olsa bile) kabul et ve çık
        var anyTtlOnExpireAt = list.stream().filter(ix -> {
            boolean keyOk = ix.getIndexFields().size() == 1
                    && "expireAt".equals(ix.getIndexFields().get(0).getKey());
            boolean ttlOk = ix.getExpireAfter().isPresent() && ix.getExpireAfter().get().isZero();
            return keyOk && ttlOk;
        }).findFirst().orElse(null);

        if (anyTtlOnExpireAt != null) {
            // Adı farklı ama işlevi doğru → oluşturma, çakışma çıkmasın
            return;
        }

        // 3) Hiç yoksa oluştur
        var index = new org.springframework.data.mongodb.core.index.Index()
                .on("expireAt", org.springframework.data.domain.Sort.Direction.ASC)
                .named(desiredName)
                .expire(java.time.Duration.ZERO);
        ops.createIndex(index);
    }

}
