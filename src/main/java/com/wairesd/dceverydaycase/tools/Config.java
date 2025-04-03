package com.wairesd.dceverydaycase.tools;

import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Loading and providing the parameters of the plugin configuration.
 */
public class Config {
    private final DCEveryDayCaseAddon plugin;
    private FileConfiguration config;

    public Config(DCEveryDayCaseAddon plugin) {
        this.plugin = plugin;
    }

    /** uploads configuration file Config.yml*/
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
        String message = config.getString("placeholder.available", "&7Daily case is available: &aclick to receive");
        return ColorSupport.translate(message);
    }

    public String getPlaceholderRemaining() {
        String message = config.getString("placeholder.remaining", "&7It remains before receiving: &6$d &7day, &6$h &7hour, &6$m &7min, &6$s &7sec");
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
                "Issued {Key} The key (to her) to the player {Player} for the case {CASE}");
    }

    public String getInfoPlaceholder() {
        return config.getString("messages.info_placeholder", "Information is available only for players");
    }

    public boolean isDebug() {
        return config.getBoolean("debug", false);
    }
}
