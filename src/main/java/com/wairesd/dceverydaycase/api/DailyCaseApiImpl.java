package com.wairesd.dceverydaycase.api;

import com.wairesd.dceverydaycase.service.DailyCaseService;
import java.util.Map;

public class DailyCaseApiImpl extends DailyCaseApi {
    private final DailyCaseService service;

    public DailyCaseApiImpl(DailyCaseService service) {
        this.service = service;
    }

    @Override
    public long getNextClaimTime(String playerName) {
        return service.getNextClaimTimes().getOrDefault(playerName, 0L);
    }

    @Override
    public long getClaimCooldown() {
        return service.getClaimCooldown();
    }

    @Override
    public String getCaseName() {
        return service.getCaseName();
    }

    @Override
    public int getKeysAmount() {
        return service.getKeysAmount();
    }

    @Override
    public boolean isPending(String playerName) {
        return service.pendingKeys.contains(playerName);
    }

    @Override
    public void resetTimer(String playerName) {
        service.resetTimer(playerName);
    }

    @Override
    public Map<String, Long> getAllNextClaimTimes() {
        return service.getNextClaimTimes();
    }
}