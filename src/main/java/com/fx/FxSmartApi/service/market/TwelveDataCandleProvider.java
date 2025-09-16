package com.fx.FxSmartApi.service.market;

import com.fx.FxSmartApi.config.FxProperties;
import com.fx.FxSmartApi.model.dto.Candle;
import com.fx.FxSmartApi.model.entity.CandleDoc;
import com.fx.FxSmartApi.service.advisor.engine.CandleProvider;
import com.fx.FxSmartApi.service.ingest.CandleUpsertService;
import com.fx.FxSmartApi.service.repository.CandleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * TwelveData'dan zaman serisi çekerken ICT uyumu için {@code fx.tz} (varsayılan America/New_York)
 * kullanılır. TwelveData 'timezone' paramına göre 'datetime' string döndürür; biz bunu
 * o zoneda parse edip UTC Instant’a çevirerek DB’ye yazarız.
 */
@Service
public class TwelveDataCandleProvider implements CandleProvider {

    private static final Logger log = LoggerFactory.getLogger(TwelveDataCandleProvider.class);

    private final WebClient.Builder builder;
    private final CandleRepository candleRepo;
    private final CandleUpsertService upsertService;
    private final FxProperties fxProps;

    public TwelveDataCandleProvider(WebClient.Builder builder,
                                    CandleRepository candleRepo,
                                    CandleUpsertService upsertService,
                                    FxProperties fxProps) {
        this.builder = builder;
        this.candleRepo = candleRepo;
        this.upsertService = upsertService;
        this.fxProps = fxProps;
    }

    @org.springframework.beans.factory.annotation.Value("${twelvedata.api-key:${twelvedata.apiKey:YOUR_API_KEY}}")
    private String apiKey;

    @org.springframework.beans.factory.annotation.Value("${twelvedata.base-url:https://api.twelvedata.com}")
    private String baseUrl;

    private ZoneId tradingZone() { return fxProps.zoneId(); }

    // ---------------------------------------------------------------
    // CandleProvider
    // ---------------------------------------------------------------

    @Override
    public List<Candle> fetchCandles(String symbol, String timeframe, int limit, Instant untilCloseUtc) {
        try {
            List<Candle> raw = requestCandles(symbol, timeframe, limit, null, null, "ASC", tradingZone());
            if (untilCloseUtc != null) raw.removeIf(c -> c.getTime().isAfter(untilCloseUtc));
            if (raw.size() > limit) return new ArrayList<>(raw.subList(raw.size() - limit, raw.size()));
            return raw;
        } catch (Exception e) {
            log.warn("TwelveData fetchCandles error {} {} -> {}", symbol, timeframe, e.getMessage());
            return List.of();
        }
    }

    public List<Candle> fetchCandlesSince(String symbol, String timeframe, Instant sinceInclusiveUtc, int maxBars) {
        try {
            return requestCandles(symbol, timeframe, maxBars, sinceInclusiveUtc, Instant.now(), "ASC", tradingZone());
        } catch (Exception e) {
            log.warn("TwelveData fetchCandlesSince error {} {} -> {}", symbol, timeframe, e.getMessage());
            return List.of();
        }
    }

    /**
     * Veriyi çek + CandleUpsertService ile idempotent upsert.
     * DB'deki son bara göre geriden başlar, yeni barları NY zamanı bazlı alır ve UTC olarak saklar.
     */
    public int fetchAndUpsert(String vendorSymbol, String timeframe, int lookbackBars) {
        CandleDoc last = candleRepo.findTopBySymbolAndTimeframeOrderByTsDesc(vendorSymbol, timeframe).orElse(null);
        Instant since = (last != null) ? last.getTs() : Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);

        List<Candle> fetched = fetchCandlesSince(vendorSymbol, timeframe, since, lookbackBars);
        if (fetched.isEmpty()) return 0;

        int inserted = 0;
        for (Candle c : fetched) {
            boolean isNew = !candleRepo.existsBySymbolAndTimeframeAndTs(vendorSymbol, timeframe, c.getTime());

            upsertService.saveOrUpdate(
                    "TWELVEDATA",
                    vendorSymbol,
                    timeframe,
                    c.getTime(), // NY 'datetime' -> parseWithZone -> UTC Instant
                    c.getOpen(), c.getHigh(), c.getLow(), c.getClose(), c.getVolume()
            );

            if (isNew) inserted++;
        }
        if (inserted > 0) log.info("Upsert {} bars for {} {}", inserted, vendorSymbol, timeframe);
        return inserted;
    }

    // ---------------------------------------------------------------
    // HTTP + parse helpers (NY-time aware)
    // ---------------------------------------------------------------

    private List<Candle> requestCandles(String symbol,
                                        String interval,
                                        Integer outputSize,
                                        Instant startInclusiveUtc,
                                        Instant endExclusiveUtc,
                                        String orderAscDesc,
                                        ZoneId tz) {

        WebClient client = builder.baseUrl(baseUrl).filter(logRequest()).build();

        // TwelveData için interval normalize et
        String vendorInterval = mapInterval(interval);
        boolean isDaily = isDailyInterval(vendorInterval);

        // Günlükte tarih-only; intraday'de tarih+saat formatını kullan
        DateTimeFormatter F_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(tz);
        DateTimeFormatter F_DT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(tz);

        Map<String, Object> json = client.get().uri(uriBuilder -> {
                    var ub = uriBuilder.path("/time_series")
                            .queryParam("symbol", symbol)
                            .queryParam("interval", vendorInterval)              // <-- normalize interval
                            .queryParam("timezone", tz.getId())                 // örn: America/New_York
                            .queryParam("order", (orderAscDesc != null ? orderAscDesc : "ASC"))
                            .queryParam("dp", 8)
                            .queryParam("apikey", apiKey);

                    if (outputSize != null && outputSize > 0) {
                        ub.queryParam("outputsize", outputSize);
                    }
                    if (startInclusiveUtc != null) {
                        var z = startInclusiveUtc.atZone(ZoneOffset.UTC).withZoneSameInstant(tz);
                        ub.queryParam("start_date", (isDaily ? F_DATE : F_DT).format(z));
                    }
                    if (endExclusiveUtc != null) {
                        var z = endExclusiveUtc.atZone(ZoneOffset.UTC).withZoneSameInstant(tz);
                        ub.queryParam("end_date", (isDaily ? F_DATE : F_DT).format(z));
                    }
                    return ub.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                        resp -> mapError(resp).flatMap(msg -> Mono.error(new RuntimeException(msg))))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (json == null) return List.of();

        Object status = json.get("status");
        if (status != null && "error".equalsIgnoreCase(String.valueOf(status))) {
            String msg = (String) json.getOrDefault("message", "unknown error");
            throw new RuntimeException("TwelveData error: " + msg);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> values = (List<Map<String, Object>>) json.get("values");
        if (values == null) return new ArrayList<>();

        List<Candle> out = new ArrayList<>(values.size());
        for (Map<String, Object> v : values) {
            // TwelveData günlükte "yyyy-MM-dd", intraday'de "yyyy-MM-dd HH:mm:ss" döndürebilir.
            String dtStr = (String) v.get("datetime");
            Instant t = parseWithZoneFlexible(dtStr, tz);

            double open  = parseNum(v.get("open"));
            double high  = parseNum(v.get("high"));
            double low   = parseNum(v.get("low"));
            double close = parseNum(v.get("close"));
            double vol   = parseNum(v.get("volume"));
            out.add(new Candle(t, open, high, low, close, vol));
        }
        return out;
    }

    /** TwelveData interval normalizasyonu (iç formatınız -> vendor formatı). */
    private static String mapInterval(String tf) {
        if (tf == null) return null;
        String s = tf.trim().toLowerCase();
        return switch (s) {
            case "1m", "1min"     -> "1min";
            case "5m", "5min"     -> "5min";
            case "15m", "15min"   -> "15min";
            case "30m", "30min"   -> "30min";
            case "1h", "60min"    -> "1h";
            case "4h", "240min"   -> "4h";
            case "1d", "1day", "daily" -> "1day";
            default -> s; // bilinmeyen değerleri aynen geçir (örn: 2h vb. destekliyorsanız)
        };
    }

    /** Günlük mü? ("1day" / "...day") */
    private static boolean isDailyInterval(String interval) {
        if (interval == null) return false;
        String s = interval.trim().toLowerCase();
        return s.equals("1day") || s.equals("1d") || s.endsWith("day");
    }

    /** "yyyy-MM-dd" VE "yyyy-MM-dd HH:mm:ss" formatlarını TZ'e göre parse eder. */
    private static Instant parseWithZoneFlexible(String dt, ZoneId tz) {
        if (dt == null) return Instant.EPOCH;
        dt = dt.trim();

        // Sadece tarih (günlük) ise:
        if (dt.length() == 10 && dt.charAt(4) == '-' && dt.charAt(7) == '-') {
            LocalDate d = LocalDate.parse(dt, DateTimeFormatter.ISO_LOCAL_DATE);
            return d.atStartOfDay(tz).toInstant();
        }

        // Tarih+saat (intraday)
        DateTimeFormatter F_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime ldt = LocalDateTime.parse(dt, F_DT);
        return ldt.atZone(tz).toInstant();
    }




    /** "yyyy-MM-dd" VE "yyyy-MM-dd HH:mm:ss" formatlarını TZ'e göre parse eder. */
    private static Instant parseWithZone(String dt, ZoneId tz) {
        // TwelveData bazen "yyyy-MM-dd HH:mm:ss" (intraday), bazen "yyyy-MM-dd" (daily) döndürür.
        dt = dt.trim();

        // 1) Sadece tarih formatı mı? (örn: "2025-08-06")
        if (dt.length() == 10 && dt.charAt(4) == '-' && dt.charAt(7) == '-') {
            java.time.LocalDate d = java.time.LocalDate.parse(dt, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
            return d.atStartOfDay(tz).toInstant(); // NY günü başlangıcı → UTC Instant
        }

        // 2) Tarih + saat "yyyy-MM-dd HH:mm:ss"
        java.time.format.DateTimeFormatter F = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(dt, F);
        return ldt.atZone(tz).toInstant();
    }


    private static double parseNum(Object o) {
        if (o == null) return 0d;
        if (o instanceof Number n) return n.doubleValue();
        try { return new BigDecimal(o.toString()).doubleValue(); } catch (Exception e) { return 0d; }
    }

    private static Mono<String> mapError(ClientResponse resp) {
        return resp.bodyToMono(String.class)
                .defaultIfEmpty("HTTP " + resp.statusCode())
                .map(body -> "HTTP " + resp.statusCode() + " - " + body);
    }

    private static ExchangeFilterFunction logRequest() {
        return (request, next) -> next.exchange(request);
    }

    // TwelveDataCandleProvider içinde (private helper)


}
