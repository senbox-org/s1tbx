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
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.engine_utilities.download.DownloadableContentImpl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Download orbit files directly from QC website
 */
abstract class OrbitFileScraper {

    private static final String POEORB = "POEORB";
    private static final String RESORB = "RESORB";
    private static final String[] EXTS = new String[] {".zip",".eof"};

    protected String baseURL;
    protected final String orbitType;

    protected OrbitFileScraper(final String orbitType) {
        this.orbitType = convertOrbitType(orbitType);
        System.setProperty("jsse.enableSNIExtension", "false");
    }

    abstract RemoteOrbitFile[] getFileURLs(final String mission, final int year, final int month);

    public File download(final File localFolder, final String missionPrefix, final String orbitType,
                         int year, int month, final int day, final ProductData.UTC stateVectorTime) throws Exception {
        scrapeOrbitFiles(localFolder, missionPrefix, year, month, stateVectorTime);
        File orbitFile = SentinelPODOrbitFile.findOrbitFile(missionPrefix, orbitType, stateVectorTime, year, month);
        if (orbitFile == null) {
            NewDate newDate = getNeighouringMonth(year, month, day);
            scrapeOrbitFiles(localFolder, missionPrefix, newDate.year, newDate.month, stateVectorTime);
            orbitFile = SentinelPODOrbitFile.findOrbitFile(missionPrefix, orbitType, stateVectorTime, year, month);
            if (orbitFile == null) {
                orbitFile = SentinelPODOrbitFile.findOrbitFile(missionPrefix, orbitType, stateVectorTime, newDate.year, newDate.month);
            }
        }
        return orbitFile;
    }

    static class NewDate {
        final int month;
        final int year;
        NewDate(int year, int month) {
            this.year = year;
            this.month = month;
        }
    }

    private static String convertOrbitType(final String orbitType) {
        if(orbitType.contains("Restituted")) {
            return RESORB;
        } else {
            return POEORB;
        }
    }

    static NewDate getNeighouringMonth(int year, int month, final int day) {
        if (day < 15) {
            month--;
            if (month < 1) {
                month = 12;
                year--;
            }
        } else {
            month++;
            if (month > 12) {
                month = 1;
                year++;
            }
        }
        return new NewDate(year, month);
    }

    private void scrapeOrbitFiles(final File localFolder, final String missionPrefix, int year, int month,
                                  final ProductData.UTC stateVectorTime) throws Exception {

        final OrbitFileScraper.RemoteOrbitFile[] orbitFiles = getFileURLs(missionPrefix, year, month);
        final SSLUtil ssl = new SSLUtil();
        ssl.disableSSLCertificateCheck();

        for (OrbitFileScraper.RemoteOrbitFile file : orbitFiles) {
            if (Sentinel1OrbitFileReader.isWithinRange(file.fileName, stateVectorTime)) {
                final File localFile = new File(localFolder, file.fileName);
                DownloadableContentImpl.getRemoteHttpFile(new URL(file.remotePath), localFile);
                break;
            }
        }

        ssl.enableSSLCertificateCheck();
    }

    protected List<RemoteOrbitFile> getFileURLs(final String remotePath) {
        final List<RemoteOrbitFile> fileList = new ArrayList<>();
        try {
            final Document doc = Jsoup.connect(remotePath).timeout(10*1000).get();

            findLinks(remotePath, doc, fileList);
        } catch (Exception e) {
            //SystemUtils.LOG.warning("Unable to connect to "+remotePath+ ": "+e.getMessage());
        }
        return fileList;
    }

    private void findLinks(final String remotePath, final Element elem, final List<RemoteOrbitFile> fileList) {
        Elements addrs = elem.getElementsByTag("a");
        for (Element addr : addrs) {
            String link = addr.text();
            for(String ext : EXTS) {
                if (link.toLowerCase().endsWith(ext)) {
                    fileList.add(new RemoteOrbitFile(remotePath, addr.text()));
                }
            }
        }
    }

    public static class RemoteOrbitFile {
        String fileName;
        String remotePath;
        public RemoteOrbitFile(final String path, final String name) {
            this.remotePath = path;
            this.fileName = name;
        }
    }

    public static class Step extends OrbitFileScraper {
        private static final String stepS1OrbitsUrl = "http://step.esa.int/auxdata/orbits/Sentinel-1/";

        public Step(final String orbitType) {
            super(orbitType);
            this.baseURL = stepS1OrbitsUrl;
        }

        @Override
        RemoteOrbitFile[] getFileURLs(final String mission, final int year, final int month) {
            final String monthStr = StringUtils.padNum(month, 2, '0');

            String remotePath = baseURL + orbitType + '/' + mission + '/' + year + '/' + monthStr + '/';

            final List<RemoteOrbitFile> remoteOrbitFiles = getFileURLs(remotePath);

            return remoteOrbitFiles.toArray(new RemoteOrbitFile[0]);
        }
    }
}
