package com.wairesd.dceverydaycase.tools;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import com.jodexindustries.donatecase.api.DCAPI;

/**
 * Плейсхолдер для отображения информации о статусе ежедневного кейса.
 */
public class DCEveryDayCaseExpansion extends PlaceholderExpansion {
    private final DailyCasePlugin plugin;
    private final String placeholderAvailable;
    private final String placeholderRemaining;

    public DCEveryDayCaseExpansion(DailyCasePlugin plugin, JavaPlugin javaPlugin) {
        this.plugin = plugin;
        placeholderAvailable = javaPlugin.getConfig().getString("placeholder.available", "Доступен ежедневный кейс: нажмите для получения");
        placeholderRemaining = javaPlugin.getConfig().getString("placeholder.remaining", "До получения осталось: $d дн, $h ч, $m мин, $s сек");
    }

    @Override
    public String getIdentifier() {
        return "dceverydaycase";
    }

    @Override
    public String getAuthor() {
        return "1wairesd";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    /**
     * Возвращает текст плейсхолдера %dceverydaycase_remaining_time%
     */
    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (!"remaining_time".equalsIgnoreCase(params)) return "";
        if (player == null) return "Информация доступна только для игроков";

        DailyCaseService service = plugin.getDailyCaseService();
        DCAPI dcapi = plugin.getDCAPI();
        if (service == null || dcapi == null) return "";

        long currentTime = System.currentTimeMillis();
        int keys = dcapi.getCaseKeyManager().getCache(service.getCaseName(), player.getName());
        if (keys > 0) return placeholderAvailable;

        long nextClaim = service.getNextClaimTimes().computeIfAbsent(player.getName(), n -> currentTime + service.getClaimCooldown());
        if (currentTime >= nextClaim) {
            return placeholderAvailable;
        } else {
            return formatTime(placeholderRemaining, nextClaim - currentTime);
        }
    }

    /** Форматирует оставшееся время согласно шаблону */
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
