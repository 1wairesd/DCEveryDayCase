package com.wairesd.dceverydaycase.bootstrap;

import com.jodexindustries.donatecase.api.DCAPI;
import com.jodexindustries.donatecase.api.addon.Addon;
import com.wairesd.dceverydaycase.config.ConfigManager;
import com.wairesd.dceverydaycase.db.DatabaseManager;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import org.bukkit.plugin.Plugin;

public interface Main {
    Plugin getPlugin();
    ConfigManager getConfigManager();
    DatabaseManager getDatabaseManager();
    DCAPI getDCAPI();
    Addon getAddon();
    DailyCaseService getDailyCaseService();
}
