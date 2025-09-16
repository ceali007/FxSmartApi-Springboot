package com.fx.FxSmartApi.service.repository;

import com.fx.FxSmartApi.model.entity.SignalDoc;
import com.fx.FxSmartApi.service.repository.SignalRepositoryCustom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;


@Repository
public class SignalRepositoryImpl implements SignalRepositoryCustom {
    private final MongoTemplate mongo;
    public SignalRepositoryImpl(MongoTemplate mongo) { this.mongo = mongo; }

    @Override
    public void upsertUnique(SignalDoc doc) {
        Query q = new Query(Criteria.where("symbol").is(doc.getSymbol())
                .and("strategy").is(doc.getStrategy())
                .and("timeframe").is(doc.getTimeframe())
                .and("tAligned").is(doc.gettAligned()));

        Instant now = Instant.now();
        Date expireAt = Date.from(doc.gettAligned().plusSeconds(daysFor(doc.getTimeframe()) * 24L * 3600L));

        Update u = new Update()
                .set("symbol", doc.getSymbol())
                .set("strategy", doc.getStrategy())
                .set("timeframe", doc.getTimeframe())
                .set("tAligned", doc.gettAligned())
                .set("side", doc.getSide())
                .set("entry", doc.getEntry())
                .set("sl", doc.getSl())
                .set("tp", doc.getTp())
                .set("risk", doc.getRisk())
                .set("reward", doc.getReward())
                .set("rr", doc.getRr())
                .set("updatedAt", now)
                .set("expireAt", expireAt)
                .setOnInsert("createdAt", now);

        mongo.upsert(q, u, SignalDoc.class);
    }

    private static int daysFor(String tf) {
        if (tf == null) return 90;
        return switch (tf.trim().toLowerCase()) {
            case "1m", "1min" -> 7;
            case "5m", "5min" -> 14;
            case "15m"       -> 30;
            case "1h"        -> 180;
            default          -> 90;
        };
    }
}
