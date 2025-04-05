package com.wairesd.dceverydaycase.tools;

import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;
import org.bukkit.configuration.file.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/** Loads and provides access to plugin configuration. */
public class Config {
    private final DCEveryDayCaseAddon plugin;
    private FileConfiguration config;

    public Config(DCEveryDayCaseAddon plugin) { this.plugin = plugin; }

    /** Loads config.yml from the plugin folder or creates it if missing. */
    public void load() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("config.yml")) {
                if (in != null) Files.copy(in, configFile.toPath());
                else configFile.createNewFile();
            } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    // Configuration getters with defaults
    public long getClaimCooldown() { return config.getLong("claim_cooldown", 86400L) * 1000; }
    public String getPlaceholderAvailable() { return Color.translate(config.getString("placeholder.available", "&7Daily case is available: &aclick to receive")); }
    public String getPlaceholderRemaining() { return Color.translate(config.getString("placeholder.remaining", "&7Time remaining: &6$d days, &6$h hours, &6$m minutes, &6$s seconds")); }
    public String getCaseName() { return config.getString("case_name", "DCEveryDayCase"); }
    public int getKeysAmount() {
        try { return Integer.parseInt(config.getString("keys_amount", "1")); }
        catch (NumberFormatException ex) { return 1; }
    }
    public String getNewPlayerChoice() { return config.getString("new_player_choice", "timer"); }
    public boolean isDebug() { return config.getBoolean("debug", false); }
    public String getLogConsoleGiveKeyMessage() { return config.getString("messages.log_console_give_key", "Issued {key} key to player {player} for case {case}"); }
    public String getInfoPlaceholder() { return config.getString("messages.info_placeholder", "Information available only for players"); }
    public String getCaseGrantedOn() { return Color.translate(config.getString("messages.case_granted_on", "&aNotifications enabled")); }
    public String getCaseGrantedOff() { return Color.translate(config.getString("messages.case_granted_off", "&cNotifications disabled")); }
    public String getOnlyForPlayersMessage() { return Color.translate(config.getString("messages.only_for_players", "This command is for players only.")); }
    public String getNoPermissionMessage() { return Color.translate(config.getString("messages.no_permission", "&cYou don't have permission.")); }
    public String getCaseReadyMessage() { return Color.translate(config.getString("messages.case_ready", "&aYour daily case is ready!")); }
}
