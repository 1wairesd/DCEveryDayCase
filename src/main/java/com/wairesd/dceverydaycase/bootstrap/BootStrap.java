package com.wairesd.dceverydaycase.bootstrap;

import com.jodexindustries.donatecase.api.DCAPI;
import com.jodexindustries.donatecase.api.data.subcommand.SubCommand;
import com.jodexindustries.donatecase.api.data.subcommand.SubCommandType;
import com.jodexindustries.donatecase.api.event.Subscriber;
import com.jodexindustries.donatecase.api.event.plugin.DonateCaseReloadEvent;
import com.jodexindustries.donatecase.api.scheduler.SchedulerTask;
import com.jodexindustries.donatecase.spigot.tools.BukkitUtils;
import com.wairesd.dceverydaycase.commands.EdcCommand;
import com.wairesd.dceverydaycase.config.ConfigManager;
import com.wairesd.dceverydaycase.db.DatabaseManager;
import com.wairesd.dceverydaycase.events.OpenCaseListener;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import com.wairesd.dceverydaycase.tools.DCEveryDayCaseExpansion;
import com.wairesd.dceverydaycase.api.DCEDCAPI;
import com.wairesd.dceverydaycase.api.DCEDCAPIImpl;
import lombok.Getter;
import net.kyori.event.method.annotation.Subscribe;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

@Getter
public class BootStrap implements Subscriber {
    private final Main plugin;
    private boolean isPlaceholderRegistered = false;
    private final DCAPI dcapi = DCAPI.getInstance();
    private final ConfigManager config;
    private DatabaseManager dbManager;
    private DailyCaseService dailyCaseService;
    private final Plugin donateCasePlugin;
    private final Logger logger;
    private SchedulerTask saveTask;

    public BootStrap(Main plugin) {
        this.plugin = plugin;
        this.donateCasePlugin = BukkitUtils.getDonateCase();
        this.logger = plugin.getPlugin().getLogger();
        this.config = new ConfigManager(new File(plugin.getPlugin().getDataFolder(), "config.yml"), plugin);
    }

    public void load() {
        config.load();
        dbManager = new DatabaseManager(plugin);
        dbManager.init();
        Map<String, Long> lastClaimTimes = dbManager.loadNextClaimTimes();
        dailyCaseService = new DailyCaseService(plugin, dcapi, lastClaimTimes,
                config.getClaimCooldown() * 1000, config.getCaseName(), config.getKeysAmount(), config.isDebug());
    }

    public void enable() {
        boolean offLogic = config.isTurnOffDailyCaseLogic();
        String newPlayerChoice = config.getNewPlayerChoice();

        if (offLogic && newPlayerChoice.equalsIgnoreCase("timer")) {
            logger.warning("The logic of daily cases is also disabled for the new players 'timer'. The plugin will not start.");
            return;
        }

        logger.info("Launch DCEveryDayCaseAddon...");
        dcapi.getEventBus().register(new OpenCaseListener(dailyCaseService, config.getCaseName(), plugin));

        EdcCommand executor = new EdcCommand(plugin);
        if (plugin.getAddon() != null) {
            SubCommand command = SubCommand.builder()
                    .name("edc")
                    .addon(plugin.getAddon())
                    .permission(SubCommandType.PLAYER.permission)
                    .executor(executor)
                    .tabCompleter(executor)
                    .args(new String[]{"(granted)"})
                    .description("The team of the daily case")
                    .build();
            dcapi.getSubCommandManager().register(command);
        }

        if (!offLogic && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            registerPlaceholder();
        }

        if (!offLogic) {
            dailyCaseService.startScheduler();
        }

        if (plugin.getAddon() != null) {
            saveTask = dcapi.getPlatform().getScheduler().run(plugin.getAddon(), () ->
                    dbManager.asyncSaveNextClaimTimes(dailyCaseService.getNextClaimTimes(), () -> {
                        if (config.isDebug()) logger.info("Data saved in the database");
                    }), 6000L, 6000L);
        }

        logger.info("DCEveryDayCaseAddon included");

        DCEDCAPI.setInstance(new DCEDCAPIImpl(dailyCaseService));

        dcapi.getEventBus().register(this);
    }

    public void unload() {
        logger.info("Disconnect DCEveryDayCaseAddon...");
        dailyCaseService.cancelScheduler();
        if (saveTask != null) dcapi.getPlatform().getScheduler().cancel(saveTask.getTaskId(), false);
        dbManager.asyncSaveNextClaimTimes(dailyCaseService.getNextClaimTimes(), () -> {
            dbManager.close();
            if (config.isDebug()) logger.info("Database connection is closed");
        });

        DCEDCAPI.setInstance(null);
    }

    @Subscribe
    public void onReload(DonateCaseReloadEvent event) {
        if (event.type() == DonateCaseReloadEvent.Type.CONFIG) {
            reloadConfig();
            reloadData();
        }
    }

    private void reloadConfig() {
        config.load();
        if (config.isTurnOffDailyCaseLogic()) {
            unregisterPlaceholder();
        } else {
            updatePlaceholder();
        }
    }

    private void reloadData() {
        dbManager.reload();
        dailyCaseService.reload();
    }

    private void registerPlaceholder() {
        if (!isPlaceholderRegistered) {
            DCEveryDayCaseExpansion expansion = new DCEveryDayCaseExpansion(plugin);
            if (expansion.register()) {
                logger.info("Expansion Placeholder is registered.");
                isPlaceholderRegistered = true;
            } else {
                logger.warning("It was not possible to register the extension of PlaceHolder.");
            }
        }
    }

    private void unregisterPlaceholder() {
        if (isPlaceholderRegistered) {
            DCEveryDayCaseExpansion expansion = new DCEveryDayCaseExpansion(plugin);
            if (expansion.unregister()) {
                logger.info("Expansion Placeholder is deleted.");
                isPlaceholderRegistered = false;
            } else {
                logger.warning("It was not possible to delete the extension of Placeholder.");
            }
        }
    }

    private void updatePlaceholder() {
        boolean offLogic = config.isTurnOffDailyCaseLogic();
        if (offLogic) {
            unregisterPlaceholder();
        } else {
            registerPlaceholder();
        }
    }

    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    public ConfigManager getConfig() {
        return config;
    }

    public DailyCaseService getDailyCaseService() {
        return dailyCaseService;
    }

}