package com.wairesd.dceverydaycase.events;

import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Handles player join events to initialize daily case timers or issue immediate gifts. */
public class PlayerJoinListener implements Listener {
    private final DailyCaseService dailyCaseService;
    private final DCEveryDayCaseAddon addon;

    public PlayerJoinListener(DailyCaseService dailyCaseService, DCEveryDayCaseAddon addon) {
        this.dailyCaseService = dailyCaseService;
        this.addon = addon;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!dailyCaseService.getNextClaimTimes().containsKey(player.getName())) {
            long nextTime = System.currentTimeMillis() + dailyCaseService.getClaimCooldown();
            if ("case".equalsIgnoreCase(addon.getConfig().getNewPlayerChoice()))
                dailyCaseService.giveGift(player);
            dailyCaseService.getNextClaimTimes().put(player.getName(), nextTime);
            addon.getDatabaseManager().asyncSaveNextClaimTimes(dailyCaseService.getNextClaimTimes(), () -> {
                if (addon.getConfig().isDebug())
                    addon.getLogger().info("Next claim times updated in database.");
            });
        }
    }
}
