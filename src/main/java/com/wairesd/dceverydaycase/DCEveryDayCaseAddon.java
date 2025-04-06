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
import com.wairesd.dceverydaycase.config.Config;
import com.wairesd.dceverydaycase.config.ConfigMigrator;
import com.wairesd.dceverydaycase.tools.DCEveryDayCaseExpansion;
import net.kyori.event.method.annotation.Subscribe;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

public final class DCEveryDayCaseAddon extends InternalJavaAddon implements Subscriber {
    private boolean isPlaceholderRegistered = false;
    private final DCAPI dcapi = DCAPI.getInstance();
    private final Config config = new Config(new File(getDataFolder(), "config.yml"), this);
    private DatabaseManager dbManager;
    private DailyCaseService dailyCaseService;
    private final Plugin donateCasePlugin = BukkitUtils.getDonateCase();
    private final Logger logger = getLogger();
    private SchedulerTask saveTask;
    private ConfigMigrator ConfigMigrator;

    @Override
    public void onLoad() {
        config.load();
        dbManager = new DatabaseManager(this);
        dbManager.init();
        Map<String, Long> lastClaimTimes = dbManager.loadNextClaimTimes();
        dailyCaseService = new DailyCaseService(this, dcapi, lastClaimTimes,
                config.node().claimCooldown * 1000, config.node().caseName, config.node().keysAmount, config.node().debug);
    }

    @Override
    public void onEnable() {
        ConfigMigrator ConfigMigrator = new ConfigMigrator(this, config);
        ConfigMigrator.migrateConfig();

        boolean offLogic = config.node().OffLogicDailyCase;
        String newPlayerChoice = config.node().newPlayerChoice;

        // Skip enabling if daily case logic is off and player choice is timer
        if (offLogic && newPlayerChoice.equalsIgnoreCase("timer")) {
            logger.warning("Daily case logic is turned off and new player choice is 'timer'. Plugin will not start.");
            return;
        }

        logger.info("Starting DCEveryDayCaseAddon...");
        dcapi.getEventBus().register(new OpenCaseListener(dailyCaseService, config.node().caseName, this));

        EdcCommand executor = new EdcCommand(this);
        SubCommand command = SubCommand.builder()
                .name("edc")
                .addon(this)
                .permission(SubCommandType.PLAYER.permission)
                .executor(executor)
                .tabCompleter(executor)
                .args(new String[]{"(granted)"}).description("Daily case command")
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
                    if (config.node().debug) logger.info("Data stored in the database");
                }), 6000L, 6000L);

        logger.info("DCEveryDayCaseAddon Enabled");

        // Register the reload event listener
        dcapi.getEventBus().register(this);
    }

    @Override
    public void onDisable() {
        logger.info("Disabling DCEveryDayCaseAddon...");
        dailyCaseService.cancelScheduler();
        if (saveTask != null) dcapi.getPlatform().getScheduler().cancel(saveTask.getTaskId());
        dbManager.asyncSaveNextClaimTimes(dailyCaseService.getNextClaimTimes(), () -> {
            dbManager.close();
            if (config.node().debug) logger.info("Database connection closed");
        });
    }

    @Subscribe
    public void onReload(DonateCaseReloadEvent event) {
        // Trigger migration when the config is reloaded
        reloadConfig();

        // Migrate the configuration if needed
        ConfigMigrator migrator = new ConfigMigrator(this, config);
        migrator.migrateConfig();  // Run the migration

        // Handle other reload actions (like updating placeholders)
        if (event.type() == DonateCaseReloadEvent.Type.CONFIG) {
            reloadConfig();
        }

        // Update placeholder if needed
        if (event.type() == DonateCaseReloadEvent.Type.CONFIG) {
            updatePlaceholder();
        }
    }

    /**
     * Reloads the configuration and updates the placeholder.
     */
    private void reloadConfig() {
        config.load();
        if (config.node().OffLogicDailyCase) {
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
                logger.info("Placeholder expansion registered.");
                isPlaceholderRegistered = true;
            } else {
                logger.warning("Placeholder expansion registration failed.");
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
                logger.info("Placeholder expansion unregistered.");
                isPlaceholderRegistered = false;
            } else {
                logger.warning("Placeholder expansion unregistration failed.");
            }
        }
    }

    /**
     * Updates the placeholder based on the current config setting.
     */
    private void updatePlaceholder() {
        boolean offLogic = config.node().OffLogicDailyCase;
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
    public Config getConfig() { return config; }
}
