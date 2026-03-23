package net.rafalohaki.veloauth.database;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.table.TableUtils;
import net.rafalohaki.veloauth.model.PremiumUuid;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PremiumUuidDaoTest {

    private DatabaseConfig config;
    private JdbcConnectionSource connectionSource;
    private PremiumUuidDao dao;

    @BeforeEach
    void setUp() throws Exception {
        config = DatabaseConfig.forLocalDatabase("H2", "premium_uuid_test_" + System.nanoTime());
        connectionSource = new JdbcConnectionSource(config.getJdbcUrl());
        TableUtils.createTableIfNotExists(connectionSource, PremiumUuid.class);
        dao = new PremiumUuidDao(connectionSource);
    }

    @AfterEach
    void tearDown() throws Exception {
        connectionSource.close();
    }

    @Test
    void findByNicknameStrict_invalidUuidRowShouldThrowSQLException() throws Exception {
        insertRawPremiumUuid("not-a-uuid", "Alice", System.currentTimeMillis(), System.currentTimeMillis());

        assertThrows(SQLException.class, () -> dao.findByNicknameStrict("Alice"));
        assertTrue(dao.findByNickname("Alice").isEmpty(), "Public lookup should still fail open for cache callers");
    }

    @Test
    void saveOrUpdateStrict_shouldDeleteConflictingNicknameRows() throws Exception {
        insertRawPremiumUuid(UUID.randomUUID().toString(), "Alice", 100L, 100L);
        insertRawPremiumUuid(UUID.randomUUID().toString(), "ALICE", 200L, 200L);
        UUID authoritativeUuid = UUID.randomUUID();

        assertTrue(dao.saveOrUpdateStrict(authoritativeUuid, "Alice"));

        Optional<PremiumUuid> resolved = dao.findByNicknameStrict("Alice");
        List<PremiumUuid> rows = dao.findAll();

        assertTrue(resolved.isPresent());
        assertEquals(authoritativeUuid, resolved.get().getUuid());
        assertEquals(1, rows.size());
        assertEquals(authoritativeUuid, rows.get(0).getUuid());
        assertEquals("Alice", rows.get(0).getNickname());
    }

    private void insertRawPremiumUuid(String uuid, String nickname, long lastSeen, long verifiedAt) throws Exception {
        try (Connection connection = DriverManager.getConnection(config.getJdbcUrl());
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO PREMIUM_UUIDS (UUID, NICKNAME, LAST_SEEN, VERIFIED_AT) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, uuid);
            statement.setString(2, nickname);
            statement.setLong(3, lastSeen);
            statement.setLong(4, verifiedAt);
            statement.executeUpdate();
        }
    }
}
