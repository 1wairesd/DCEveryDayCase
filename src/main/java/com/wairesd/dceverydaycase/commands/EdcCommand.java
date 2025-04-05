package com.wairesd.dceverydaycase.commands;

import com.jodexindustries.donatecase.api.data.subcommand.*;
import com.jodexindustries.donatecase.api.platform.*;
import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;
import com.wairesd.dceverydaycase.tools.Color;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.List;

/** Executes the "edc" command to toggle notification status. */
public class EdcCommand implements SubCommandExecutor, SubCommandTabCompleter {
    private final DCEveryDayCaseAddon addon;

    public EdcCommand(DCEveryDayCaseAddon addon) { this.addon = addon; }

    @Override
    public boolean execute(DCCommandSender sender, @NotNull String label, String[] args) {
        // Ensure the sender is a player and has permission
        if (!(sender instanceof DCPlayer dp)) {
            sender.sendMessage(addon.getConfig().getOnlyForPlayersMessage());
            return true;
        }
        if (!sender.hasPermission("dc.everydaycase.granted")) {
            sender.sendMessage(Color.translate(addon.getConfig().getNoPermissionMessage()));
            return true;
        }
        // Toggle notification status and inform the player
        Player player = (Player) dp.getHandler();
        boolean newStatus = !addon.getDatabaseManager().getNotificationStatus(player.getName());
        addon.getDatabaseManager().setNotificationStatus(player.getName(), newStatus);
        String msg = newStatus ? addon.getConfig().getCaseGrantedOn() : addon.getConfig().getCaseGrantedOff();
        player.sendMessage(Color.translate(msg));
        return true;
    }

    @Override
    public List<String> getTabCompletions(@NotNull DCCommandSender sender, @NotNull String label, String[] args) {
        return args.length == 1 ? Collections.singletonList("granted") : Collections.emptyList();
    }
}
