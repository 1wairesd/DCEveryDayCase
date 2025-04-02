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
        String message = config.getString("placeholder.available", "&7Доступен ежедневный кейс: &aнажмите для получения");
        return ColorSupport.translate(message);
    }

    public String getPlaceholderRemaining() {
        String message = config.getString("placeholder.remaining", "&7До получения осталось: &6$d &7дн, &6$h &7ч, &6$m &7мин, &6$s &7сек");
        return ColorSupport.translate(message);
    }

    public String getCaseName() {
        return config.getString("case_name", "DCEveryDayCase");
    }

    public int getKeysAmount() {
        try {
            return Integer.parseInt(config.getString("keys_amount", "1"));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    public String getLogConsoleGiveKeyMessage() {
        return config.getString("messages.log_console_give_key",
                "Выдано {key} ключ(ей) игроку {player} для кейса {case}");
    }

    public String getInfoPlaceholder() {
        return config.getString("messages.info_placeholder", "Информация доступна только для игроков");
    }

    public boolean isDebug() {
        return config.getBoolean("debug", false);
    }
}
