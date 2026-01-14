package com.los.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Flyway configuration to ensure migrations are always found.
 * This configuration helps when resources aren't properly copied to target/classes.
 */
@Configuration
@Slf4j
public class FlywayConfig {

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String flywayLocations;

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // Verify migrations are available
            verifyMigrationsAvailable();
            // Run migrations
            flyway.migrate();
        };
    }

    private void verifyMigrationsAvailable() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:db/migration/*.sql");
            
            if (resources.length == 0) {
                log.warn("⚠️  No migration files found in classpath:db/migration");
                log.warn("   Attempting to find migrations in file system...");
                
                // Fallback: Check file system
                Path migrationPath = Paths.get("src/main/resources/db/migration");
                if (Files.exists(migrationPath)) {
                    long fileCount = Files.list(migrationPath)
                            .filter(p -> p.toString().endsWith(".sql"))
                            .count();
                    if (fileCount > 0) {
                        log.warn("   Found {} migration files in src/main/resources/db/migration", fileCount);
                        log.warn("   ⚠️  Please run './ensure-resources.sh' before starting the application");
                        log.warn("   Or configure IntelliJ to copy resources (see INTELLIJ_SETUP.md)");
                    }
                }
            } else {
                log.info("✅ Found {} Flyway migration files in classpath", resources.length);
            }
        } catch (Exception e) {
            log.error("Error verifying Flyway migrations: {}", e.getMessage());
        }
    }
}
