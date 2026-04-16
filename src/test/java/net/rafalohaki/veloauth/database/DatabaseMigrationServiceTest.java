package net.rafalohaki.veloauth.database;

import net.rafalohaki.veloauth.i18n.Messages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationServiceTest {

    private DatabaseConfig config;
    private DatabaseManager manager;

    @BeforeEach
    void setUp() {
        Messages messages = new Messages();
        messages.setLanguage("en");
        config = DatabaseConfig.forLocalDatabase("H2", "migration_test_" + System.nanoTime());
        manager = new DatabaseManager(config, messages);
    }

    @AfterEach
    void tearDown() {
        manager.shutdown();
    }

    @Test
    void initialize_shouldCreateExpectedIndexesForH2() throws Exception {
        assertTrue(manager.initialize().join(), "Database should initialize");

        assertTrue(indexExists("AUTH", "idx_auth_ip"));
        assertTrue(indexExists("AUTH", "idx_auth_uuid"));
        assertTrue(indexExists("AUTH", "idx_auth_logindate"));
        assertTrue(indexExists("AUTH", "idx_auth_regdate"));
        assertTrue(indexExists("PREMIUM_UUIDS", "idx_premium_uuids_nickname"));
        assertTrue(indexExists("PREMIUM_UUIDS", "idx_premium_uuids_last_seen"));
    }

    private boolean indexExists(String tableName, String indexName) throws SQLException {
        try (Connection connection = DriverManager.getConnection(config.getJdbcUrl())) {
            DatabaseMetaData metaData = connection.getMetaData();
            return indexExists(metaData, tableName, indexName)
                    || indexExists(metaData, tableName.toUpperCase(Locale.ROOT), indexName)
                    || indexExists(metaData, tableName.toLowerCase(Locale.ROOT), indexName);
        }
    }

    private boolean indexExists(DatabaseMetaData metaData, String tableName, String indexName) throws SQLException {
        try (ResultSet indexes = metaData.getIndexInfo(null, null, tableName, false, false)) {
            while (indexes.next()) {
                String existingIndex = indexes.getString("INDEX_NAME");
                if (existingIndex != null && existingIndex.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
