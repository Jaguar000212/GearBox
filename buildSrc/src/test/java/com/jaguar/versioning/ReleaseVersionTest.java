package com.jaguar.versioning;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReleaseVersionTest {
    @Test public void approvedBetaAndStableIdentity() {
        ReleaseVersion beta = ReleaseVersion.fromTag("v2.41.0-beta");
        ReleaseVersion stable = ReleaseVersion.fromTag("v2.41.0");
        assertEquals("2.41.0-beta", beta.getName());
        assertEquals(241000, beta.getCode());
        assertTrue(beta.isPrerelease());
        assertEquals("2.41.0", stable.getName());
        assertEquals(241001, stable.getCode());
        assertFalse(stable.isPrerelease());
        assertTrue(beta.getCode() > 22900); // Previously distributed v2.29.0.
        assertTrue(stable.getCode() > beta.getCode());
    }

    @Test public void boundedComponentsPreventCollisionsAcrossEveryRollover() {
        for (int major : new int[] {0, 1, 2, 20998}) {
            for (int minor = 0; minor < 100; minor++) {
                for (int patch = 0; patch < 100; patch++) {
                    if (major == 0 && minor == 0 && patch == 0) continue;
                    String tag = "v" + major + "." + minor + "." + patch;
                    int beta = ReleaseVersion.fromTag(tag + "-beta").getCode();
                    int stable = ReleaseVersion.fromTag(tag).getCode();
                    assertEquals(beta + 1, stable);
                    String next = patch < 99 ? "v" + major + "." + minor + "." + (patch + 1)
                            : minor < 99 ? "v" + major + "." + (minor + 1) + ".0"
                            : "v" + (major + 1) + ".0.0";
                    assertTrue(stable < ReleaseVersion.fromTag(next + "-beta").getCode());
                }
            }
        }
    }

    @Test public void maximumFitsAndroidAndLeavesRoomForStable() {
        assertEquals(2099999990, ReleaseVersion.fromTag("v20999.99.99-beta").getCode());
        assertEquals(2099999991, ReleaseVersion.fromTag("v20999.99.99").getCode());
        assertTrue(ReleaseVersion.fromTag("v20999.99.99").getCode() <= 2100000000);
    }

    @Test public void malformedAndOverflowingTagsFailClosed() {
        for (String tag : new String[] {null, "", "2.41.0", "v2.41", "v2.41.0-beta.1",
                "v2.41.0-rc", "v02.41.0", "v2.100.0", "v2.41.100", "v0.0.0", "v0.0.0-beta",
                "v21000.0.0-beta", "v2147483647.0.0", "v9999999999999999999999.0.0",
                "v2.41.0\n", "v2.41.0+build", "v2.٤1.0", "v-1.0.0"}) {
            assertThrows("Must reject " + tag, IllegalArgumentException.class, () -> ReleaseVersion.fromTag(tag));
        }
    }
}
