package com.wairesd.dceverydaycase.tools;

import com.jodexindustries.donatecase.api.DCAPI;
import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DCEveryDayCaseExpansion extends PlaceholderExpansion {
    private final DCEveryDayCaseAddon plugin;

    public DCEveryDayCaseExpansion(DCEveryDayCaseAddon plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "dceverydaycase"; }

    @Override
    public @NotNull String getAuthor() { return "1wairesd"; }

    @Override
    public @NotNull String getVersion() { return "1.0.0"; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (plugin.getConfig().isTurnOffDailyCaseLogic() &&
                plugin.getConfig().getNewPlayerChoice().equalsIgnoreCase("timer")) {
            return "";
        }

        if (!"remaining_time".equalsIgnoreCase(params)) {
            return "";
        }

        if (player == null || plugin.getDailyCaseService() == null || plugin.getDCAPI() == null) {
            return plugin.getConfig().getInfoPlaceholder();
        }

        DailyCaseService service = plugin.getDailyCaseService();
        DCAPI dcapi = plugin.getDCAPI();
        long now = System.currentTimeMillis();
        int keys = dcapi.getCaseKeyManager().getCache(service.getCaseName(), player.getName());

        if (keys > 0) {
            return plugin.getConfig().getAvailable();
        }

        long nextClaim = service.getNextClaimTimes().computeIfAbsent(player.getName(),
                n -> now + service.getClaimCooldown());
        return now >= nextClaim ? plugin.getConfig().getAvailable()
                : formatTime(plugin.getConfig().getRemaining(), nextClaim - now);
    }

    private String formatTime(String template, long millis) {
        long seconds = millis / 1000, minutes = (seconds / 60) % 60, hours = (seconds / 3600) % 24, days = seconds / 86400;
        return template.replace("$d", String.valueOf(days))
                .replace("$h", String.valueOf(hours))
                .replace("$m", String.valueOf(minutes))
                .replace("$s", String.valueOf(seconds % 60));
    }
}