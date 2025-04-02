package com.wairesd.dceverydaycase;

import com.jodexindustries.donatecase.api.addon.InternalJavaAddon;
import com.jodexindustries.donatecase.api.DCAPI;
import com.wairesd.dceverydaycase.db.DatabaseManager;
import com.wairesd.dceverydaycase.events.OpenCaseListener;
import com.wairesd.dceverydaycase.events.PlayerJoinListener;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import com.wairesd.dceverydaycase.tools.Config;
import com.wairesd.dceverydaycase.tools.DCEveryDayCaseExpansion;
import com.wairesd.dceverydaycase.tools.DailyCasePlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Точка входа плагина. Инициализирует компоненты, регистрирует обработчики событий и плейсхолдер.
 */
public final class DCEveryDayCaseAddon extends InternalJavaAddon implements DailyCasePlugin {
    private DatabaseManager dbManager;
    private DailyCaseService dailyCaseService;
    private Map<String, Long> lastClaimTimes = new HashMap<>();
    private boolean initialized = false;
    private JavaPlugin donateCasePlugin;
    private String caseName;
    private final Logger logger = getLogger();

    @Override
    public void onLoad() {
        Config config = new Config(this);
        caseName = config.getCaseName();

        donateCasePlugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("DonateCase");
        if (donateCasePlugin == null) {
            logger.severe("Плагин DonateCase не найден!");
            return;
        }

        dbManager = new DatabaseManager(donateCasePlugin);
        dbManager.init();
        lastClaimTimes = dbManager.loadNextClaimTimes();

        long claimCooldown = config.getClaimCooldown();
        int keysAmount = config.getKeysAmount();
        boolean debug = config.isDebug();

        dailyCaseService = new DailyCaseService(donateCasePlugin, DCAPI.getInstance(), lastClaimTimes,
                claimCooldown, caseName, keysAmount, debug);
        initialized = true;
    }

    @Override
    public void onEnable() {
        logger.info("DCEveryDayCaseAddon включён!");
        if (!initialized || donateCasePlugin == null || !donateCasePlugin.isEnabled()) {
            logger.severe("Ошибка инициализации или DonateCase не включён!");
            return;
        }

        // Регистрируем обработчики событий
        DCAPI.getInstance().getEventBus().register(new OpenCaseListener(dailyCaseService, caseName));
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(dailyCaseService, caseName), donateCasePlugin);

        // Регистрируем плейсхолдер
        DCEveryDayCaseExpansion expansion = new DCEveryDayCaseExpansion(this, donateCasePlugin);
        if (expansion.register()) {
            logger.info("Placeholder expansion успешно зарегистрирован!");
        } else {
            logger.warning("Ошибка регистрации Placeholder expansion!");
        }
        dailyCaseService.startScheduler();
    }

    @Override
    public void onDisable() {
        logger.info("Отключение DCEveryDayCaseAddon...");
        if (dailyCaseService != null) {
            dailyCaseService.cancelScheduler();
        }
        if (dbManager != null) {
            dbManager.asyncSaveNextClaimTimes(dailyCaseService.getNextClaimTimes(), () -> {
                dbManager.close();
                logger.info("Соединение с БД успешно закрыто.");
            });
        }
        initialized = false;
    }

    @Override
    public DCAPI getDCAPI() {
        return DCAPI.getInstance();
    }

    @Override
    public DailyCaseService getDailyCaseService() {
        return dailyCaseService;
    }
}
