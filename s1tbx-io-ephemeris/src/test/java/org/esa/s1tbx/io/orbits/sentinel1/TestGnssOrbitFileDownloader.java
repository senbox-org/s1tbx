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

import org.esa.s1tbx.cloud.opendata.OpenData;
import org.esa.s1tbx.cloud.opensearch.OpenSearch;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.util.ZipUtils;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestGnssOrbitFileDownloader {

    private static final String query = "https://scihub.copernicus.eu/gnss/search?q=producttype:AUX_RESORB AND ingestiondate:[2021-02-10T10:00:000Z TO 2021-02-15T10:00:000Z] AND platformname:Sentinel-1";

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

        Assume.assumeTrue("Internet connection not available, skipping TestGnssOrbitFileDownloader", internetAvailable);
    }

    @Test
    public void testConstructQuery() throws Exception {
        String expected = "https://scihub.copernicus.eu/gnss/search?q=platformname:Sentinel-1 AND platformnumber:B AND producttype:AUX_POEORB AND beginposition:[2021-02-01T00:00:000Z TO 2021-02-31T24:00:000Z]";

        String queryStr = GnssOrbitFileDownloader.constructQuery("Sentinel-1", "S1B", "AUX_POEORB", 2021, 2, 10);
        assertEquals(expected, queryStr);
    }

    @Test
    public void testConnect() throws Exception  {
        final OpenSearch openSearch = new OpenSearch(GnssOrbitFileDownloader.COPERNICUS_HOST,
                GnssOrbitFileDownloader.USER_NAME, GnssOrbitFileDownloader.PASSWORD);

        final OpenSearch.PageResult pageResult = openSearch.getPages(query);

        OpenSearch.SearchResult[] searchResults = openSearch.getSearchResults(pageResult);

        final File outputFolder = Files.createTempDirectory("gnss").toFile();
        final String downloadURL = GnssOrbitFileDownloader.COPERNICUS_ODATA_ROOT+"Products('" + searchResults[0].id + "')" + "/$value";

        final OpenData openData = new OpenData(GnssOrbitFileDownloader.COPERNICUS_ODATA_ROOT,
                GnssOrbitFileDownloader.USER_NAME, GnssOrbitFileDownloader.PASSWORD);
        File localFile = openData.download(searchResults[0].id, downloadURL, outputFolder, ".EOF");

        if (localFile.exists()) {
            final File localZipFile = FileUtils.exchangeExtension(localFile, ".EOF.zip");
            ZipUtils.zipFile(localFile, localZipFile);
            localFile.delete();
        }

        FileUtils.deleteTree(outputFolder);
    }

    @Test
    public void testDownloadPreciseOrbitFileS1A() throws Exception {
        final String mission = "Sentinel-1";
        final String missionPrefix = "S1A";
        final String orbitType = SentinelPODOrbitFile.PRECISE;
        final int year = 2021;
        final int month = 1;
        final int day = 27;
        final ProductData.UTC stateVectorTime = ProductData.UTC.parse("2021-01-27 15:19:21.698661", Sentinel1OrbitFileReader.orbitDateFormat);
        final File localFolder = SentinelPODOrbitFile.getDestFolder(missionPrefix, orbitType, year, month);

        final GnssOrbitFileDownloader gnssDownloader = new GnssOrbitFileDownloader();

        File orbitFile = gnssDownloader.download(localFolder, mission, missionPrefix, orbitType,
                year, month, day, stateVectorTime);
        assertTrue(orbitFile.exists());
        orbitFile.delete();
    }

    @Test
    public void testDownloadPreciseOrbitFileS1A_NextMonth() throws Exception {
        final String mission = "Sentinel-1";
        final String missionPrefix = "S1A";
        final String orbitType = SentinelPODOrbitFile.PRECISE;
        final int year = 2021;
        final int month = 1;
        final int day = 28;
        final ProductData.UTC stateVectorTime = ProductData.UTC.parse("2021-01-28 15:19:21.698661", Sentinel1OrbitFileReader.orbitDateFormat);
        final File localFolder = SentinelPODOrbitFile.getDestFolder(missionPrefix, orbitType, year, month);

        final GnssOrbitFileDownloader gnssDownloader = new GnssOrbitFileDownloader();

        File orbitFile = gnssDownloader.download(localFolder, mission, missionPrefix, orbitType,
                year, month, day, stateVectorTime);
        assertTrue(orbitFile.exists());
        orbitFile.delete();
    }

    @Test
    public void testDownloadRestituteOrbitFileS1B() throws Exception {
        final String mission = "Sentinel-1";
        final String missionPrefix = "S1B";
        final String orbitType = SentinelPODOrbitFile.RESTITUTED;
        final int year = 2021;
        final int month = 2;
        final int day = 14;
        final ProductData.UTC stateVectorTime = ProductData.UTC.parse("2021-02-15 15:19:21.698661", Sentinel1OrbitFileReader.orbitDateFormat);
        final File localFolder = SentinelPODOrbitFile.getDestFolder(missionPrefix, orbitType, year, month);

        final GnssOrbitFileDownloader gnssDownloader = new GnssOrbitFileDownloader();

        File orbitFile = gnssDownloader.download(localFolder, mission, missionPrefix, orbitType,
                year, month, day, stateVectorTime);
        assertTrue(orbitFile.exists());
        orbitFile.delete();
    }
}
