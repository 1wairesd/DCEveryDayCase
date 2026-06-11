package com.wairesd.dceverydaycase.events;

import com.jodexindustries.donatecase.api.event.Subscriber;
import com.jodexindustries.donatecase.api.event.animation.AnimationEndEvent;
import com.jodexindustries.donatecase.api.event.player.JoinEvent;
import com.jodexindustries.donatecase.api.event.player.OpenCaseEvent;
import com.jodexindustries.donatecase.api.platform.DCPlayer;
import com.jodexindustries.donatecase.spigot.tools.BukkitUtils;
import com.wairesd.dceverydaycase.bootstrap.Main;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import net.kyori.event.method.annotation.Subscribe;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class OpenCaseListener implements Subscriber {
    private final DailyCaseService service;
    private final String targetCaseName;
    private final Main addon;
    private final Set<String> resetByAnimation = ConcurrentHashMap.newKeySet();

    public OpenCaseListener(DailyCaseService service, String targetCaseName, Main addon) {
        this.service = service;
        this.targetCaseName = targetCaseName;
        this.addon = addon;
    }

    @Subscribe
    public void onAnimationEnd(AnimationEndEvent event) {
        if (!event.activeCase().caseType().equalsIgnoreCase(targetCaseName)) return;

        String playerName = event.activeCase().player().getName();
        resetByAnimation.add(playerName);
        service.resetTimer(playerName);
    }

    @Subscribe
    public void onCaseOpen(OpenCaseEvent event) {
        if (!event.definition().settings().type().equalsIgnoreCase(targetCaseName)) return;

        DCPlayer dcPlayer = event.player();
        Player player = BukkitUtils.toBukkit(dcPlayer);
        String playerName = player.getName();

        if (resetByAnimation.remove(playerName)) return; 

        service.resetTimer(playerName);
    }

    @Subscribe
    public void onPlayerJoin(JoinEvent event) {
        DCPlayer dcPlayer = event.player();
        Player player = BukkitUtils.toBukkit(dcPlayer);
        boolean offLogic = addon.getConfigManager().isTurnOffDailyCaseLogic();
        String newPlayerChoice = addon.getConfigManager().getNewPlayerChoice();

        if (offLogic && "timer".equalsIgnoreCase(newPlayerChoice)) {
            return;
        }

        if (offLogic && "case".equalsIgnoreCase(newPlayerChoice)) {
            if (service.getNextClaimTimes().putIfAbsent(player.getName(), -1L) == null) {
                service.giveGift(player.getName());
                addon.getDatabaseManager().asyncSaveNextClaimTimes(service.getNextClaimTimes(), () -> {
                    if (addon.getConfigManager().isDebug())
                        addon.getDCAPI().getPlatform().getLogger().info("Next claim times updated in database.");
                });
            }
            return;
        }

        if (!offLogic && "case".equalsIgnoreCase(newPlayerChoice)) {
            long nextTime = System.currentTimeMillis() + service.getClaimCooldown();
            if (service.getNextClaimTimes().putIfAbsent(player.getName(), nextTime) == null) {
                service.giveGift(player.getName());
                addon.getDatabaseManager().asyncSaveNextClaimTimes(service.getNextClaimTimes(), () -> {
                    if (addon.getConfigManager().isDebug())
                        addon.getDCAPI().getPlatform().getLogger().info("Next claim times updated in database.");
                });
            }
            return;
        }

        if (!offLogic && "timer".equalsIgnoreCase(newPlayerChoice)) {
            long nextTime = System.currentTimeMillis() + service.getClaimCooldown();
            if (service.getNextClaimTimes().putIfAbsent(player.getName(), nextTime) == null) {
                addon.getDatabaseManager().asyncSaveNextClaimTimes(service.getNextClaimTimes(), () -> {
                    if (addon.getConfigManager().isDebug())
                        addon.getDCAPI().getPlatform().getLogger().info("Next claim times updated in database.");
                });
            }
        }
    }
}
