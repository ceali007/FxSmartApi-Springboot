package com.fx.FxSmartApi.service.repository;

import com.fx.FxSmartApi.model.entity.SignalDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SignalRepository extends MongoRepository<SignalDoc, String>, SignalRepositoryCustom {

}