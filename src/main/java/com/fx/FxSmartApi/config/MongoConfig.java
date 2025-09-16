package com.fx.FxSmartApi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration
@EnableMongoAuditing   // @CreatedDate / @LastModifiedDate için
public class MongoConfig {

    @Bean
    public MappingMongoConverter mappingMongoConverter(
            MongoDatabaseFactory factory,
            MongoMappingContext context,
            org.springframework.data.mongodb.core.convert.MongoCustomConversions conversions) {

        var resolver = new org.springframework.data.mongodb.core.convert.DefaultDbRefResolver(factory);
        var converter = new MappingMongoConverter(resolver, context);
        converter.setCustomConversions(conversions);
        // _class alanını yazmayı kapat
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return converter;
    }
}
