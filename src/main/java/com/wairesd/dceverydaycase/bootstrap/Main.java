package com.wairesd.dceverydaycase.bootstrap;

import com.jodexindustries.donatecase.api.manager.CaseManager;
import org.bukkit.plugin.Plugin;

public interface Main {
    Plugin getPlugin();
    Plugin getPluginInstance();
    CaseManager getCaseManager();
    com.wairesd.dceverydaycase.config.ConfigManager getConfigManager();
    com.wairesd.dceverydaycase.db.DatabaseManager getDatabaseManager();
    com.jodexindustries.donatecase.api.DCAPI getDCAPI();
    com.jodexindustries.donatecase.api.addon.Addon getAddon();
    com.wairesd.dceverydaycase.service.DailyCaseService getDailyCaseService();
}