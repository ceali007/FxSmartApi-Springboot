package com.fx.FxSmartApi.service.advisor.strategies;

import com.fx.FxSmartApi.model.dto.Candle;
import com.fx.FxSmartApi.model.dto.Signal;
import com.fx.FxSmartApi.model.dto.StrategyConfig;

import java.util.List;

public interface Strategy {

    /** Strateji adı — override etmezsen sınıf adını döner */
    default String name() {
        return this.getClass().getSimpleName();
    }

    /**
     * Çoklu TF değerlendirme.
     * Gerekli olmayan TF'ler için boş liste veya null gelebilir; strateji kendini korumalı.
     */
    List<Signal> evaluate(String symbol,
                          List<Candle> m1,
                          List<Candle> m5,
                          List<Candle> m15,
                          List<Candle> h4,
                          List<Candle> d1,
                          StrategyConfig cfg);
}
