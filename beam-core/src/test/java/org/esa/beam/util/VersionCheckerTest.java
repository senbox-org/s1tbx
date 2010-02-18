/*
 * $Id: VersionCheckerTest.java,v 1.4 2007/04/12 12:52:31 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
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
            assertTrue(localVersion.startsWith("VERSION 4.7"));
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
    }
}
