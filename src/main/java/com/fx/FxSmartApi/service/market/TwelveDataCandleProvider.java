// src/main/java/com/fxsmart/api/market/TwelveDataCandleProvider.java
package com.fx.FxSmartApi.service.market;

import com.fx.FxSmartApi.model.Candle;
import com.fx.FxSmartApi.service.advisor.engine.CandleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Gerçek entegrasyon için iskelet. Free planı korumak adına cache eklemen önerilir. */
@Service
public class TwelveDataCandleProvider implements CandleProvider {

    private final WebClient client;

    // Spring Boot otomatik WebClient.Builder bean sağlar; ctor injection ile alıyoruz
    public TwelveDataCandleProvider(WebClient.Builder builder) {
        this.client = builder.baseUrl("https://api.twelvedata.com").build();
    }

    @Value("${twelvedata.apiKey:YOUR_API_KEY}")
    private String apiKey;

    @Override
    public List<Candle> fetchCandles(String symbol, String timeframe, int limit, Instant untilCloseUtc) {
        try {
            var uri = "/time_series?symbol=" + symbol +
                    "&interval=" + timeframe +
                    "&outputsize=" + limit +
                    "&apikey=" + apiKey;

            Map<?, ?> json = client.get().uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            var values = (List<Map<String, String>>) json.get("values");
            var out = new ArrayList<Candle>();
            if (values == null) return out;

            System.out.println("TwelveData URL: " + uri);
            System.out.println("JSON Response: " + json);



            // (Genelde yeni->eski gelir) eski->yeni sırası için tersten ekle
            for (int i = values.size() - 1; i >= 0; i--) {
                var v = values.get(i);
                // "2025-08-15 12:30:00" → "2025-08-15T12:30:00Z"
                var iso = v.get("datetime").replace(" ", "T") + "Z";
                Instant t = Instant.parse(iso);

                double open  = Double.parseDouble(v.get("open"));
                double high  = Double.parseDouble(v.get("high"));
                double low   = Double.parseDouble(v.get("low"));
                double close = Double.parseDouble(v.get("close"));
                double vol   = Double.parseDouble(v.getOrDefault("volume", "0"));

                if (untilCloseUtc == null || !t.isAfter(untilCloseUtc)) {
                    out.add(new Candle(t, open, high, low, close, vol));
                }
            }
            if (out.size() > limit) {
                out = new ArrayList<>(out.subList(out.size() - limit, out.size()));
            }
            return out;
        } catch (Exception e) {
            // TODO: log
            System.out.println("TwelveData Fetch Error: " + e.getMessage());
            return List.of();
        }
    }
}