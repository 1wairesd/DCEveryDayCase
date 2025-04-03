package com.wairesd.dceverydaycase.events;

import com.jodexindustries.donatecase.api.event.Subscriber;
import com.jodexindustries.donatecase.api.event.player.OpenCaseEvent;
import com.jodexindustries.donatecase.spigot.tools.BukkitUtils;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import net.kyori.event.method.annotation.Subscribe;
import org.bukkit.entity.Player;

/**
 * The processor of the case of the case of the case.
 * When opening the case, it checks the presence of keys and updates the timer if there are no keys.
 */
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
        int keys = service.getDCAPI().getCaseKeyManager().getCache(targetCaseName, player.getName());
        if (keys == 0) {
            service.resetTimer(player.getName());
        }
    }
}
