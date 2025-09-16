// src/main/java/com/fx/FxSmartApi/controller/SignalController.java
package com.fx.FxSmartApi.controller;

import com.fx.FxSmartApi.model.api.ManualIngestRequest;
import com.fx.FxSmartApi.model.api.ManualIngestResponse;
import com.fx.FxSmartApi.model.entity.StrategyTimeframe;
import com.fx.FxSmartApi.service.data.StrategyTimeframeService;
import com.fx.FxSmartApi.service.market.IngestRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/signals")
public class SignalController {
    private final IngestRunner runner;
    private final StrategyTimeframeService strategyTimeframeService;

    public SignalController(IngestRunner runner, StrategyTimeframeService strategyTimeframeService) { this.runner = runner;
        this.strategyTimeframeService = strategyTimeframeService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<ManualIngestResponse> manualIngest(@RequestBody ManualIngestRequest req) {
        int inserted = runner.runFor(req.getSymbol(), req.getTimeframe(), req.getLookbackBars());
        return ResponseEntity.ok(new ManualIngestResponse(req.getSymbol(), req.getTimeframe(),
                (req.getLookbackBars()==null? -1 : req.getLookbackBars()), inserted));
    }

    @PostMapping("/ingest/all")
    public ResponseEntity<IngestRunner.IngestReport> runAllNow() {
        return ResponseEntity.ok(runner.runOnce());
    }

}
