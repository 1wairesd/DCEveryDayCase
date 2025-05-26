package com.wairesd.dceverydaycase.db;

import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class DatabaseManager {
    private Connection connection;
    private final DCEveryDayCaseAddon addon;
    private String dbUrl;

    // Constructor to initialize DatabaseManager with the addon instance
    public DatabaseManager(DCEveryDayCaseAddon addon) { this.addon = addon; }

    /**
     * Initializes the SQLite database and creates required tables if they don't exist.
     */
    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dbDir = new File(addon.getDataFolder(), "databases");
            dbDir.mkdirs();
            File dbFile = new File(dbDir, "DCEveryDayCase.db");
            if (!dbFile.exists()) addon.getLogger().info("Database created: " + dbFile.getAbsolutePath());
            dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(dbUrl);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS next_claim_times (player_name TEXT PRIMARY KEY, next_claim_time LONG)");
                stmt.execute("CREATE TABLE IF NOT EXISTS notification_status (player_name TEXT PRIMARY KEY, status INTEGER)");
            }
        } catch (Exception e) {
            addon.getLogger().log(Level.SEVERE, "Error initializing database", e);
        }
    }

    /**
     * Loads the next claim times for all players from the database.
     */
    public Map<String, Long> loadNextClaimTimes() {
        Map<String, Long> times = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT player_name, next_claim_time FROM next_claim_times")) {
            while (rs.next()) times.put(rs.getString("player_name"), rs.getLong("next_claim_time"));
        } catch (Exception e) {
            addon.getLogger().log(Level.SEVERE, "Error loading data", e);
        }
        return times;
    }

    /**
     * Asynchronously saves the next claim times for players.
     */
    public void asyncSaveNextClaimTimes(Map<String, Long> times, Runnable callback) {
        addon.getDCAPI().getPlatform().getScheduler().async(addon, () -> {
            File dbFile = new File(dbUrl.substring("jdbc:sqlite:".length()));
            dbFile.getParentFile().mkdirs();
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR REPLACE INTO next_claim_times (player_name, next_claim_time) VALUES (?, ?)")) {
                    times.forEach((player, time) -> {
                        try {
                            ps.setString(1, player);
                            ps.setLong(2, time);
                            ps.addBatch();
                        } catch (SQLException ex) {
                            addon.getLogger().log(Level.SEVERE, "Error batching player " + player, ex);
                        }
                    });
                    ps.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                addon.getLogger().log(Level.SEVERE, "Error saving data asynchronously", e);
            } finally {
                addon.getDCAPI().getPlatform().getScheduler().run(addon, callback, 0L);
            }
        }, 0L);
    }

    /**
     * Gets the notification status of a player.
     */
    public boolean getNotificationStatus(String playerName) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT status FROM notification_status WHERE player_name = ?")) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("status") == 1;
            }
        } catch (SQLException e) {
            addon.getLogger().log(Level.SEVERE, "Error getting notification status for " + playerName, e);
        }
        return false;
    }

    /**
     * Sets the notification status of a player.
     */
    public void setNotificationStatus(String playerName, boolean status) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO notification_status (player_name, status) VALUES (?, ?)")) {
            ps.setString(1, playerName);
            ps.setInt(2, status ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            addon.getLogger().log(Level.SEVERE, "Error updating notification status for " + playerName, e);
        }
    }

    /**
     * Closes the database connection safely.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            addon.getLogger().log(Level.SEVERE, "Error closing database connection", e);
        }
    }

    /**
     * Reloads the database by closing and reopening the connection.
     */
    public void reload() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }

            File dbDir = new File(addon.getDataFolder(), "databases");
            dbDir.mkdirs();
            File dbFile = new File(dbDir, "DCEveryDayCase.db");
            dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(dbUrl);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS next_claim_times (player_name TEXT PRIMARY KEY, next_claim_time LONG)");
                stmt.execute("CREATE TABLE IF NOT EXISTS notification_status (player_name TEXT PRIMARY KEY, status INTEGER)");
            }

            addon.getLogger().info("Database reloaded successfully.");
        } catch (Exception e) {
            addon.getLogger().log(Level.SEVERE, "Error reloading database", e);
        }
    }
}
