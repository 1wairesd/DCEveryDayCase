package com.wairesd.dceverydaycase;

import com.jodexindustries.donatecase.api.addon.InternalJavaAddon;
import com.jodexindustries.donatecase.api.DCAPI;
import com.jodexindustries.donatecase.api.data.subcommand.SubCommand;
import com.jodexindustries.donatecase.api.data.subcommand.SubCommandType;
import com.jodexindustries.donatecase.api.scheduler.SchedulerTask;
import com.jodexindustries.donatecase.spigot.tools.BukkitUtils;
import com.wairesd.dceverydaycase.commands.EdcCommand;
import com.wairesd.dceverydaycase.db.DatabaseManager;
import com.wairesd.dceverydaycase.events.OpenCaseListener;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import com.wairesd.dceverydaycase.config.Config;
import com.wairesd.dceverydaycase.tools.DCEveryDayCaseExpansion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Main class for the plugin, responsible for initialization and registering components.
 */
public final class DCEveryDayCaseAddon extends InternalJavaAddon {
    private final DCAPI dcapi = DCAPI.getInstance();
    private final Config config = new Config(new File(getDataFolder(), "config.yml"), this);
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
                config.node().claimCooldown * 1000, config.node().caseName, config.node().keysAmount, config.node().debug);
    }

    @Override
    public void onEnable() {
        // Register event listeners
        dcapi.getEventBus().register(new OpenCaseListener(dailyCaseService, config.node().caseName, this));

        // Register the /edc command
        EdcCommand executor = new EdcCommand(this);
        SubCommand command = SubCommand.builder()
                .name("edc")
                .addon(this)
                .permission(SubCommandType.PLAYER.permission)
                .executor(executor)
                .tabCompleter(executor)
                .args(new String[]{"(granted)"})
                .description("Daily case command")
                .build();
        dcapi.getSubCommandManager().register(command);

        logger.info("DCEveryDayCaseAddon Enabled");

        // Register placeholder expansion if PlaceholderAPI is enabled
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            DCEveryDayCaseExpansion expansion = new DCEveryDayCaseExpansion(this);
            if (expansion.register())
                logger.info("Placeholder expansion registered");
            else
                logger.warning("Placeholder expansion registration failed");
        }

        // Start the daily case scheduler
        dailyCaseService.startScheduler();

        // Periodically save claim times to the database
        saveTask = dcapi.getPlatform().getScheduler().run(this, () -> dbManager.asyncSaveNextClaimTimes(dailyCaseService.getNextClaimTimes(), () -> {
            if (config.node().debug)
                logger.info("Data stored in the database");
        }), 6000L, 6000L);
    }

    @Override
    public void onDisable() {
        logger.info("Disabling DCEveryDayCaseAddon...");
        dailyCaseService.cancelScheduler();
        dcapi.getPlatform().getScheduler().cancel(saveTask.getTaskId());
        // Save data and close the database connection
        // TODO Incorrect asynchronous saving when DonateCase is disabled (move to main thread)
        dbManager.asyncSaveNextClaimTimes(dailyCaseService.getNextClaimTimes(), () -> {
            dbManager.close();
            if (config.node().debug)
                logger.info("Database connection closed");
        });
    }

    // Getter methods for various components
    public DCAPI getDCAPI() { return dcapi; }
    public DailyCaseService getDailyCaseService() { return dailyCaseService; }
    public Plugin getPluginInstance() { return donateCasePlugin; }
    public DatabaseManager getDatabaseManager() { return dbManager; }
    public Config getConfig() { return config; }
}
