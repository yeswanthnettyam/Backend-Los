package com.los.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Startup listener to verify the correct database is being used.
 * Logs a warning if an in-memory database is detected instead of file-based.
 */
@Slf4j
@Component
public class DatabaseStartupListener {

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    @EventListener(ApplicationReadyEvent.class)
    public void checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String url = metaData.getURL();
            
            log.info("=== DATABASE CONFIGURATION ===");
            log.info("Database URL: {}", url);
            
            if (url.contains(":mem:")) {
                log.error("⚠️  WARNING: Using IN-MEMORY database! Data will be lost on restart!");
                log.error("⚠️  Expected file-based database at: {}/data/los-config-db", 
                    environment.getProperty("user.dir"));
                log.error("⚠️  Please check IntelliJ Run Configuration:");
                log.error("⚠️  1. Run → Edit Configurations...");
                log.error("⚠️  2. Remove any VM options like: -Dspring.datasource.url=...");
                log.error("⚠️  3. Remove any Environment variables overriding datasource.url");
            } else if (url.contains(":file:")) {
                log.info("✅ Using FILE-BASED database - data will persist across restarts");
            }
            
            log.info("Database Product: {}", metaData.getDatabaseProductName());
            log.info("Database Version: {}", metaData.getDatabaseProductVersion());
            log.info("==============================");
        } catch (Exception e) {
            log.error("Failed to check database configuration", e);
        }
    }
}
