package com.wairesd.dceverydaycase.bootstrap;

import com.jodexindustries.donatecase.api.addon.InternalJavaAddon;
import com.jodexindustries.donatecase.api.manager.CaseManager;
import org.bukkit.plugin.Plugin;

public class MainAddon extends InternalJavaAddon implements Main {
    private BootStrap bootstrap;
    private CaseManager caseManager;

    @Override
    public void onEnable() {
        caseManager = getPlatform().getAPI().getCaseManager();
        bootstrap = new BootStrap(this);
        bootstrap.load();
        bootstrap.enable();
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.unload();
        }
    }

    @Override
    public Plugin getPlugin() {
        return com.jodexindustries.donatecase.spigot.tools.BukkitUtils.getDonateCase();
    }

    @Override
    public CaseManager getCaseManager() {
        return caseManager;
    }

    @Override
    public Plugin getPluginInstance() {
        return com.jodexindustries.donatecase.spigot.tools.BukkitUtils.getDonateCase();
    }

    @Override
    public com.wairesd.dceverydaycase.config.ConfigManager getConfigManager() {
        return bootstrap.getConfig();
    }

    @Override
    public com.wairesd.dceverydaycase.db.DatabaseManager getDatabaseManager() {
        return bootstrap.getDatabaseManager();
    }

    @Override
    public com.jodexindustries.donatecase.api.DCAPI getDCAPI() {
        return getPlatform().getAPI();
    }

    @Override
    public com.jodexindustries.donatecase.api.addon.Addon getAddon() {
        return this;
    }

    @Override
    public com.wairesd.dceverydaycase.service.DailyCaseService getDailyCaseService() {
        return bootstrap.getDailyCaseService();
    }
}