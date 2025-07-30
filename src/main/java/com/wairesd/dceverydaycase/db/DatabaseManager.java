package com.wairesd.dceverydaycase.db;

import com.wairesd.dceverydaycase.bootstrap.Main;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class DatabaseManager {
    private Connection connection;
    private final Main addon;
    private String dbUrl;

    public DatabaseManager(Main addon) { this.addon = addon; }

    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dbDir = new File(addon.getPlugin().getDataFolder(), "databases");
            dbDir.mkdirs();
            File dbFile = new File(dbDir, "DCEveryDayCase.db");
            if (!dbFile.exists()) addon.getDCAPI().getPlatform().getLogger().info("Database created: " + dbFile.getAbsolutePath());
            dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(dbUrl);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS next_claim_times (player_name TEXT PRIMARY KEY, next_claim_time LONG)");
                stmt.execute("CREATE TABLE IF NOT EXISTS notification_status (player_name TEXT PRIMARY KEY, status INTEGER)");
            }
        } catch (Exception e) {
            addon.getDCAPI().getPlatform().getLogger().log(Level.SEVERE, "Error initializing database", e);
        }
    }

    public Map<String, Long> loadNextClaimTimes() {
        Map<String, Long> times = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT player_name, next_claim_time FROM next_claim_times")) {
            while (rs.next()) times.put(rs.getString("player_name"), rs.getLong("next_claim_time"));
        } catch (Exception e) {
            addon.getDCAPI().getPlatform().getLogger().log(Level.SEVERE, "Error loading data", e);
        }
        return times;
    }

    public void asyncSaveNextClaimTimes(Map<String, Long> times, Runnable callback) {
        addon.getDCAPI().getPlatform().getScheduler().async(addon.getAddon(), () -> {
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
                            addon.getDCAPI().getPlatform().getLogger().log(Level.SEVERE, "Error batching player " + player, ex);
                        }
                    });
                    ps.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                addon.getDCAPI().getPlatform().getLogger().log(Level.SEVERE, "Error saving data asynchronously", e);
            } finally {
                addon.getDCAPI().getPlatform().getScheduler().run(addon.getAddon(), callback, 0L);
            }
        }, 0L);
    }

    public boolean getNotificationStatus(String playerName) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT status FROM notification_status WHERE player_name = ?")) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("status") == 1;
            }
        } catch (SQLException e) {
            addon.getDCAPI().getPlatform().getLogger().log(Level.SEVERE, "Error getting notification status for " + playerName, e);
        }
        return false;
    }

    public void setNotificationStatus(String playerName, boolean status) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO notification_status (player_name, status) VALUES (?, ?)")) {
            ps.setString(1, playerName);
            ps.setInt(2, status ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            addon.getDCAPI().getPlatform().getLogger().log(Level.SEVERE, "Error updating notification status for " + playerName, e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            addon.getDCAPI().getPlatform().getLogger().log(Level.SEVERE, "Error closing database connection", e);
        }
    }

    public void reload() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }

            File dbDir = new File(addon.getPlugin().getDataFolder(), "databases");
            dbDir.mkdirs();
            File dbFile = new File(dbDir, "DCEveryDayCase.db");
            dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(dbUrl);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS next_claim_times (player_name TEXT PRIMARY KEY, next_claim_time LONG)");
                stmt.execute("CREATE TABLE IF NOT EXISTS notification_status (player_name TEXT PRIMARY KEY, status INTEGER)");
            }

            addon.getDCAPI().getPlatform().getLogger().info("Database reloaded successfully.");
        } catch (Exception e) {
            addon.getDCAPI().getPlatform().getLogger().log(Level.SEVERE, "Error reloading database", e);
        }
    }
}
