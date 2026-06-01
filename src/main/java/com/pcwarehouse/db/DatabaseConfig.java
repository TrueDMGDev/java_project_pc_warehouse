package com.pcwarehouse.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class DatabaseConfig {

    private static final String CONFIG_FILE = "/application.properties";
    private static final Properties PROPERTIES = loadProperties();

    private DatabaseConfig() {
    }

    public static String getUrl() {
        return PROPERTIES.getProperty("db.url", "jdbc:postgresql://localhost:5432/pc_warehouse");
    }

    public static String getUsername() {
        return PROPERTIES.getProperty("db.username", "postgres");
    }

    public static String getPassword() {
        return PROPERTIES.getProperty("db.password", "postgres");
    }

    public static boolean isFlywayEnabled() {
        return Boolean.parseBoolean(PROPERTIES.getProperty("db.flyway.enabled", "true"));
    }

    public static String[] getFlywayLocations() {
        String rawLocations = PROPERTIES.getProperty("db.flyway.locations", "classpath:db/migration");
        return rawLocations.split("\\s*,\\s*");
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = DatabaseConfig.class.getResourceAsStream(CONFIG_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load application.properties", exception);
        }
        return properties;
    }
}
