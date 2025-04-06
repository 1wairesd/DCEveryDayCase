package com.wairesd.dceverydaycase.events;

import com.jodexindustries.donatecase.api.event.Subscriber;
import com.jodexindustries.donatecase.api.event.player.JoinEvent;
import com.jodexindustries.donatecase.api.event.player.OpenCaseEvent;
import com.jodexindustries.donatecase.spigot.tools.BukkitUtils;
import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import net.kyori.event.method.annotation.Subscribe;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class OpenCaseListener implements Subscriber {
    private final DailyCaseService service;
    private final String targetCaseName;
    private final DCEveryDayCaseAddon addon;

    public OpenCaseListener(DailyCaseService service, String targetCaseName, DCEveryDayCaseAddon addon) {
        this.service = service;
        this.targetCaseName = targetCaseName;
        this.addon = addon;
    }

    @Subscribe
    public void onCaseOpen(OpenCaseEvent event) {
        Player player = BukkitUtils.toBukkit(event.player());
        if (!event.caseData().caseType().equalsIgnoreCase(targetCaseName)) return;
        Bukkit.getScheduler().runTaskLater(service.getPlugin(), () -> {
            if (service.getDCAPI().getCaseKeyManager().getCache(targetCaseName, player.getName()) == 0)
                service.resetTimer(player.getName());
        }, 2L);
    }

    @Subscribe
    public void onPlayerJoin(JoinEvent event) {
        var player = event.player();
        if (!service.getNextClaimTimes().containsKey(player.getName())) {
            long nextTime = System.currentTimeMillis() + service.getClaimCooldown();
            if ("case".equalsIgnoreCase(addon.getConfig().node().newPlayerChoice))
                service.giveGift(player);
            service.getNextClaimTimes().put(player.getName(), nextTime);
            addon.getDatabaseManager().asyncSaveNextClaimTimes(service.getNextClaimTimes(), () -> {
                if (addon.getConfig().node().debug)
                    addon.getLogger().info("Next claim times updated in database.");
            });
        }
    }
}
