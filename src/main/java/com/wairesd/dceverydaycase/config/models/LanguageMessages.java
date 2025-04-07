package com.wairesd.dceverydaycase.config.models;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

/**
 * Holds messages for a specific language.
 */
@ConfigSerializable
public class LanguageMessages {
    @Setting
    public String available = "&7Daily case is available: &aclick to receive";

    @Setting
    public String remaining = "&7Time remaining: &6$d day, &6$h hour, &6$m minute, &6$s sec";

    @Setting("log_console_give_key")
    public String logConsoleGiveKey = "Issued {key} key to player {player} for case {case}";

    @Setting("info_placeholder")
    public String infoPlaceholder = "Information available only for players";

    @Setting("case_granted_on")
    public String caseGrantedOn = "&aNotifications enabled";

    @Setting("case_granted_off")
    public String caseGrantedOff = "&cNotifications disabled";

    @Setting("only_for_players")
    public String onlyForPlayersMessage = "This command is for players only.";

    @Setting("no_permission")
    public String noPermissionMessage = "&cYou don't have permission.";

    @Setting("case_ready")
    public String caseReadyMessage = "&aYour daily case is ready!";
}