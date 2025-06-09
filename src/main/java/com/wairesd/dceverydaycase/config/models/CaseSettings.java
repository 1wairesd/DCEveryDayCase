package com.wairesd.dceverydaycase.config.models;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class CaseSettings {
    @Setting("case_name")
    public String caseName = "DCEveryDayCase";

    @Setting("keys_amount")
    public int keysAmount = 1;

    @Setting("claim_cooldown")
    public long claimCooldown = 86400;

    @Setting("new_player_choice")
    public String newPlayerChoice = "case";

    @Setting("turn_off_daily_case_logic")
    public boolean turnOffDailyCaseLogic = false;
}