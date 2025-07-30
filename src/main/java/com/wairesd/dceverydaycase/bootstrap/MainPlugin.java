package com.wairesd.dceverydaycase.bootstrap;

import com.jodexindustries.donatecase.api.DCAPI;
import com.jodexindustries.donatecase.api.manager.CaseManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MainPlugin extends JavaPlugin implements Main {
    private BootStrap bootstrap;
    private CaseManager caseManager;

    @Override
    public void onEnable() {
        caseManager = DCAPI.getInstance().getCaseManager();
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
        return this;
    }

    @Override
    public CaseManager getCaseManager() {
        return caseManager;
    }

    @Override
    public Plugin getPluginInstance() {
        return this;
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
        return DCAPI.getInstance();
    }

    @Override
    public com.jodexindustries.donatecase.api.addon.Addon getAddon() {
        return null;
    }

    @Override
    public com.wairesd.dceverydaycase.service.DailyCaseService getDailyCaseService() {
        return bootstrap.getDailyCaseService();
    }

    @Override
    public java.util.logging.Logger getLogger() {
        return super.getLogger();
    }
}