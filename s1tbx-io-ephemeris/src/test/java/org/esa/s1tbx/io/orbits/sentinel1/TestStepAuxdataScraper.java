/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.orbits.sentinel1;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import static junit.framework.TestCase.*;

/**
 * find files on step
 */
@Ignore
public class TestStepAuxdataScraper {

    @BeforeClass
    public static void setUpClass() throws Exception {
        boolean internetAvailable;
        try {
            URLConnection urlConnection = new URL("http://www.google.com").openConnection();
            urlConnection.setConnectTimeout(3000);
            urlConnection.getContent();
            internetAvailable = true;
        } catch (IOException e) {
            internetAvailable = false;
        }

        Assume.assumeTrue("Internet connection not available, skipping TestStepAuxdataScraper", internetAvailable);
    }


    @Test
    public void testDownloadPreciseOrbitFileS1A() {
        final StepAuxdataScraper step = new StepAuxdataScraper(SentinelPODOrbitFile.PRECISE);

        String[] orbitFiles = step.getFileURLs("S1A", 2016, 2);
        assertEquals(29, orbitFiles.length);
    }

    @Test
    public void testDownloadPreciseOrbitFileS1B() {
        final StepAuxdataScraper step = new StepAuxdataScraper(SentinelPODOrbitFile.PRECISE);

        String[] orbitFiles = step.getFileURLs("S1B", 2016, 7);
        assertEquals(31, orbitFiles.length);
    }

    @Test
    public void testDownloadRestituteOrbitFileS1A() {
        final StepAuxdataScraper step = new StepAuxdataScraper(SentinelPODOrbitFile.RESTITUTED);

        String[] orbitFiles = step.getFileURLs("S1A", 2016, 2);
        assertEquals(591, orbitFiles.length);
    }

    @Test
    public void testDownloadRestituteOrbitFileS1B() {
        final StepAuxdataScraper step = new StepAuxdataScraper(SentinelPODOrbitFile.RESTITUTED);

        String[] orbitFiles = step.getFileURLs("S1B", 2016, 7);
        assertEquals(592, orbitFiles.length);
    }
}
