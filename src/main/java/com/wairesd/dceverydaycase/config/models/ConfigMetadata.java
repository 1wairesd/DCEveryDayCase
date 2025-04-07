package com.wairesd.dceverydaycase.config.models;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

/**
 * Stores metadata about the configuration, such as version and type.
 */
@ConfigSerializable
public class ConfigMetadata {
    @Setting
    public int version = 1;

    @Setting
    public String type = "CONFIG";
}