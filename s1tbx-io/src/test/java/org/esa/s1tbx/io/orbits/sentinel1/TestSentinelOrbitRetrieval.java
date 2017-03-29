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

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.datamodel.DownloadableContentImpl;
import org.esa.snap.engine_utilities.util.ZipUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static junit.framework.TestCase.assertEquals;

/**
 * Test Sentinel-1 QC Scrapping of orbit files
 */
public class TestSentinelOrbitRetrieval {
    private static final int STARTYEARS1A = 2017; //2014;
    private static final int STARTYEARS1B = 2017; //2015;
    private static final int ENDYEAR = 2018;
    private static final String S1A = "S1A";
    private static final String S1B = "S1B";

    private static final boolean ZIP = true;

    @Test
    @Ignore
    public void testDownloadAllPreciseOrbitFiles() throws IOException {

        retrieveMission(S1A, SentinelPODOrbitFile.PRECISE, STARTYEARS1A, ENDYEAR);

        retrieveMission(S1B, SentinelPODOrbitFile.PRECISE, STARTYEARS1B, ENDYEAR);
    }

    @Test
    @Ignore
    public void testDownloadAllRestitudedOrbitFiles() throws IOException {

        retrieveMission(S1A, SentinelPODOrbitFile.RESTITUTED, STARTYEARS1A, ENDYEAR);

        retrieveMission(S1B, SentinelPODOrbitFile.RESTITUTED, STARTYEARS1B, ENDYEAR);
    }

    private void retrieveMission(final String missionPrefix, final String orbitType,
                                final int startYear, final int endYear) throws IOException {
        final SSLUtil ssl = new SSLUtil();
        ssl.disableSSLCertificateCheck();

        for(int year=startYear; year < endYear; ++year) {
            for(int month=1; month <= 12; ++month) {
                retrieveFolder(missionPrefix, orbitType, year, month);
            }
        }

        ssl.enableSSLCertificateCheck();
    }

    private void retrieveFolder(final String missionPrefix, final String orbitType, final int year, final int month)
            throws IOException {
        final File destFolder = SentinelPODOrbitFile.getDestFolder(missionPrefix, orbitType, year, month);

        final QCScraper qc = new QCScraper(orbitType);
        final URL remotePath = new URL(qc.getRemoteURL());
        final String[] orbitFiles = qc.getFileURLs(missionPrefix, year, month);

        if(orbitFiles.length == 0) {
            return;
        }

        SystemUtils.LOG.info("Retrieving " + orbitFiles.length +" files " + year + ' ' + month + " to " + destFolder.getAbsolutePath());

        int cnt = 0;
        for (String file : orbitFiles) {
            final File newFile = new File(destFolder, file);
            final File localZipFile = FileUtils.exchangeExtension(newFile, ".EOF.zip");
            if (!newFile.exists() && !localZipFile.exists()) {
                download(remotePath, newFile);
                ++cnt;
            }
        }
        if (cnt > 0) {
            SystemUtils.LOG.info(cnt + " new files retrieved");
        }

        final File[] localFiles = destFolder.listFiles();
        assertEquals(orbitFiles.length, localFiles.length);

        final StepAuxdataScraper step = new StepAuxdataScraper(orbitType);
        final String[] stepOrbitFiles = step.getFileURLs(missionPrefix, year, month);
        if(orbitFiles.length != stepOrbitFiles.length) {
            SystemUtils.LOG.severe("Step "+ missionPrefix +' '+ year +' '+ month + " incomplete");
            SystemUtils.LOG.severe("Step "+ stepOrbitFiles.length + " vs QC " + orbitFiles.length);
            //assertEquals(orbitFiles.length, stepOrbitFiles.length);
        }
    }

    private void download(final URL remotePath, final File file) throws IOException {

        DownloadableContentImpl.getRemoteHttpFile(remotePath, file);
        if (ZIP && file.exists()) {
            final File localZipFile = FileUtils.exchangeExtension(file, ".EOF.zip");
            ZipUtils.zipFile(file, localZipFile);
            file.delete();
        }
    }
}
