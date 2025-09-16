// com/fx/FxSmartApi/service/repository/StrategyTimeframeRepository.java
package com.fx.FxSmartApi.service.repository;

import com.fx.FxSmartApi.model.entity.StrategyTimeframe;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface StrategyTimeframeRepository extends MongoRepository<StrategyTimeframe, String> {
    Optional<StrategyTimeframe> findByInterval(String interval);
}
