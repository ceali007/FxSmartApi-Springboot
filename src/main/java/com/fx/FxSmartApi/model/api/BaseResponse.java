package com.fx.FxSmartApi.model.api;

import java.util.List;

public final class BaseResponse {
    private final boolean success;
    private final String message;
    private final List<String> errors;

    private BaseResponse(boolean success, String message, List<String> errors) {
        this.success = success;
        this.message = message;
        this.errors = errors;
    }

    public static BaseResponse ok() {
        return new BaseResponse(true, "ok", List.of());
    }

    public static BaseResponse ok(String message) {
        return new BaseResponse(true, message, List.of());
    }

    public static BaseResponse fail(String message) {
        return new BaseResponse(false, message, List.of());
    }

    public static BaseResponse fail(String message, List<String> errors) {
        return new BaseResponse(false, message, errors == null ? List.of() : List.copyOf(errors));
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<String> getErrors() { return errors; }
}
