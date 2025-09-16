package com.fx.FxSmartApi.model.api;

import lombok.Data;

import java.util.List;

@Data
public class AdvisorExecuteResponse {
    private String at;                    // ISO local, opsiyonel
    private String tz;                    // default "Europe/Istanbul"
    private List<String> symbols;         // ops.
    private List<String> strategies;      // ops.
    private List<String> timeframes;      // ops.
    private String mode;                  // "manual"|"auto"
}
