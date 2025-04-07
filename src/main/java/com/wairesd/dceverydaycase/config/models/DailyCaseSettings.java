package com.wairesd.dceverydaycase.config.models;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

/**
 * Contains settings specific to the daily case feature.
 */
@ConfigSerializable
public class DailyCaseSettings {
    @Setting("case_settings")
    public CaseSettings caseSettings = new CaseSettings();

    @Setting("debug")
    public boolean debug = false;

    @Setting("Languages")
    public String languages = "en_US";
}