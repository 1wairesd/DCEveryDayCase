package com.wairesd.dceverydaycase.db;

import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Database Management: Creating a connection, tables, downloading and saving data.
 */
public class DatabaseManager {
    private Connection connection;
    private final DCEveryDayCaseAddon addon;

    public DatabaseManager(DCEveryDayCaseAddon addon) {
        this.addon = addon;
    }

    /** Initializes the database and creates a table if it is not */
    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            File databases = new File(addon.getDataFolder(), "databases");
            if (!databases.exists()) databases.mkdirs();
            File dbFile = new File(databases, "DCEveryDayCase.db");
            if (!dbFile.exists()) addon.getLogger().info("The database is created: " + dbFile.getAbsolutePath());
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS next_claim_times (" +
                        "player_name TEXT PRIMARY KEY, " +
                        "next_claim_time LONG)");
            }
        } catch (Exception e) {
            addon.getLogger().log(Level.SEVERE, "The error of initialization of the database", e);
        }
    }

    /** Loads the data of the next receipt of the key */
    public Map<String, Long> loadNextClaimTimes() {
        Map<String, Long> times = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT player_name, next_claim_time FROM next_claim_times")) {
            while (rs.next()) {
                times.put(rs.getString("player_name"), rs.getLong("next_claim_time"));
            }
        } catch (Exception e) {
            addon.getLogger().log(Level.SEVERE, "Data loading error", e);
        }
        return times;
    }

    /** Retains the data of the next receipt of the key synchronously */
    public void saveNextClaimTimes(Map<String, Long> times) {
        try {
            connection.setAutoCommit(false);
            try (Statement clear = connection.createStatement()) {
                clear.execute("DELETE FROM next_claim_times");
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO next_claim_times (player_name, next_claim_time) VALUES (?, ?)")) {
                times.forEach((player, time) -> {
                    try {
                        ps.setString(1, player);
                        ps.setLong(2, time);
                        ps.addBatch();
                    } catch (SQLException ignored) {}
                });
                ps.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            addon.getLogger().log(Level.SEVERE, "Data conservation error", e);
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                addon.getLogger().log(Level.SEVERE, "Transaction rollback error", rollbackEx);
            }
        }
    }

    /**
     * Asynchronously retains the data of the next receipt of the key and performs Callback after completion.
     *      * @param Times to save.
     *      * @param callback code, which will be executed in the main stream after completion of the conservation.
     */
    public void asyncSaveNextClaimTimes(Map<String, Long> times, Runnable callback) {
        addon.getDCAPI().getPlatform().getScheduler().async(addon, () -> {
            try {
                connection.setAutoCommit(false);
                try (Statement clear = connection.createStatement()) {
                    clear.execute("DELETE FROM next_claim_times");
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO next_claim_times (player_name, next_claim_time) VALUES (?, ?)")) {
                    times.forEach((player, time) -> {
                        try {
                            ps.setString(1, player);
                            ps.setLong(2, time);
                            ps.addBatch();
                        } catch (SQLException ignored) {}
                    });
                    ps.executeBatch();
                }
                connection.commit();
            } catch (SQLException e) {
                addon.getLogger().log(Level.SEVERE, "Data conservation error", e);
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    addon.getLogger().log(Level.SEVERE, "Transaction rollback error", rollbackEx);
                }
            } finally {
                addon.getDCAPI().getPlatform().getScheduler().run(addon, callback, 0L);
            }
        }, 0L);
    }

    /** Закрывает соединение с базой данных */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            addon.getLogger().log(Level.SEVERE, "The error of closing the connection", e);
        }
    }
}
