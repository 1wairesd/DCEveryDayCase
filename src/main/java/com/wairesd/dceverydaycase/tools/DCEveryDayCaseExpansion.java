package com.wairesd.dceverydaycase.tools;

import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import com.jodexindustries.donatecase.api.DCAPI;
import org.jetbrains.annotations.NotNull;

/**
 * PLALISHLEDER to display information about the status of a daily case.
 */
public class DCEveryDayCaseExpansion extends PlaceholderExpansion {
    private final DCEveryDayCaseAddon plugin;

    public DCEveryDayCaseExpansion(DCEveryDayCaseAddon plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dceverydaycase";
    }

    @Override
    public @NotNull String getAuthor() {
        return "1wairesd";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    /**
     * Returns the text of the PLYSHOLDER %dceverydaycase_remaining_time%
     */
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (!"remaining_time".equalsIgnoreCase(params)) return "";
        if (player == null) return plugin.getConfig().getInfoPlaceholder();

        DailyCaseService service = plugin.getDailyCaseService();
        DCAPI dcapi = plugin.getDCAPI();
        if (service == null || dcapi == null) return "";

        long currentTime = System.currentTimeMillis();
        int keys = dcapi.getCaseKeyManager().getCache(service.getCaseName(), player.getName());
        if (keys > 0) return plugin.getConfig().getPlaceholderAvailable();

        long nextClaim = service.getNextClaimTimes().computeIfAbsent(player.getName(), n -> currentTime + service.getClaimCooldown());
        if (currentTime >= nextClaim) {
            return plugin.getConfig().getPlaceholderAvailable();
        } else {
            return formatTime(plugin.getConfig().getPlaceholderRemaining(), nextClaim - currentTime);
        }
    }

    /** formates the remaining time according to the template*/
    private String formatTime(String template, long millis) {
        long seconds = millis / 1000;
        long minutes = (seconds / 60) % 60;
        long hours = (seconds / 3600) % 24;
        long days = seconds / 86400;
        return template.replace("$d", String.valueOf(days))
                .replace("$h", String.valueOf(hours))
                .replace("$m", String.valueOf(minutes))
                .replace("$s", String.valueOf(seconds % 60));
    }
}
