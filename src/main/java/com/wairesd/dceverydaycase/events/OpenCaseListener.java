package com.wairesd.dceverydaycase.events;

import com.jodexindustries.donatecase.api.event.Subscriber;
import com.jodexindustries.donatecase.api.event.player.OpenCaseEvent;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import net.kyori.event.method.annotation.Subscribe;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Обработчик события открытия кейса.
 * При открытии кейса проверяет наличие ключей и обновляет таймер, если ключей нет.
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
        if (event.player() == null) return;
        Player player = Bukkit.getPlayer(event.player().getUniqueId());
        if (player == null) return;
        if (!event.caseData().caseType().equalsIgnoreCase(targetCaseName)) return;
        int keys = service.getDCAPI().getCaseKeyManager().getCache(targetCaseName, player.getName());
        if (keys == 0) {
            service.resetTimer(player.getName());
        }
    }
}
