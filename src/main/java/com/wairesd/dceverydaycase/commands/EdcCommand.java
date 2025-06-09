package com.wairesd.dceverydaycase.commands;

import com.jodexindustries.donatecase.api.data.subcommand.SubCommandExecutor;
import com.jodexindustries.donatecase.api.data.subcommand.SubCommandTabCompleter;
import com.jodexindustries.donatecase.api.platform.DCCommandSender;
import com.jodexindustries.donatecase.api.platform.DCPlayer;
import com.jodexindustries.donatecase.api.tools.DCTools;
import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class EdcCommand implements SubCommandExecutor, SubCommandTabCompleter {
    private final DCEveryDayCaseAddon addon;

    public EdcCommand(DCEveryDayCaseAddon addon) { this.addon = addon; }

    @Override
    public boolean execute(@NotNull DCCommandSender sender, @NotNull String label, String[] args) {
        if (!(sender instanceof DCPlayer)) {
            sender.sendMessage(addon.getConfig().getOnlyForPlayersMessage());
            return true;
        }
        if (!sender.hasPermission("dc.everydaycase.granted")) {
            sender.sendMessage(DCTools.rc(addon.getConfig().getNoPermissionMessage()));
            return true;
        }

        boolean newStatus = !addon.getDatabaseManager().getNotificationStatus(sender.getName());
        addon.getDatabaseManager().setNotificationStatus(sender.getName(), newStatus);
        String msg = newStatus ? addon.getConfig().getCaseGrantedOn() : addon.getConfig().getCaseGrantedOff();
        sender.sendMessage(DCTools.rc(msg));
        return true;
    }

    @Override
    public List<String> getTabCompletions(@NotNull DCCommandSender sender, @NotNull String label, String[] args) {
        return args.length == 1 ? Collections.singletonList("granted") : Collections.emptyList();
    }
}