package com.wairesd.dceverydaycase.events;

import com.wairesd.dceverydaycase.service.DailyCaseService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Processor of the player's entrance event.
 * If there is no record for the new player, the case is issued.
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
