package com.wairesd.dceverydaycase.config.models;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

/**
 * Root configuration object that combines daily case settings and metadata.
 */
@ConfigSerializable
public class RootConfig {
    @Setting("DCEveryDayCase")
    public DailyCaseSettings dailyCaseSettings = new DailyCaseSettings();

    @Setting("config")
    public ConfigMetadata configMetadata = new ConfigMetadata();
}