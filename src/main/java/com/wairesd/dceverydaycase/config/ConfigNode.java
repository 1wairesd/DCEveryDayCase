package com.wairesd.dceverydaycase.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.List;

// Configuration structure
@ConfigSerializable
public class ConfigNode {

    @Setting("config_version")
    public int configVersion = 1;

    @Setting("claim_cooldown")
    public long claimCooldown = 86400;

    @Setting("case_name")
    public String caseName = "DCEveryDayCase";

    @Setting("keys_amount")
    public int keysAmount = 1;

    @Setting("new_player_choice")
    public String newPlayerChoice = "case";

    @Setting("turn_off_daily_case_logic")
    public boolean OffLogicDailyCase = true;

    @Setting
    public boolean debug = false;

    @Setting
    public Placeholder placeholder = new Placeholder();

    @Setting
    public Messages messages = new Messages();

    public boolean containsKey(String key) {
        return false;
    }

    public void set(String key, Object defaultValue) {
    }

    @ConfigSerializable
    public static class Placeholder {
        @Setting
        public String available = "&7Daily case is available: &aclick to receive";

        @Setting
        public String remaining = "&7Time remaining: &6$d days, &6$h hours, &6$m minutes, &6$s seconds";
    }


    @ConfigSerializable
    public static class Messages {
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

        public boolean containsKey(String key) {
            return false;
        }
    }
}
