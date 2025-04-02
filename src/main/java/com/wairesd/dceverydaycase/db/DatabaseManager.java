package com.wairesd.dceverydaycase.db;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Управление базой данных: создание подключения, таблиц, загрузка и сохранение данных.
 */
public class DatabaseManager {
    private Connection connection;
    private final JavaPlugin plugin;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Инициализирует базу данных и создаёт таблицу, если её нет */
    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            File databases = new File(plugin.getDataFolder(), "databases");
            if (!databases.exists()) databases.mkdirs();
            File dbFile = new File(databases, "DCEveryDayCase.db");
            if (!dbFile.exists()) plugin.getLogger().info("Создаётся база данных: " + dbFile.getAbsolutePath());
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS next_claim_times (" +
                        "player_name TEXT PRIMARY KEY, " +
                        "next_claim_time LONG)");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка инициализации БД", e);
        }
    }

    /** Загружает данные времени следующего получения ключа */
    public Map<String, Long> loadNextClaimTimes() {
        Map<String, Long> times = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT player_name, next_claim_time FROM next_claim_times")) {
            while (rs.next()) {
                times.put(rs.getString("player_name"), rs.getLong("next_claim_time"));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки данных", e);
        }
        return times;
    }

    /** Сохраняет данные времени следующего получения ключа синхронно */
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
            plugin.getLogger().log(Level.SEVERE, "Ошибка сохранения данных", e);
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка отката транзакции", rollbackEx);
            }
        }
    }

    /**
     * Асинхронно сохраняет данные времени следующего получения ключа и выполняет callback после завершения.
     * @param times Данные для сохранения.
     * @param callback Код, который будет выполнен в основном потоке после завершения сохранения.
     */
    public void asyncSaveNextClaimTimes(Map<String, Long> times, Runnable callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
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
                plugin.getLogger().log(Level.SEVERE, "Ошибка сохранения данных", e);
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    plugin.getLogger().log(Level.SEVERE, "Ошибка отката транзакции", rollbackEx);
                }
            } finally {
                // Выполнить callback в основном потоке
                Bukkit.getScheduler().runTask(plugin, callback);
            }
        });
    }

    /** Закрывает соединение с базой данных */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка закрытия соединения", e);
        }
    }
}
