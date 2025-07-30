package com.wairesd.dceverydaycase.config;

import com.wairesd.dceverydaycase.bootstrap.Main;
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

public class ConfigManager {
    private final Main addon;
    private final File configFile;
    private final File langDir;
    private final Map<String, YamlConfigurationLoader> languageLoaders = new HashMap<>();
    private final Map<String, LanguageMessages> languageNodes = new HashMap<>();
    private RootConfig rootConfig;
    private LanguageMessages currentLanguageMessages;

    public ConfigManager(File configFile, Main addon) {
        this.addon = addon;
        this.configFile = configFile;
        this.langDir = new File(configFile.getParentFile(), "lang");
        if (!langDir.exists()) langDir.mkdirs();
    }

    public void load() {
        if (!configFile.exists()) addon.getPlugin().saveResource(configFile.getName(), false);
        reload();
    }

    public void reload() {
        try {
            YamlConfigurationLoader configLoader = YamlConfigurationLoader.builder()
                    .nodeStyle(NodeStyle.BLOCK)
                    .file(configFile)
                    .build();
            ConfigurationNode root = configLoader.load();
            this.rootConfig = root.get(RootConfig.class, new RootConfig());

            if (rootConfig.configMetadata == null || rootConfig.configMetadata.version == 0) {
                rootConfig.configMetadata = new ConfigMetadata();
                rootConfig.configMetadata.version = 1;
                rootConfig.configMetadata.type = "CONFIG";
                configLoader.save(root);
            }

            loadAllLanguages();

            loadCurrentLanguage(rootConfig.dailyCaseSettings.languages);
        } catch (ConfigurateException e) {
            addon.getDCAPI().getPlatform().getLogger().log(Level.WARNING, "Error reloading configuration", e);
        }
    }

    private void loadAllLanguages() {
        languageLoaders.clear();
        languageNodes.clear();

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
                    addon.getDCAPI().getPlatform().getLogger().log(Level.WARNING, "Error loading language file: " + file.getName(), e);
                }
            }
        }
    }

    private void loadCurrentLanguage(String lang) {
        if (languageNodes.containsKey(lang)) {
            currentLanguageMessages = languageNodes.get(lang);
        } else {
            File langFile = new File(langDir, lang + ".yml");
            try (InputStream is = addon.getPlugin().getClass().getResourceAsStream("/lang/" + lang + ".yml")) {
                if (is != null) {
                    Files.copy(is, langFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                            .nodeStyle(NodeStyle.BLOCK)
                            .file(langFile)
                            .build();
                    ConfigurationNode node = loader.load();
                    LanguageMessages messages = node.get(LanguageMessages.class, new LanguageMessages());
                    languageNodes.put(lang, messages);
                    currentLanguageMessages = messages;
                    addon.getDCAPI().getPlatform().getLogger().info("Language " + lang + " loaded from resources and saved to lang folder.");
                } else {
                    addon.getDCAPI().getPlatform().getLogger().warning("Language " + lang + " not found in plugin resources. Falling back to en_US.");
                    loadFallbackLanguage();
                }
            } catch (IOException e) {
                addon.getDCAPI().getPlatform().getLogger().log(Level.WARNING, "Error loading language " + lang + " from resources", e);
                loadFallbackLanguage();
            }
        }
    }

    private void loadFallbackLanguage() {
        if (languageNodes.containsKey("en_US")) {
            currentLanguageMessages = languageNodes.get("en_US");
        } else {
            File enUsFile = new File(langDir, "en_US.yml");
            try (InputStream is = addon.getPlugin().getClass().getResourceAsStream("/lang/en_US.yml")) {
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
                    addon.getDCAPI().getPlatform().getLogger().info("Fallback language en_US loaded from resources and saved to lang folder.");
                } else {
                    addon.getDCAPI().getPlatform().getLogger().severe("Default language file en_US.yml not found in plugin resources!");
                    currentLanguageMessages = new LanguageMessages();
                }
            } catch (IOException e) {
                addon.getDCAPI().getPlatform().getLogger().log(Level.WARNING, "Error loading default language file en_US", e);
                currentLanguageMessages = new LanguageMessages();
            }
        }
    }

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