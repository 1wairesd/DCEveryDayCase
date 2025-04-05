package com.wairesd.dceverydaycase.events;

import com.jodexindustries.donatecase.api.event.Subscriber;
import com.jodexindustries.donatecase.api.event.player.OpenCaseEvent;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import com.jodexindustries.donatecase.spigot.tools.BukkitUtils;
import net.kyori.event.method.annotation.Subscribe;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Listens for case opening events to reset timers when conditions match. */
public class OpenCaseListener implements Subscriber {
    private final DailyCaseService service;
    private final String targetCaseName;

    public OpenCaseListener(DailyCaseService service, String targetCaseName) {
        this.service = service;
        this.targetCaseName = targetCaseName;
    }

    @Subscribe
    public void onCaseOpen(OpenCaseEvent event) {
        Player player = BukkitUtils.toBukkit(event.player());
        if (!event.caseData().caseType().equalsIgnoreCase(targetCaseName)) return;
        // Schedule a task to reset timer if player has no keys.
        Bukkit.getScheduler().runTaskLater(service.getPlugin(), () -> {
            if (service.getDCAPI().getCaseKeyManager().getCache(targetCaseName, player.getName()) == 0)
                service.resetTimer(player.getName());
        }, 2L);
    }
}
