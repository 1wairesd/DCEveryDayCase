package com.wairesd.dceverydaycase.events;

import com.wairesd.dceverydaycase.service.DailyCaseService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Обработчик события входа игрока.
 * Если для нового игрока отсутствует запись, производится выдача кейса.
 */
public class PlayerJoinListener implements Listener {
    private final DailyCaseService service;

    public PlayerJoinListener(DailyCaseService service) {
        this.service = service;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!service.getNextClaimTimes().containsKey(player.getName())) {
            service.giveGift(player);
        }
    }
}
