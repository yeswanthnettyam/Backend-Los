package com.los;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main application class for LOS Config Service.
 * 
 * This service is the single source of truth for:
 * - Screen Configuration (UI structure & actions)
 * - Validation Configuration (business correctness)
 * - Flow Configuration (navigation & decisioning)
 * - Field Mapping Configuration (UI → Domain → DB)
 * - Runtime orchestration for Android & Web
 * - Master Data (Partners, Products, Branches)
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
public class LosConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LosConfigServiceApplication.class, args);
    }
}

