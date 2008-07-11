/*
 * $Id: VersionCheckerTest.java,v 1.4 2007/04/12 12:52:31 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.ui;

import junit.framework.TestCase;
import org.esa.beam.util.VersionChecker;

import java.io.IOException;

public class VersionCheckerTest extends TestCase {

    private static final boolean PERFORM_REMOTE_TEST = true;

    private VersionChecker _versionChecker;

    protected void setUp() throws Exception {
        _versionChecker = new VersionChecker();
    }

    public void testCurrentLocalVersionString() throws IOException {
        // Failed? --> Adapt current version number here.
        assertEquals("VERSION 4.2", _versionChecker.getLocalVersion());
    }

    public void testCurrentRemoteVersionString() throws IOException {
        if (!PERFORM_REMOTE_TEST) {
            fail("testCurrentRemoteVersionString NOT PERFORMED: " +
                    "Enable this test, as soon as an internet connection is available again!");
        }
        assertEquals(true, _versionChecker.getRemoteVersion().startsWith("VERSION "));
    }
}
