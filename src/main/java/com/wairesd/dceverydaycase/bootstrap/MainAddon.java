package com.wairesd.dceverydaycase.bootstrap;

import com.jodexindustries.donatecase.api.DCAPI;
import com.jodexindustries.donatecase.api.addon.Addon;
import com.jodexindustries.donatecase.api.addon.InternalJavaAddon;
import com.jodexindustries.donatecase.spigot.tools.BukkitUtils;
import com.wairesd.dceverydaycase.DCEveryDayCase;
import com.wairesd.dceverydaycase.config.ConfigManager;
import com.wairesd.dceverydaycase.db.DatabaseManager;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import org.bukkit.plugin.Plugin;

public class MainAddon extends InternalJavaAddon implements Main {
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
        return BukkitUtils.getDonateCase();
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
        return getPlatform().getAPI();
    }

    @Override
    public Addon getAddon() {
        return this;
    }

    @Override
    public DailyCaseService getDailyCaseService() {
        return core.getDailyCaseService();
    }
}
