package com.jaguar.versioning;

import java.util.Locale;

/** Separate application identity: monotonically ordered by the dedicated workflow run number. */
public final class DeveloperPreviewVersion {
    public static final int VERSION_CODE_BASE = 1_000_000;
    public static final int MAX_RUN_NUMBER = 2_099_000_000;
    private final String name;
    private final int code;

    private DeveloperPreviewVersion(String name, int code) {
        this.name = name;
        this.code = code;
    }

    public static DeveloperPreviewVersion fromBuild(String runNumber, String fullSha) {
        if (runNumber == null || !runNumber.matches("[1-9][0-9]{0,9}")) {
            throw new IllegalArgumentException("Developer preview requires a positive canonical run number");
        }
        long run = Long.parseLong(runNumber);
        if (run > MAX_RUN_NUMBER) {
            throw new IllegalArgumentException("Developer preview run number exceeds Android version code limit");
        }
        if (fullSha == null || !fullSha.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("Developer preview requires a full 40-character commit SHA");
        }
        return new DeveloperPreviewVersion("preview." + runNumber + "+" +
                fullSha.substring(0, 7).toLowerCase(Locale.ROOT), Math.toIntExact(VERSION_CODE_BASE + run));
    }

    public String getName() { return name; }
    public int getCode() { return code; }
}
