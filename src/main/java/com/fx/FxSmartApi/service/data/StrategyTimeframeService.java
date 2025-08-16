package com.fx.FxSmartApi.service.data;

import com.fx.FxSmartApi.model.StrategyTimeframe;
import com.fx.FxSmartApi.service.repository.StrategyTimeframeRepository;
import org.springframework.stereotype.Service;

@Service
public class StrategyTimeframeService {
    private final StrategyTimeframeRepository repo;

    public StrategyTimeframeService(StrategyTimeframeRepository repo) {
        this.repo = repo;
    }

    public int getRequiredBars(String interval) {

        return repo.findByInterval(interval)
                .map(StrategyTimeframe::getRequiredBars)
               .orElse(200); // fallback default
    }
}
