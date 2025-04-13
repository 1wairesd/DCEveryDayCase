package com.wairesd.dceverydaycase;

import com.jodexindustries.donatecase.api.addon.InternalJavaAddon;
import com.jodexindustries.donatecase.api.DCAPI;
import com.jodexindustries.donatecase.api.data.subcommand.SubCommand;
import com.jodexindustries.donatecase.api.data.subcommand.SubCommandType;
import com.jodexindustries.donatecase.api.event.plugin.DonateCaseReloadEvent;
import com.jodexindustries.donatecase.api.event.Subscriber;
import com.jodexindustries.donatecase.api.scheduler.SchedulerTask;
import com.jodexindustries.donatecase.spigot.tools.BukkitUtils;
import com.wairesd.dceverydaycase.commands.EdcCommand;
import com.wairesd.dceverydaycase.db.DatabaseManager;
import com.wairesd.dceverydaycase.events.OpenCaseListener;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import com.wairesd.dceverydaycase.tools.DCEveryDayCaseExpansion;
import net.kyori.event.method.annotation.Subscribe;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import com.wairesd.dceverydaycase.config.ConfigManager;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

public final class DCEveryDayCaseAddon extends InternalJavaAddon implements Subscriber {
    private boolean isPlaceholderRegistered = false;
    private final DCAPI dcapi = DCAPI.getInstance();
    private final ConfigManager config = new ConfigManager(new File(getDataFolder(), "config.yml"), this);
    private DatabaseManager dbManager;
    private DailyCaseService dailyCaseService;
    private final Plugin donateCasePlugin = BukkitUtils.getDonateCase();
    private final Logger logger = getLogger();
    private SchedulerTask saveTask;

    @Override
    public void onLoad() {
        config.load();
        dbManager = new DatabaseManager(this);
        dbManager.init();
        Map<String, Long> lastClaimTimes = dbManager.loadNextClaimTimes();
        dailyCaseService = new DailyCaseService(this, dcapi, lastClaimTimes,
                config.getClaimCooldown() * 1000, config.getCaseName(), config.getKeysAmount(), config.isDebug());
    }

    @Override
    public void onEnable() {
        boolean offLogic = config.isTurnOffDailyCaseLogic();
        String newPlayerChoice = config.getNewPlayerChoice();

        // Skip enabling if daily case logic is off and player choice is timer
        if (offLogic && newPlayerChoice.equalsIgnoreCase("timer")) {
            logger.warning("The logic of daily cases is also disabled for the new players 'timer'. The plugin will not start.");
            return;
        }

        logger.info("Launch DCEveryDayCaseAddon...");
        dcapi.getEventBus().register(new OpenCaseListener(dailyCaseService, config.getCaseName(), this));

        EdcCommand executor = new EdcCommand(this);
        SubCommand command = SubCommand.builder()
                .name("edc")
                .addon(this)
                .permission(SubCommandType.PLAYER.permission)
                .executor(executor)
                .tabCompleter(executor)
                .args(new String[]{"(granted)"})
                .description("The team of the daily case")
                .build();
        dcapi.getSubCommandManager().register(command);

        // Register PlaceholderAPI if enabled
        if (!offLogic && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            registerPlaceholder();
        }

        // Start the scheduler for the daily case service
        if (!offLogic) {
            dailyCaseService.startScheduler();
        }

        saveTask = dcapi.getPlatform().getScheduler().run(this, () ->
                dbManager.asyncSaveNextClaimTimes(dailyCaseService.getNextClaimTimes(), () -> {
                    if (config.isDebug()) logger.info("Data saved in the database");
                }), 6000L, 6000L);

        logger.info("DCEveryDayCaseAddon included");

        // Register the reload event listener
        dcapi.getEventBus().register(this);
    }

    @Override
    public void onDisable() {
        logger.info("Disconnect DCEveryDayCaseAddon...");
        dailyCaseService.cancelScheduler();
        if (saveTask != null) dcapi.getPlatform().getScheduler().cancel(saveTask.getTaskId());
        dbManager.asyncSaveNextClaimTimes(dailyCaseService.getNextClaimTimes(), () -> {
            dbManager.close();
            if (config.isDebug()) logger.info("Database connection is closed");
        });
    }

    @Subscribe
    public void onReload(DonateCaseReloadEvent event) {

        if (event.type() == DonateCaseReloadEvent.Type.CONFIG) {
            reloadConfig();
            reloadData();
        logger.info("Config reloaded!");
        }
    }

    /**
     * Reloads the database and daily case service.
     */
    private void reloadConfig() {
        config.load();
        if (config.isTurnOffDailyCaseLogic()) {
            unregisterPlaceholder();
        } else {
            updatePlaceholder();
        }
    }

    /**
     * Reloads the database and daily case service.
     */
    private void reloadData() {
        dbManager.reload();
        dailyCaseService.reload();
    }

    /**
     * Registers the placeholder if it's not already registered.
     */
    private void registerPlaceholder() {
        if (!isPlaceholderRegistered) {
            DCEveryDayCaseExpansion expansion = new DCEveryDayCaseExpansion(this);
            if (expansion.register()) {
                logger.info("Expansion Placeholder is registered.");
                isPlaceholderRegistered = true;
            } else {
                logger.warning("It was not possible to register the extension of PlaceHolder.");
            }
        }
    }

    /**
     * Unregisters the placeholder if it's registered.
     */
    private void unregisterPlaceholder() {
        if (isPlaceholderRegistered) {
            DCEveryDayCaseExpansion expansion = new DCEveryDayCaseExpansion(this);
            if (expansion.unregister()) {
                logger.info("Expansion Placeholder is deleted.");
                isPlaceholderRegistered = false;
            } else {
                logger.warning("It was not possible to delete the extension of Placeholder.");
            }
        }
    }

    /**
     * Updates the placeholder based on the current config setting.
     */
    private void updatePlaceholder() {
        boolean offLogic = config.isTurnOffDailyCaseLogic();
        if (offLogic) {
            unregisterPlaceholder();
        } else {
            registerPlaceholder();
        }
    }

    public DCAPI getDCAPI() { return dcapi; }
    public DailyCaseService getDailyCaseService() { return dailyCaseService; }
    public Plugin getPluginInstance() { return donateCasePlugin; }
    public DatabaseManager getDatabaseManager() { return dbManager; }
    public ConfigManager getConfig() { return config; }
}