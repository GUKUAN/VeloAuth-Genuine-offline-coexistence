package net.rafalohaki.veloauth.database;

import net.rafalohaki.veloauth.i18n.Messages;
import net.rafalohaki.veloauth.model.RegisteredPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcAuthDaoTest {

    private static final String OFFLINE_HASH = "$2a$10$offlinehashvalueofflinehashvalueofflinehashval";

    private DatabaseConfig config;
    private DatabaseManager manager;
    private JdbcAuthDao dao;

    @BeforeEach
    void setUp() {
        Messages messages = new Messages();
        messages.setLanguage("en");
        config = DatabaseConfig.forLocalDatabase("H2", "jdbc_auth_test_" + System.nanoTime());
        manager = new DatabaseManager(config, messages);
        assertTrue(manager.initialize().join(), "Database should initialize");
        dao = new JdbcAuthDao(config);
    }

    @AfterEach
    void tearDown() {
        manager.shutdown();
    }

    @Test
    void findPlayerByLowercaseNickname_emptyNicknameRowShouldThrowSQLException() throws Exception {
        insertRawAuthRow("", "broken-empty", OFFLINE_HASH, UUID.randomUUID().toString(), null);

        assertThrows(SQLException.class, () -> dao.findPlayerByLowercaseNickname("broken-empty"));
    }

    @Test
    void findPlayerByLowercaseNickname_invalidUuidRowShouldThrowSQLException() throws Exception {
        insertRawAuthRow("BrokenUuid", "brokenuuid", OFFLINE_HASH, "not-a-uuid", null);

        assertThrows(SQLException.class, () -> dao.findPlayerByLowercaseNickname("brokenuuid"));
    }

    @Test
    void upsertPlayer_existingPlayerShouldUpdateWithoutCreatingDuplicateRow() throws Exception {
        RegisteredPlayer player = new RegisteredPlayer(
                "Alice",
                OFFLINE_HASH,
                "127.0.0.1",
                UUID.randomUUID().toString()
        );

        assertTrue(dao.upsertPlayer(player));

        player.setHash("$2a$10$updatedhashvalueupdatedhashvalueupdatedhashva");
        assertTrue(dao.upsertPlayer(player));

        assertEquals(1, countAuthRows("alice"));
        assertEquals("$2a$10$updatedhashvalueupdatedhashvalueupdatedhashva",
                dao.findPlayerByLowercaseNickname("alice").getHash());
    }

    private void insertRawAuthRow(String nickname, String lowercaseNickname, String hash, String uuid, String premiumUuid)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(config.getJdbcUrl());
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO AUTH (LOWERCASENICKNAME, NICKNAME, HASH, IP, LOGINIP, UUID, REGDATE, LOGINDATE, PREMIUMUUID, TOTPTOKEN, ISSUEDTIME) "
                             + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, lowercaseNickname.toLowerCase(Locale.ROOT));
            statement.setString(2, nickname);
            statement.setString(3, hash);
            statement.setString(4, "127.0.0.1");
            statement.setString(5, "127.0.0.1");
            statement.setString(6, uuid);
            statement.setLong(7, 1L);
            statement.setLong(8, 1L);
            statement.setString(9, premiumUuid);
            statement.setString(10, null);
            statement.setLong(11, 0L);
            statement.executeUpdate();
        }
    }

    private int countAuthRows(String lowercaseNickname) throws Exception {
        try (Connection connection = DriverManager.getConnection(config.getJdbcUrl());
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM AUTH WHERE LOWERCASENICKNAME = ?")) {
            statement.setString(1, lowercaseNickname);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}
