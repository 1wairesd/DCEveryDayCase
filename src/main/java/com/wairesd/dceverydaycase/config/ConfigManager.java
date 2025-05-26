package com.wairesd.dceverydaycase.config;

import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;
import com.wairesd.dceverydaycase.config.models.ConfigMetadata;
import com.wairesd.dceverydaycase.config.models.LanguageMessages;
import com.wairesd.dceverydaycase.config.models.RootConfig;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages the loading and reloading of configuration and language files for the DCEveryDayCase addon.
 */
public class ConfigManager {
    private final DCEveryDayCaseAddon addon;
    private final File configFile;
    private final File langDir;
    private final Map<String, YamlConfigurationLoader> languageLoaders = new HashMap<>();
    private final Map<String, LanguageMessages> languageNodes = new HashMap<>();
    private RootConfig rootConfig;
    private LanguageMessages currentLanguageMessages;

    public ConfigManager(File configFile, DCEveryDayCaseAddon addon) {
        this.addon = addon;
        this.configFile = configFile;
        this.langDir = new File(configFile.getParentFile(), "lang");
        if (!langDir.exists()) langDir.mkdirs();
    }

    /**
     * Loads the configuration file if it doesn't exist and triggers a reload.
     */
    public void load() {
        if (!configFile.exists()) addon.saveResource(configFile.getName(), false);
        reload();
    }

    /**
     * Reloads the main configuration and language files.
     */
    public void reload() {
        try {
            // Load the main configuration file
            YamlConfigurationLoader configLoader = YamlConfigurationLoader.builder()
                    .nodeStyle(NodeStyle.BLOCK)
                    .file(configFile)
                    .build();
            ConfigurationNode root = configLoader.load();
            this.rootConfig = root.get(RootConfig.class, new RootConfig());

            // Set the config metadata if it's missing
            if (rootConfig.configMetadata == null || rootConfig.configMetadata.version == 0) {
                rootConfig.configMetadata = new ConfigMetadata();
                rootConfig.configMetadata.version = 1;
                rootConfig.configMetadata.type = "CONFIG";
                configLoader.save(root);
            }

            // Reload all language files
            loadAllLanguages();

            // Load the current selected language
            loadCurrentLanguage(rootConfig.dailyCaseSettings.languages);
        } catch (ConfigurateException e) {
            addon.getLogger().log(Level.WARNING, "Error reloading configuration", e);
        }
    }

    /**
     * Loads all language files from the lang directory.
     */
    private void loadAllLanguages() {
        languageLoaders.clear();
        languageNodes.clear();

        // Scan the lang directory for .yml files
        File[] files = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String lang = file.getName().replace(".yml", "");
                YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                        .nodeStyle(NodeStyle.BLOCK)
                        .file(file)
                        .build();
                languageLoaders.put(lang, loader);
                try {
                    ConfigurationNode node = loader.load();
                    LanguageMessages messages = node.get(LanguageMessages.class, new LanguageMessages());
                    languageNodes.put(lang, messages);
                } catch (ConfigurateException e) {
                    addon.getLogger().log(Level.WARNING, "Error loading language file: " + file.getName(), e);
                }
            }
        }
    }

    /**
     * Loads the specified language or falls back to en_US if not found.
     */
    private void loadCurrentLanguage(String lang) {
        if (languageNodes.containsKey(lang)) {
            // If the tongue is already loaded from the Lang folder, we use it
            currentLanguageMessages = languageNodes.get(lang);
        } else {
            // Try to download the specified language from resources
            File langFile = new File(langDir, lang + ".yml");
            try (InputStream is = addon.getClass().getResourceAsStream("/lang/" + lang + ".yml")) {
                if (is != null) {
                    // If the file is found in resources, copy it to the Lang folder
                    Files.copy(is, langFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                            .nodeStyle(NodeStyle.BLOCK)
                            .file(langFile)
                            .build();
                    ConfigurationNode node = loader.load();
                    LanguageMessages messages = node.get(LanguageMessages.class, new LanguageMessages());
                    languageNodes.put(lang, messages);
                    currentLanguageMessages = messages;
                    addon.getLogger().info("Language " + lang + " loaded from resources and saved to lang folder.");
                } else {
                    // If the specified language is not found in resources, we switch to en_us
                    addon.getLogger().warning("Language " + lang + " not found in plugin resources. Falling back to en_US.");
                    loadFallbackLanguage();
                }
            } catch (IOException e) {
                addon.getLogger().log(Level.WARNING, "Error loading language " + lang + " from resources", e);
                loadFallbackLanguage();
            }
        }
    }

    /**
     * Loads the fallback language (en_US) from resources if needed.
     */
    private void loadFallbackLanguage() {
        if (languageNodes.containsKey("en_US")) {
            currentLanguageMessages = languageNodes.get("en_US");
        } else {
            File enUsFile = new File(langDir, "en_US.yml");
            try (InputStream is = addon.getClass().getResourceAsStream("/lang/en_US.yml")) {
                if (is != null) {
                    Files.copy(is, enUsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                            .nodeStyle(NodeStyle.BLOCK)
                            .file(enUsFile)
                            .build();
                    ConfigurationNode node = loader.load();
                    LanguageMessages enUsMessages = node.get(LanguageMessages.class, new LanguageMessages());
                    languageNodes.put("en_US", enUsMessages);
                    currentLanguageMessages = enUsMessages;
                    addon.getLogger().info("Fallback language en_US loaded from resources and saved to lang folder.");
                } else {
                    addon.getLogger().severe("Default language file en_US.yml not found in plugin resources!");
                    currentLanguageMessages = new LanguageMessages(); // Empty object as the last reserve
                }
            } catch (IOException e) {
                addon.getLogger().log(Level.WARNING, "Error loading default language file en_US", e);
                currentLanguageMessages = new LanguageMessages(); // Empty object as the last reserve
            }
        }
    }

    // Getters for settings
    public long getClaimCooldown() {
        return rootConfig.dailyCaseSettings.caseSettings.claimCooldown;
    }

    public String getCaseName() {
        return rootConfig.dailyCaseSettings.caseSettings.caseName;
    }

    public int getKeysAmount() {
        return rootConfig.dailyCaseSettings.caseSettings.keysAmount;
    }

    public String getNewPlayerChoice() {
        return rootConfig.dailyCaseSettings.caseSettings.newPlayerChoice;
    }

    public boolean isDebug() {
        return rootConfig.dailyCaseSettings.debug;
    }

    public boolean isTurnOffDailyCaseLogic() {
        return rootConfig.dailyCaseSettings.caseSettings.turnOffDailyCaseLogic;
    }

    public String getLanguages() {
        return rootConfig.dailyCaseSettings.languages;
    }

    // Getters for messages
    public String getAvailable() {
        return currentLanguageMessages.available;
    }

    public String getRemaining() {
        return currentLanguageMessages.remaining;
    }

    public String getLogConsoleGiveKey() {
        return currentLanguageMessages.logConsoleGiveKey;
    }

    public String getInfoPlaceholder() {
        return currentLanguageMessages.infoPlaceholder;
    }

    public String getCaseGrantedOn() {
        return currentLanguageMessages.caseGrantedOn;
    }

    public String getCaseGrantedOff() {
        return currentLanguageMessages.caseGrantedOff;
    }

    public String getOnlyForPlayersMessage() {
        return currentLanguageMessages.onlyForPlayersMessage;
    }

    public String getNoPermissionMessage() {
        return currentLanguageMessages.noPermissionMessage;
    }

    public String getCaseReadyMessage() {
        return currentLanguageMessages.caseReadyMessage;
    }
}