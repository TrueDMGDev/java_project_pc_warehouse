package com.pcwarehouse.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

public final class DatabaseMigrator {

    private DatabaseMigrator() {
    }

    public static void migrate() {
        if (!DatabaseConfig.isFlywayEnabled()) {
            return;
        }

        try {
            String[] locations = resolveLocations();
            Flyway.configure()
                    .dataSource(
                            DatabaseConfig.getUrl(),
                            DatabaseConfig.getUsername(),
                            DatabaseConfig.getPassword()
                    )
                    .locations(locations)
                    .cleanDisabled(true)
                    .baselineOnMigrate(false)
                    .load()
                    .migrate();
            LegacyPasswordUpgrade.upgradeSeedPasswords();
        } catch (FlywayException exception) {
            throw new IllegalStateException("Flyway migration failed: " + exception.getMessage(), exception);
        } catch (SQLException exception) {
            throw new IllegalStateException("Legacy password upgrade failed: " + exception.getMessage(), exception);
        }
    }

    private static String[] resolveLocations() {
        Path developmentMigrationPath = Path.of("src", "main", "resources", "db", "migration");
        if (Files.isDirectory(developmentMigrationPath)) {
            return new String[] {
                    "filesystem:" + developmentMigrationPath.toAbsolutePath().normalize()
            };
        }

        return DatabaseConfig.getFlywayLocations();
    }
}
