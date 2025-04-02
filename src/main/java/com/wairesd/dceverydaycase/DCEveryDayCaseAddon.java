package com.wairesd.dceverydaycase;

import com.jodexindustries.donatecase.api.addon.InternalJavaAddon;
import com.jodexindustries.donatecase.api.DCAPI;
import com.jodexindustries.donatecase.spigot.tools.BukkitUtils;
import com.wairesd.dceverydaycase.db.DatabaseManager;
import com.wairesd.dceverydaycase.events.OpenCaseListener;
import com.wairesd.dceverydaycase.events.PlayerJoinListener;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import com.wairesd.dceverydaycase.tools.Config;
import com.wairesd.dceverydaycase.tools.DCEveryDayCaseExpansion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Точка входа плагина. Инициализирует компоненты, регистрирует обработчики событий и плейсхолдер.
 */
public final class DCEveryDayCaseAddon extends InternalJavaAddon {

    private final DCAPI dcapi = DCAPI.getInstance();
    private final Config config = new Config(this);

    private DatabaseManager dbManager;
    private DailyCaseService dailyCaseService;
    private final Plugin donateCasePlugin = BukkitUtils.getDonateCase();
    private final Logger logger = getLogger();

    @Override
    public void onLoad() {
        config.load();

        dbManager = new DatabaseManager(this);
        dbManager.init();
        Map<String, Long> lastClaimTimes = dbManager.loadNextClaimTimes();

        long claimCooldown = config.getClaimCooldown();
        int keysAmount = config.getKeysAmount();
        boolean debug = config.isDebug();

        dailyCaseService = new DailyCaseService(this, DCAPI.getInstance(), lastClaimTimes,
                claimCooldown, config.getCaseName(), keysAmount, debug);
    }

    @Override
    public void onEnable() {
        logger.info("DCEveryDayCaseAddon включён!");

        // Регистрируем обработчики событий
        DCAPI.getInstance().getEventBus().register(new OpenCaseListener(dailyCaseService, config.getCaseName()));
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(dailyCaseService), donateCasePlugin);

        // Регистрируем плейсхолдер
        if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            DCEveryDayCaseExpansion expansion = new DCEveryDayCaseExpansion(this);
            if (expansion.register()) {
                logger.info("Placeholder expansion успешно зарегистрирован!");
            } else {
                logger.warning("Ошибка регистрации Placeholder expansion!");
            }
        }
        dailyCaseService.startScheduler();
    }

    @Override
    public void onDisable() {
        logger.info("Отключение DCEveryDayCaseAddon...");
        dailyCaseService.cancelScheduler();

        if (dbManager != null) {
            dbManager.asyncSaveNextClaimTimes(dailyCaseService.getNextClaimTimes(), () -> {
                dbManager.close();
                logger.info("Соединение с БД успешно закрыто.");
            });
        }
    }

    public DCAPI getDCAPI() {
        return dcapi;
    }

    public DailyCaseService getDailyCaseService() {
        return dailyCaseService;
    }

    public Config getConfig() {
        return config;
    }
}
