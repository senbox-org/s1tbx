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
package org.esa.snap.core.util;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.*;

public class VersionCheckerTest {

    @Test
    public void testVersions() throws IOException {
        VersionChecker vc = new VersionChecker(asInputStream("5.0"), asInputStream("4.9.12"));
        String actual = vc.getLocalVersion().toString();
        assertEquals("5.0", actual);
        assertEquals("4.9.12", vc.getRemoteVersion().toString());
    }

    @Test
    public void mustCheck() throws Exception {
        assertFalse(VersionChecker.mustCheck(VersionChecker.CHECK.NEVER, LocalDateTime.now()));
        assertFalse(VersionChecker.mustCheck(VersionChecker.CHECK.NEVER, LocalDateTime.now().minus(100, ChronoUnit.DAYS)));
        assertTrue(VersionChecker.mustCheck(VersionChecker.CHECK.ON_START, LocalDateTime.now()));
        assertTrue(VersionChecker.mustCheck(VersionChecker.CHECK.ON_START, LocalDateTime.now().minus(100, ChronoUnit.DAYS)));
        assertTrue(VersionChecker.mustCheck(VersionChecker.CHECK.DAILY, LocalDateTime.now().minus(100, ChronoUnit.DAYS)));
        assertFalse(VersionChecker.mustCheck(VersionChecker.CHECK.DAILY, LocalDateTime.now()));
        assertTrue(VersionChecker.mustCheck(VersionChecker.CHECK.WEEKLY, LocalDateTime.now().minus(100, ChronoUnit.DAYS)));
        assertFalse(VersionChecker.mustCheck(VersionChecker.CHECK.WEEKLY, LocalDateTime.now().minus(3, ChronoUnit.DAYS)));
        assertTrue(VersionChecker.mustCheck(VersionChecker.CHECK.MONTHLY, LocalDateTime.now().minus(100, ChronoUnit.DAYS)));
        assertFalse(VersionChecker.mustCheck(VersionChecker.CHECK.MONTHLY, LocalDateTime.now().minus(15, ChronoUnit.DAYS)));
    }

    @Test
    public void testCheckForNewRelease_WithRemoteGreater() throws Exception {

        VersionChecker vc = new VersionChecker(asInputStream("5.0"), asInputStream("6.0"));
        assertTrue(vc.checkForNewRelease());
    }

    @Test
    public void testCheckForNewRelease_WithRemoteLower() throws Exception {
        VersionChecker vc = new VersionChecker(asInputStream("5.0"), asInputStream("4.0"));
        assertFalse(vc.checkForNewRelease());
    }


    private InputStream asInputStream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}
