package com.jaguar.versioning;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Android release identity. Beta sorts immediately before stable for the same version. */
public final class ReleaseVersion {
    private static final Pattern TAG = Pattern.compile(
            "v(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(-beta)?");
    // Bounding every component keeps arithmetic and future stable promotion safe.
    private static final int MAX_MAJOR = 20999;
    private final String name;
    private final int code;
    private final boolean prerelease;

    private ReleaseVersion(String name, int code, boolean prerelease) {
        this.name = name;
        this.code = code;
        this.prerelease = prerelease;
    }

    public static ReleaseVersion fromTag(String tag) {
        if (tag == null) throw new IllegalArgumentException("Release tag must not be null");
        Matcher match = TAG.matcher(tag);
        if (!match.matches()) {
            throw new IllegalArgumentException("Expected vMAJOR.MINOR.PATCH or vMAJOR.MINOR.PATCH-beta: " + tag);
        }
        int major = component(match.group(1), MAX_MAJOR, "major");
        int minor = component(match.group(2), 99, "minor");
        int patch = component(match.group(3), 99, "patch");
        long base = major * 10000L + minor * 100L + patch;
        if (base == 0) throw new IllegalArgumentException("Release version must be greater than 0.0.0");
        boolean beta = match.group(4) != null;
        int code = Math.toIntExact(base * 10L + (beta ? 0 : 1));
        return new ReleaseVersion(tag.substring(1), code, beta);
    }

    private static int component(String text, int maximum, String label) {
        // Reject enormous input before parsing, rather than overflowing or truncating it.
        if (text.length() > Integer.toString(maximum).length()) {
            throw new IllegalArgumentException("Release " + label + " must be 0.." + maximum);
        }
        int value = Integer.parseInt(text);
        if (value > maximum) throw new IllegalArgumentException("Release " + label + " must be 0.." + maximum);
        return value;
    }

    public String getName() { return name; }
    public int getCode() { return code; }
    public boolean isPrerelease() { return prerelease; }
}
