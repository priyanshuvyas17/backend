package com.xray.backend;

import com.xray.backend.config.XrayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableAsync
@EnableConfigurationProperties(XrayProperties.class)
public class XrayBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(XrayBackendApplication.class, args);
	}

}
