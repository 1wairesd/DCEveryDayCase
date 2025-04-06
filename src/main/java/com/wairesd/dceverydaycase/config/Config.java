package com.wairesd.dceverydaycase.config;

import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.logging.Level;

/** Loads and provides access to plugin configuration. */
public class Config {

    private final DCEveryDayCaseAddon addon;
    private final File file;
    private final YamlConfigurationLoader loader;

    private ConfigNode configNode;

    public Config(File file, DCEveryDayCaseAddon addon) {
        this.addon = addon;
        this.file = file;

        this.loader = YamlConfigurationLoader
                .builder()
                .nodeStyle(NodeStyle.BLOCK)
                .file(file)
                .build();
    }

    /**
     * Loads config.yml from the plugin folder or creates it if missing.
     */
    public void load() {
        try {
            if (!file.exists()) addon.saveResource(file.getName(), false);
            ConfigurationNode node = loader.load();
            this.configNode = node.get(ConfigNode.class, new ConfigNode());
        } catch (ConfigurateException e) {
            addon.getLogger().log(Level.WARNING, "Error with loading configuration", e);
        }
    }

    public ConfigNode node() {
        return configNode;
    }

    public YamlConfigurationLoader getLoader() {
        return loader;
    }

    public File getFile() {
        return file;
    }
}
