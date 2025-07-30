package com.wairesd.dceverydaycase;

import com.jodexindustries.donatecase.api.addon.InternalJavaAddon;
import com.jodexindustries.donatecase.api.event.Subscriber;
import com.wairesd.dceverydaycase.bootstrap.BootStrap;
import com.wairesd.dceverydaycase.bootstrap.Main;

public final class DCEveryDayCaseAddon extends InternalJavaAddon implements Subscriber, Main {
    private BootStrap bootstrap;

    @Override
    public void onLoad() {
        bootstrap = new BootStrap(this);
        bootstrap.load();
    }

    @Override
    public void onEnable() {
        bootstrap.enable();
    }

    @Override
    public void onDisable() {
        bootstrap.unload();
    }

    @Override
    public org.bukkit.plugin.Plugin getPlugin() {
        return com.jodexindustries.donatecase.spigot.tools.BukkitUtils.getDonateCase();
    }

    @Override
    public com.jodexindustries.donatecase.api.manager.CaseManager getCaseManager() {
        return getPlatform().getAPI().getCaseManager();
    }

    @Override
    public org.bukkit.plugin.Plugin getPluginInstance() {
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