package com.wairesd.dceverydaycase.api;

import java.util.Map;

public abstract class DailyCaseApi {
    private static class Holder {
        private static DailyCaseApi instance;
    }

    public static DailyCaseApi getInstance() {
        if (Holder.instance == null) {
            throw new IllegalStateException("DailyCaseApi is not initialized!");
        }
        return Holder.instance;
    }

    public static void setInstance(DailyCaseApi api) {
        Holder.instance = api;
    }

    public abstract long getNextClaimTime(String playerName);
    public abstract long getClaimCooldown();
    public abstract String getCaseName();
    public abstract int getKeysAmount();
    public abstract boolean isPending(String playerName);
    public abstract void resetTimer(String playerName);
    public abstract Map<String, Long> getAllNextClaimTimes();
}