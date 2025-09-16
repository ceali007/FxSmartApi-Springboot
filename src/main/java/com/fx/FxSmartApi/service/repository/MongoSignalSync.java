package com.fx.FxSmartApi.service.repository;

import com.fx.FxSmartApi.mapper.SignalMapper;
import com.fx.FxSmartApi.service.advisor.engine.EngineResult;
import com.fx.FxSmartApi.service.advisor.engine.SignalSync;
import org.springframework.stereotype.Service;

@Service
public class MongoSignalSync implements SignalSync {
    private final SignalRepository repo;
    public MongoSignalSync(SignalRepository repo) { this.repo = repo; }

    @Override
    public void upsert(EngineResult result) {
        var doc = SignalMapper.toDoc(result);
        repo.upsertUnique(doc); // <-- Artık derlenir/çalışır
    }
}