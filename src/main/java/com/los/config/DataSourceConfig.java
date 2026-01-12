package com.los.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.File;

/**
 * DataSource configuration to ensure file-based H2 database is always used.
 * This prevents IntelliJ or other tools from overriding the datasource URL.
 */
@Configuration
public class DataSourceConfig {

    @Value("${user.dir}")
    private String userDir;

    @Bean
    @Primary
    public DataSource dataSource() {
        // Ensure data directory exists
        File dataDir = new File(userDir, "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        // Force file-based database with absolute path
        String dbPath = new File(dataDir, "los-config-db").getAbsolutePath();
        String jdbcUrl = String.format(
            "jdbc:h2:file:%s;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            dbPath
        );

        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url(jdbcUrl)
                .username("sa")
                .password("password")
                .build();
    }
}
