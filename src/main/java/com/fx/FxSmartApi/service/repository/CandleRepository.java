package com.fx.FxSmartApi.service.repository;

import com.fx.FxSmartApi.model.entity.CandleDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CandleRepository extends MongoRepository<CandleDoc, String> {
    Optional<CandleDoc> findTopBySymbolAndTimeframeOrderByTsDesc(String symbol, String timeframe);
    List<CandleDoc> findTop500BySymbolAndTimeframeOrderByTsDesc(String symbol, String timeframe);
    boolean existsBySymbolAndTimeframeAndTs(String symbol, String timeframe, Instant ts);
}