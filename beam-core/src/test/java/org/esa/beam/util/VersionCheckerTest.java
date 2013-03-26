/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.util;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

public class VersionCheckerTest extends TestCase {


    public void testLocalVersion() throws IOException {
        VersionChecker vc = new VersionChecker();
        assertNotNull(vc.getRemoteVersionUrlString());
        assertNotNull(vc.getLocalVersionFile());
        if (vc.getLocalVersionFile().exists()) {
            final String localVersion = vc.getLocalVersion();
            assertNotNull(localVersion);
            assertTrue(localVersion.startsWith("VERSION 4.11"));
            // Failed? --> Adapt current version number here.
        }
    }

    public void testRemoteVersion() throws IOException {
        VersionChecker vc = new VersionChecker(new File("./VERSION.txt"),
                                               getClass().getResource("version.txt").toExternalForm());
        assertNotNull(vc.getRemoteVersionUrlString());
        assertNotNull(vc.getLocalVersionFile());
        assertEquals("VERSION 4.9.12", vc.getRemoteVersion());
    }

    public void testCompare() {
        assertTrue(VersionChecker.compareVersions("VERSION 4.7", "VERSION 4.7") == 0);
        assertTrue(VersionChecker.compareVersions("VERSION 4.7", "VERSION 3.7") > 0);
        assertTrue(VersionChecker.compareVersions("VERSION 4.7", "VERSION 4.7-SNAPSHOT") > 0);
        assertTrue(VersionChecker.compareVersions("VERSION 4.2.0", "VERSION 4.2") == 0);
        assertTrue(VersionChecker.compareVersions("VERSION 4.2", "VERSION 4.2.0") == 0);
        assertTrue(VersionChecker.compareVersions("VERSION 4.6.999", "VERSION 4.7") < 0);
        assertTrue(VersionChecker.compareVersions("VERSION 4.7", "VERSION 4.7-RC1") > 0);
        assertTrue(VersionChecker.compareVersions("VERSION 4.7-RC1", "VERSION 4.7") < 0);
        assertTrue(VersionChecker.compareVersions("VERSION 4.7-RC1", "VERSION 4.7-RC2") < 0);
        assertTrue(VersionChecker.compareVersions("VERSION 4.7.1", "VERSION 4.7-RC2") > 0);
        assertTrue(VersionChecker.compareVersions("VERSION 4.7.2", "VERSION 4.7-RC1") > 0);
        assertTrue(VersionChecker.compareVersions("VERSION 4.9.1", "VERSION 4.9.0") > 0);
        assertTrue(VersionChecker.compareVersions("VERSION 4.9.0.1", "VERSION 4.9.0") > 0);
        assertTrue(VersionChecker.compareVersions("VERSION 4.9.0.1", "VERSION 4.9") > 0);
        assertTrue(VersionChecker.compareVersions("VERSION 4.9.0.1", "VERSION 4.9.1") < 0);
    }
}
