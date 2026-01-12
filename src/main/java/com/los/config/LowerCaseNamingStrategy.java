package com.los.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Custom naming strategy to convert all identifiers to lowercase and quote them.
 * This ensures compatibility with H2 database and matches the Flyway migrations
 * which use lowercase identifiers like "partner_code", "screen_configs", etc.
 */
public class LowerCaseNamingStrategy implements PhysicalNamingStrategy {

    @Override
    public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return toLowerCase(name);
    }

    @Override
    public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return toLowerCase(name);
    }

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return toLowerCase(name);
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return toLowerCase(name);
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return toLowerCase(name);
    }

    private Identifier toLowerCase(Identifier identifier) {
        if (identifier == null) {
            return null;
        }
        // Quote the identifier to preserve exact lowercase case in H2
        return Identifier.toIdentifier(identifier.getText().toLowerCase(), true);
    }
}
