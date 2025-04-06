package com.wairesd.dceverydaycase.config;

import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.util.logging.Logger;

public class ConfigMigrator {

    private final DCEveryDayCaseAddon addon;
    private final Config config;

    public ConfigMigrator(DCEveryDayCaseAddon addon, Config config) {
        this.addon = addon;
        this.config = config;
    }

    public void migrateConfig() {
        int currentVersion = config.node().configVersion;
        int latestVersion = 2;  // Update the version if necessary

        if (currentVersion < latestVersion) {
            Logger logger = addon.getLogger();

            // We migrate the configuration based on the version
            if (currentVersion == 1) {
                logger.info("Migrating config from version 1 to version 2");
                migrateV1toV2();
            }

            // Check and add missing keys to the configuration
            checkAndAddKey("claim_cooldown", 86400);
            checkAndAddKey("case_name", "DCEveryDayCase");
            checkAndAddKey("keys_amount", 1);
            checkAndAddKey("new_player_choice", "case");
            checkAndAddKey("turn_off_daily_case_logic", false);
            checkAndAddKey("debug", false);

            // Checking messages
            checkAndAddMessage("log_console_give_key", "Issued {key} key to player {player} for case {case}");
            checkAndAddMessage("info_placeholder", "Information available only for players");
            checkAndAddMessage("case_granted_on", "&aNotifications enabled");
            checkAndAddMessage("case_granted_off", "&cNotifications disabled");
            checkAndAddMessage("only_for_players", "This command is for players only.");
            checkAndAddMessage("case_ready", "&aYour daily case is ready!");

            // We update the version after migration
            config.node().configVersion = latestVersion;
            saveConfig();
        }
    }

    private void checkAndAddKey(String key, Object defaultValue) {
        if (!config.node().containsKey(key)) {
            addon.getLogger().info("Adding missing key: " + key);
            config.node().set(key, defaultValue);
        }
    }

    private void checkAndAddMessage(String key, String defaultMessage) {
        if (!config.node().messages.containsKey(key)) {
            addon.getLogger().info("Adding missing message: " + key);
            try {
                // Here checking to add the missing keys to the Messages section
                config.node().messages.getClass().getField(key).set(config.node().messages, defaultMessage);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                addon.getLogger().warning("Error adding message: " + key);
            }
        }
    }


    private void migrateV1toV2() {
        // Example migration: Update existing settings or add new ones
        // If keysAmount was stored as a boolean (0 or 1), convert it to an integer
        if (config.node().keysAmount == 0) {
            config.node().keysAmount = 1;
        }

        // Additional migration logic can be added here as needed
    }

    private void saveConfig() {
        try {
            // Create a new loader to load and save the configuration file
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .file(config.getFile())  // Make sure the file is passed here
                    .build();

            // Create the root node of the configuration
            ConfigurationNode node = loader.load();

            // Now set the config node into this loaded node
            node.set(ConfigNode.class, config.node());  // Save the updated configuration into the node

            // Save the updated configuration back to the file
            loader.save(node);
        } catch (ConfigurateException e) {
            addon.getLogger().warning("Error saving migrated config: " + e.getMessage());
        }
    }
}
