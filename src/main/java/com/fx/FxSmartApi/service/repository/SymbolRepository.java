// src/main/java/com/fx/FxSmartApi/repository/SymbolRepository.java
package com.fx.FxSmartApi.service.repository;

import java.util.List;
import java.util.Optional;

import com.fx.FxSmartApi.model.entity.SymbolDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SymbolRepository extends MongoRepository<SymbolDoc, String> {
    List<SymbolDoc> findByIsActiveTrue();
    Optional<SymbolDoc> findByCode(String code);
}
