package com.wairesd.dceverydaycase.bootstrap;

import com.jodexindustries.donatecase.api.DCAPI;
import com.jodexindustries.donatecase.api.addon.Addon;
import com.wairesd.dceverydaycase.DCEveryDayCase;
import com.wairesd.dceverydaycase.config.ConfigManager;
import com.wairesd.dceverydaycase.db.DatabaseManager;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MainPlugin extends JavaPlugin implements Main {
    private DCEveryDayCase core;

    @Override
    public void onEnable() {
        core = new DCEveryDayCase(this);
        core.load();
        core.enable();
    }

    @Override
    public void onDisable() {
        if (core != null) core.unload();
    }

    @Override
    public Plugin getPlugin() {
        return this;
    }

    @Override
    public ConfigManager getConfigManager() {
        return core.getConfig();
    }

    @Override
    public DatabaseManager getDatabaseManager() {
        return core.getDatabaseManager();
    }

    @Override
    public DCAPI getDCAPI() {
        return DCAPI.getInstance();
    }

    @Override
    public Addon getAddon() {
        return null;
    }

    @Override
    public DailyCaseService getDailyCaseService() {
        return core.getDailyCaseService();
    }
}
