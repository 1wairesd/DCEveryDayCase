package com.wairesd.dceverydaycase.tools;

import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Загрузка и предоставление параметров конфигурации плагина.
 */
public class Config {
    private final DCEveryDayCaseAddon plugin;
    private FileConfiguration config;

    public Config(DCEveryDayCaseAddon plugin) {
        this.plugin = plugin;
    }

    /** Загружает конфигурационный файл config.yml */
    public void load() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                } else {
                    configFile.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public long getClaimCooldown() {
        return config.getLong("claim_cooldown", 86400L) * 1000;
    }

    public String getPlaceholderAvailable() {
        return config.getString("placeholder.available", "Доступен ежедневный кейс: нажмите для получения");
    }

    public String getPlaceholderRemaining() {
        return config.getString("placeholder.remaining", "До получения осталось: $d дн, $h ч, $m мин, $s сек");
    }

    public String getCaseName() {
        return config.getString("case_name", "DCEveryDayCase");
    }

    public int getKeysAmount() {
        try {
            return Integer.parseInt(config.getString("keys_amout", "1"));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    public boolean isDebug() {
        return config.getBoolean("debug", false);
    }

    public boolean getBoolean(String ipChecker, boolean b) {
        return config.getBoolean("ip_checker", false);
    }
}
