package com.fx.FxSmartApi.common;

public class SymbolUtils {
    public static double pipSize(String symbol) {
        var s = symbol.toUpperCase().replace(" ", "");
        if (s.endsWith("JPY")) return 0.01;
        // XAUUSD vb. için ileride kural eklenebilir
        return 0.0001;
    }
}