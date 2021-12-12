/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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

import org.esa.snap.core.datamodel.ProductData;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertTrue;

/**
 * find files on step
 */
public class TestStepOrbitFileScraper {

    @BeforeClass
    public static void setUpClass() {
        boolean internetAvailable;
        try {
            URLConnection urlConnection = new URL("http://www.google.com").openConnection();
            urlConnection.setConnectTimeout(3000);
            urlConnection.getContent();
            internetAvailable = true;
        } catch (IOException e) {
            internetAvailable = false;
        }

        Assume.assumeTrue("Internet connection not available, skipping TestStepOrbitFileScraper", internetAvailable);
    }

    @Test
    public void testGetFileURLsPreciseOrbitFileS1A() {
        final OrbitFileScraper scraper = new OrbitFileScraper.Step(SentinelPODOrbitFile.PRECISE);

        OrbitFileScraper.RemoteOrbitFile[] orbitFiles = scraper.getFileURLs("S1A", 2016, 2);
        assertEquals(29, orbitFiles.length);
    }

    @Test
    public void testGetFileURLsPreciseOrbitFileS1B() {
        final OrbitFileScraper scraper = new OrbitFileScraper.Step(SentinelPODOrbitFile.PRECISE);

        OrbitFileScraper.RemoteOrbitFile[] orbitFiles = scraper.getFileURLs("S1B", 2016, 7);
        assertEquals(31, orbitFiles.length);
    }

    @Test
    public void testGetFileURLsRestituteOrbitFileS1A() {
        final OrbitFileScraper scraper = new OrbitFileScraper.Step(SentinelPODOrbitFile.RESTITUTED);

        OrbitFileScraper.RemoteOrbitFile[] orbitFiles = scraper.getFileURLs("S1A", 2016, 2);
        assertEquals(591, orbitFiles.length);
    }

    @Test
    public void testGetFileURLsRestituteOrbitFileS1B() {
        final OrbitFileScraper scraper = new OrbitFileScraper.Step(SentinelPODOrbitFile.RESTITUTED);

        OrbitFileScraper.RemoteOrbitFile[] orbitFiles = scraper.getFileURLs("S1B", 2016, 7);
        assertEquals(592, orbitFiles.length);
    }

    @Test
    public void testDownloadPreciseOrbitFileS1A() throws Exception {
        final String missionPrefix = "S1A";
        final String orbitType = SentinelPODOrbitFile.PRECISE;
        final int year = 2016;
        final int month = 2;
        final int day = 28;
        final ProductData.UTC stateVectorTime = ProductData.UTC.parse("2016-02-28 15:19:21.698661", Sentinel1OrbitFileReader.orbitDateFormat);
        final File localFolder = SentinelPODOrbitFile.getDestFolder(missionPrefix, orbitType, year, month);

        final OrbitFileScraper scraper = new OrbitFileScraper.Step(orbitType);

        File orbitFile = scraper.download(localFolder,missionPrefix, orbitType,
                year, month, day, stateVectorTime);
        assertTrue(orbitFile.exists());
        orbitFile.delete();
    }

    @Test
    public void testDownloadPreciseOrbitFileS1B() throws Exception {
        final String missionPrefix = "S1B";
        final String orbitType = SentinelPODOrbitFile.PRECISE;
        final int year = 2018;
        final int month = 2;
        final int day = 28;
        final ProductData.UTC stateVectorTime = ProductData.UTC.parse("2018-02-28 15:19:21.698661", Sentinel1OrbitFileReader.orbitDateFormat);
        final File localFolder = SentinelPODOrbitFile.getDestFolder(missionPrefix, orbitType, year, month);

        final OrbitFileScraper scraper = new OrbitFileScraper.Step(SentinelPODOrbitFile.PRECISE);

        File orbitFile = scraper.download(localFolder,missionPrefix, orbitType,
                year, month, day, stateVectorTime);
        assertTrue(orbitFile.exists());
        orbitFile.delete();
    }
}
