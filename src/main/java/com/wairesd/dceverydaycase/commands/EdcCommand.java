package com.wairesd.dceverydaycase.commands;

import com.jodexindustries.donatecase.api.data.subcommand.*;
import com.jodexindustries.donatecase.api.platform.*;
import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;
import com.wairesd.dceverydaycase.tools.Color;
import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.List;

/** Executes the "edc" command to toggle notification status. */
public class EdcCommand implements SubCommandExecutor, SubCommandTabCompleter {
    private final DCEveryDayCaseAddon addon;

    public EdcCommand(DCEveryDayCaseAddon addon) { this.addon = addon; }

    @Override
    public boolean execute(@NotNull DCCommandSender sender, @NotNull String label, String[] args) {
        // Ensure the sender is a player and has permission
        if (!(sender instanceof DCPlayer)) {
            sender.sendMessage(addon.getConfig().node().messages.onlyForPlayersMessage);
            return true;
        }
        if (!sender.hasPermission("dc.everydaycase.granted")) {
            sender.sendMessage(Color.translate(addon.getConfig().node().messages.noPermissionMessage));
            return true;
        }
        // Toggle notification status and inform the player
        boolean newStatus = !addon.getDatabaseManager().getNotificationStatus(sender.getName());
        addon.getDatabaseManager().setNotificationStatus(sender.getName(), newStatus);
        String msg = newStatus ? addon.getConfig().node().messages.caseGrantedOn : addon.getConfig().node().messages.caseGrantedOff;
        sender.sendMessage(Color.translate(msg));
        return true;
    }

    @Override
    public List<String> getTabCompletions(@NotNull DCCommandSender sender, @NotNull String label, String[] args) {
        return args.length == 1 ? Collections.singletonList("granted") : Collections.emptyList();
    }
}
