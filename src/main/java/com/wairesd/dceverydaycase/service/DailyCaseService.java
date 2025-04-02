package com.wairesd.dceverydaycase.service;

import com.jodexindustries.donatecase.api.DCAPI;
import com.jodexindustries.donatecase.api.addon.Addon;
import com.jodexindustries.donatecase.api.data.database.DatabaseStatus;
import com.jodexindustries.donatecase.api.scheduler.SchedulerTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Сервис по управлению выдачей кейсов и контролю таймаута.
 */
public class DailyCaseService {
    private final Addon addon;
    private final DCAPI dcapi;
    private final Map<String, Long> nextClaimTimes;
    private final long claimCooldown;
    private final String caseName;
    private final int keysAmount;
    private final boolean debug;
    private SchedulerTask schedulerTask;
    private final Logger logger;

    public DailyCaseService(Addon addon, DCAPI dcapi, Map<String, Long> nextClaimTimes,
                            long claimCooldown, String caseName, int keysAmount, boolean debug) {
        this.addon = addon;
        this.dcapi = dcapi;
        this.nextClaimTimes = nextClaimTimes;
        this.claimCooldown = claimCooldown;
        this.caseName = caseName;
        this.keysAmount = keysAmount;
        this.debug = debug;
        this.logger = addon.getLogger();
    }

    /** Запускает планировщик, проверяющий статус игроков каждую секунду */
    public void startScheduler() {
        schedulerTask = dcapi.getPlatform().getScheduler().run(addon, () -> Bukkit.getOnlinePlayers().forEach(DailyCaseService.this::checkPlayer), 0, 20);
    }

    /** Останавливает планировщик */
    public void cancelScheduler() {
        if (schedulerTask != null) schedulerTask.cancel();
    }

    /** Проверяет, можно ли выдать ключ игроку, и выдаёт его, если таймаут истёк */
    private void checkPlayer(Player player) {
        int keys = dcapi.getCaseKeyManager().getCache(caseName, player.getName());
        if (keys > 0) return;
        long currentTime = System.currentTimeMillis();
        long nextClaim = nextClaimTimes.getOrDefault(player.getName(), currentTime + claimCooldown);
        if (currentTime >= nextClaim) {
            giveGift(player);
            resetTimer(player.getName());
        }
    }

    /** Выдаёт ключ игроку с помощью консольной команды DonateCase */
    public void giveGift(Player player) {
        dcapi.getCaseKeyManager().add(caseName, player.getName(), keysAmount).thenAccept(status -> {
            if (status == DatabaseStatus.COMPLETE && debug)
                logger.info("Выдано " + keysAmount + " ключ(ей) игроку " + player.getName() + " для кейса " + caseName);
        });
    }

    /** Обновляет время следующего получения ключа для игрока */
    public void resetTimer(String playerName) {
        nextClaimTimes.put(playerName, System.currentTimeMillis() + claimCooldown);
    }

    public Map<String, Long> getNextClaimTimes() {
        return nextClaimTimes;
    }

    public long getClaimCooldown() {
        return claimCooldown;
    }

    public DCAPI getDCAPI() {
        return dcapi;
    }

    public String getCaseName() {
        return caseName;
    }
}
