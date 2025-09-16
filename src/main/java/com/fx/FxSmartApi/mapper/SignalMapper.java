package com.fx.FxSmartApi.mapper;

import com.fx.FxSmartApi.model.dto.Signal;
import com.fx.FxSmartApi.model.entity.SignalDoc;
import com.fx.FxSmartApi.service.advisor.engine.EngineResult;

import java.time.Instant;
import java.util.ArrayList;

public class SignalMapper {
    private SignalMapper(){}

    public static SignalDoc toDoc(EngineResult er) {
        // EngineResult: key(), strategy(), signal(), candleCloseUtc()
        Signal s = er.signal();

        SignalDoc d = new SignalDoc();
        d.setSymbol(er.key().symbol());
        d.setStrategy(er.strategy());
        d.setTimeframe(er.key().timeframe());

        // TF hizalı kapanış zamanı:
        d.settAligned(er.candleCloseUtc());

        // Signal alan eşlemesi
        d.setSide(s.getSide() != null ? s.getSide().name() : "FLAT");
        d.setEntry(s.getEntry());
        d.setSl(s.getSl());

        // tp (list) null ise boş liste kullan
        d.setTp(s.getTp() != null ? s.getTp() : new ArrayList<>());

        d.setRisk(s.getRisk());
        d.setReward(s.getReward());
        d.setRr(s.getRr());

        return d;
    }
}
