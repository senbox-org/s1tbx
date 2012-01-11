/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.core.runtime.internal;

import junit.framework.TestCase;
import com.bc.ceres.core.runtime.Version;


public class VersionTest extends TestCase {

    public void testParsing() {
        try {
            Version.parseVersion(null);
            fail();
        } catch (NullPointerException e) {
        }

        testVersion(Version.parseVersion(""), 1, 0, 0, "");
        testVersion(Version.parseVersion("  "), 1, 0, 0, "");
        testVersion(Version.parseVersion("A"), 1, 0, 0, "A");
        testVersion(Version.parseVersion(" A"), 1, 0, 0, "A");

        testVersion(Version.parseVersion("1"), 1, 0, 0, "");
        testVersion(Version.parseVersion("1.0"), 1, 0, 0, "");
        testVersion(Version.parseVersion("1.0.0"), 1, 0, 0, "");
        testVersion(Version.parseVersion("1.0.0.23"), new int[]{1, 0, 0, 23}, "");

        testVersion(Version.parseVersion("2"), 2, 0, 0, "");
        testVersion(Version.parseVersion("2.5"), 2, 5, 0, "");
        testVersion(Version.parseVersion("2.5.1"), 2, 5, 1, "");
        testVersion(Version.parseVersion("2.5.1.23"), new int[]{2, 5, 1, 23}, "");

        testVersion(Version.parseVersion("4-M1"), 4, 0, 0, "M1");
        testVersion(Version.parseVersion("4.3-M1"), 4, 3, 0, "M1");
        testVersion(Version.parseVersion("4.3.0-M1"), 4, 3, 0, "M1");
        testVersion(Version.parseVersion("4.3.0-M1-SNAPSHOT"), 4, 3, 0, "M1-SNAPSHOT");

        testVersion(Version.parseVersion("543"), 543, 0, 0, "");
        testVersion(Version.parseVersion("543.765"), 543, 765, 0, "");
        testVersion(Version.parseVersion("543.765.93452"), 543, 765, 93452, "");
        testVersion(Version.parseVersion("543.765.93452.6743.865.654"), new int[]{543, 765, 93452, 6743, 865, 654}, "");

        testVersion(Version.parseVersion("13A"), 1, 0, 0, "13A");
        testVersion(Version.parseVersion("13.4A"), 13, 0, 0, "4A");
        testVersion(Version.parseVersion("13.4.9656A"), 13, 4, 0, "9656A");
        testVersion(Version.parseVersion("13.4.9656.A"), 13, 4, 9656, "A");
        testVersion(Version.parseVersion("13.4.9656.12A"), 13, 4, 9656, "12A");

        testVersion(Version.parseVersion("13A"), 1, 0, 0, "13A");
        testVersion(Version.parseVersion("13-4A"), 13, 0, 0, "4A");
        testVersion(Version.parseVersion("13-4-9656A"), 13, 4, 0, "9656A");
        testVersion(Version.parseVersion("13-4-9656-A"), 13, 4, 9656, "A");
        testVersion(Version.parseVersion("13-4-9656-12A"), 13, 4, 9656, "12A");
    }

    public void testToString() {
        assertEquals("1.6.3-SNAPSHOT", new Version(1, 6, 3, "SNAPSHOT").toString());
        assertEquals("1.6.3-SNAPSHOT", Version.parseVersion("1.6.3-SNAPSHOT").toString());
        assertEquals("1-6-3_SNAPSHOT", Version.parseVersion("1-6-3_SNAPSHOT").toString());

        assertEquals("1.0.0-RC4", new Version(1, 0, 0, "RC4").toString());
        assertEquals("10.3.2-RC4", new Version(10, 3, 2, "RC4").toString());
    }

    public void testPartSeparators() {
        assertEquals(Version.parseVersion("1.6"), Version.parseVersion("1.6"));
        assertEquals(Version.parseVersion("1.6"), Version.parseVersion("1-6"));
        assertEquals(Version.parseVersion("1.6"), Version.parseVersion("1_6"));
        assertEquals(Version.parseVersion("1-6"), Version.parseVersion("1_6"));
    }

    public void testCompareMajor() {
        Version v1;
        Version v2;

        v1 = new Version(4, 2, 6, "");
        v2 = new Version(5, 2, 6, "");
        assertTrue(v1.compareTo(v2) < 0);

        v1 = new Version(4, 2, 6, "");
        v2 = new Version(4, 2, 6, "");
        assertTrue(v1.compareTo(v2) == 0);

        v1 = new Version(4, 2, 6, "");
        v2 = new Version(3, 2, 6, "");
        assertTrue(v1.compareTo(v2) > 0);
    }

    public void testCompareMinor() {
        Version v1;
        Version v2;

        v1 = new Version(4, 4, 12, "");
        v2 = new Version(4, 9, 12, "");
        assertTrue(v1.compareTo(v2) < 0);

        v1 = new Version(4, 4, 12, "");
        v2 = new Version(4, 4, 12, "");
        assertTrue(v1.compareTo(v2) == 0);

        v1 = new Version(4, 4, 12, "");
        v2 = new Version(4, 1, 12, "");
        assertTrue(v1.compareTo(v2) > 0);
    }

    public void testCompareMicro() {
        Version v1;
        Version v2;

        v1 = new Version(4, 2, 4, "");
        v2 = new Version(4, 2, 5, "");
        assertTrue(v1.compareTo(v2) < 0);

        v1 = new Version(4, 2, 4, "");
        v2 = new Version(4, 2, 4, "");
        assertTrue(v1.compareTo(v2) == 0);

        v1 = new Version(4, 2, 4, "");
        v2 = new Version(4, 2, 1, "");
        assertTrue(v1.compareTo(v2) > 0);
    }

    public void testCompareSnapshot() {
        Version v1;
        Version v2;

        v1 = Version.parseVersion("2.2");
        v2 = Version.parseVersion("2.2-SNAPSHOT");
        assertTrue(v1.compareTo(v2) > 0);

        v1 = Version.parseVersion("2.2-SNAPSHOT");
        v2 = Version.parseVersion("2.1-SNAPSHOT");
        assertTrue(v1.compareTo(v2) > 0);

        v1 = Version.parseVersion("2.2.4-SNAPSHOT");
        v2 = Version.parseVersion("2.2.1-SNAPSHOT");
        assertTrue(v1.compareTo(v2) > 0);

        v1 = Version.parseVersion("2.2.1");
        v2 = Version.parseVersion("2.2-SNAPSHOT");
        assertTrue(v1.compareTo(v2) > 0);

        v1 = Version.parseVersion("2.2.2-SNAPSHOT");
        v2 = Version.parseVersion("2.2");
        assertTrue(v1.compareTo(v2) > 0);

        v1 = Version.parseVersion("2.2-SNAPSHOT");
        v2 = Version.parseVersion("2.2-SNAPSHOT");
        assertTrue(v1.compareTo(v2) == 0);
    }

    public void testCompareQualifier() {
        Version v1;
        Version v2;


        v1 = new Version(4, 2, 1, "RC2");
        v2 = new Version(4, 2, 1, "RC3");
        assertTrue(v1.compareTo(v2) < 0);

        v1 = new Version(4, 2, 1, "RC2");
        v2 = new Version(4, 2, 1, "RC2");
        assertTrue(v1.compareTo(v2) == 0);

        v1 = new Version(4, 2, 1, "RC2");
        v2 = new Version(4, 2, 1, "RC1");
        assertTrue(v1.compareTo(v2) > 0);

        v1 = new Version(4, 2, 1, "RC2");
        v2 = new Version(4, 2, 1, "RC13");
        assertTrue(v1.compareTo(v2) > 0);

        v1 = new Version(4, 2, 1, "RC1");
        v2 = new Version(4, 2, 1, "RC13");
        assertTrue(v1.compareTo(v2) < 0);

        v1 = new Version(4, 2, 1, "");
        v2 = new Version(4, 2, 1, "RC13");
        assertTrue(v1.compareTo(v2) > 0);

        v1 = new Version(4, 2, 1, "");
        v2 = new Version(4, 2, 1, "9785");
        assertTrue(v1.compareTo(v2) < 0);

        v1 = new Version(1, 0, 0, "SNAPSHOT-20120110135600");
        v2 = new Version(1, 0, 0, "SNAPSHOT-20120110135601");
        assertTrue(v1.compareTo(v2) < 0);

        v1 = new Version(4, 2, 1, "");
        v2 = new Version(4, 2, 1, "0");
        assertTrue(v1.compareTo(v2) == 0);

        v1 = new Version(4, 2, 1, "0000");
        v2 = new Version(4, 2, 1, "");
        assertTrue(v1.compareTo(v2) == 0);

        v1 = new Version(4, 2, 1, "00.00");
        v2 = new Version(4, 2, 1, "");
        assertTrue(v1.compareTo(v2) == 0);

        v1 = new Version(4, 2, 1, "54.324");
        v2 = new Version(4, 2, 1, "54");
        assertTrue(v1.compareTo(v2) > 0);
    }

    private static void testVersion(Version actualVersion,
                                    int expectedMajor,
                                    int expectedMinor,
                                    int expectedMicro,
                                    String expectedQualifier) {
        assertEquals(expectedMajor, actualVersion.getMajor());
        assertEquals(expectedMinor, actualVersion.getMinor());
        assertEquals(expectedMicro, actualVersion.getMicro());
        assertEquals(expectedQualifier, actualVersion.getQualifier());
    }

    private static void testVersion(Version actualVersion,
                                    int[] expectedNumbers,
                                    String expectedQualifier) {
        assertEquals(expectedNumbers.length, actualVersion.getNumberCount());
        for (int i = 0; i < expectedNumbers.length; i++) {
            assertEquals("expectedNumbers[" + i + "]", expectedNumbers[i], actualVersion.getNumber(i));
        }
        assertEquals(expectedQualifier, actualVersion.getQualifier());
    }
}
