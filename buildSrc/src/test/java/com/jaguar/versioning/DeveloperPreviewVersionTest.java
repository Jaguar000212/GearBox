package com.jaguar.versioning;

import org.junit.Test;
import static org.junit.Assert.*;

public class DeveloperPreviewVersionTest {
    private static final String SHA = "ABCDEF0123456789abcdef0123456789abcdef0123";
    @Test public void previewIdentityAndMonotonicOrdering() {
        DeveloperPreviewVersion first = DeveloperPreviewVersion.fromBuild("1", SHA);
        DeveloperPreviewVersion next = DeveloperPreviewVersion.fromBuild("2", SHA);
        assertEquals(1000001, first.getCode());
        assertEquals("preview.1+abcdef0", first.getName());
        assertEquals(first.getCode() + 1, next.getCode());
        assertEquals(first.getCode(), DeveloperPreviewVersion.fromBuild("1", "0".repeat(40)).getCode());
        assertEquals(2100000000, DeveloperPreviewVersion.fromBuild("2099000000", SHA).getCode());
    }
    @Test public void malformedMissingAndOverflowingRunNumbersFailClosed() {
        for (String run : new String[] {null, "", "0", "01", "-1", "+1", " 1", "1\n", "1.0", "١", "2099000001", "9999999999", "99999999999999999999"}) {
            assertThrows(IllegalArgumentException.class, () -> DeveloperPreviewVersion.fromBuild(run, SHA));
        }
    }
    @Test public void fullSourceShaIsRequired() {
        for (String sha : new String[] {null, "", "abcdef0", "a".repeat(39), "a".repeat(41), "g".repeat(40), "a".repeat(39) + "\n"}) {
            assertThrows(IllegalArgumentException.class, () -> DeveloperPreviewVersion.fromBuild("1", sha));
        }
    }
}
