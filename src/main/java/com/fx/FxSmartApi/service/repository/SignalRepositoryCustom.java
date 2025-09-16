package com.fx.FxSmartApi.service.repository;

import com.fx.FxSmartApi.model.entity.SignalDoc;

public interface SignalRepositoryCustom {
    void upsertUnique(SignalDoc doc);

}
