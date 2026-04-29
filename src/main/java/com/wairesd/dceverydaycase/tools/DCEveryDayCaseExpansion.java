package com.wairesd.dceverydaycase.tools;

import com.jodexindustries.donatecase.api.DCAPI;
import com.wairesd.dceverydaycase.bootstrap.Main;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DCEveryDayCaseExpansion extends PlaceholderExpansion {
    private final Main plugin;

    public DCEveryDayCaseExpansion(Main plugin) {
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

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (plugin.getConfigManager().isTurnOffDailyCaseLogic() &&
                plugin.getConfigManager().getNewPlayerChoice().equalsIgnoreCase("timer")) {
            return "";
        }

        if (!"remaining_time".equalsIgnoreCase(params)) {
            return null; // Возвращаем null для неизвестных плейсхолдеров (стандарт PAPI)
        }

        if (player == null || plugin.getDailyCaseService() == null || plugin.getDCAPI() == null) {
            String info = plugin.getConfigManager().getInfoPlaceholder();
            return info != null ? info : "";
        }

        DailyCaseService service = plugin.getDailyCaseService();
        DCAPI dcapi = plugin.getDCAPI();
        long now = System.currentTimeMillis();
        int keys = dcapi.getCaseKeyManager().getCache(service.getCaseName(), player.getName());

        if (keys > 0) {
            String available = plugin.getConfigManager().getAvailable();
            return available != null ? available : "";
        }

        long nextClaim = service.getNextClaimTimes().getOrDefault(player.getName(),
                now + service.getClaimCooldown());
        
        if (now >= nextClaim) {
            String available = plugin.getConfigManager().getAvailable();
            return available != null ? available : "";
        } else {
            String remaining = plugin.getConfigManager().getRemaining();
            return remaining != null ? formatTime(remaining, nextClaim - now) : "";
        }
    }

    private String formatTime(String template, long millis) {
        long seconds = millis / 1000, minutes = (seconds / 60) % 60, hours = (seconds / 3600) % 24, days = seconds / 86400;
        return template.replace("$d", String.valueOf(days))
                .replace("$h", String.valueOf(hours))
                .replace("$m", String.valueOf(minutes))
                .replace("$s", String.valueOf(seconds % 60));
    }
}