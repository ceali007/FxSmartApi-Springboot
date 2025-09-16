package com.fx.FxSmartApi;

import com.fx.FxSmartApi.config.IngestProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan(basePackageClasses = IngestProperties.class)
// Alternatif: @EnableConfigurationProperties(IngestProperties.class)
public class FxSmartApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(FxSmartApiApplication.class, args);
	}
}

