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


import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Download orbit files directly from QC website
 */
abstract class OrbitFileScraper {

    private static final String stepS1OrbitsUrl = "http://step.esa.int/auxdata/orbits/Sentinel-1/";
    private static final String esaS1OrbitsUrl = "http://aux.sentinel1.eo.esa.int/";

    private static final String POEORB = "POEORB";
    private static final String RESORB = "RESORB";
    private static String[] EXTS = new String[] {".zip",".eof"};

    protected String baseURL;
    protected final String orbitType;

    protected OrbitFileScraper(final String orbitType) {
        if(orbitType.contains("Restituted")) {
            this.orbitType = RESORB;
        } else {
            this.orbitType = POEORB;
        }
        System.setProperty("jsse.enableSNIExtension", "false");
    }

    abstract RemoteOrbitFile[] getFileURLs(final String mission, final int year, final int month);

    protected List<RemoteOrbitFile> getFileURLs(final String remotePath) {
        final List<RemoteOrbitFile> fileList = new ArrayList<>();
        try {
            final Document doc = Jsoup.connect(remotePath).timeout(10*1000).get();

//            final Elements tables = doc.select("table");
//            for(Element table : tables) {
//                final Elements tbRows = table.select("tr");
//
//                for (Element row : tbRows) {
//                    Elements tbCols = row.select("td");
//                    for (Element col : tbCols) {
//                        findLinks(remotePath, col, fileList);
//                    }
//                }
//            }
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

    public static class ESA_S1 extends OrbitFileScraper {
        public ESA_S1(final String orbitType) {
            super(orbitType);
            this.baseURL = esaS1OrbitsUrl;
        }

        @Override
        RemoteOrbitFile[] getFileURLs(final String mission, final int year, final int month) {
            final String monthStr = StringUtils.padNum(month, 2, '0');
            final List<RemoteOrbitFile> allRemoteOrbitFiles = new ArrayList<>();

            for(int day = 1; day <= 31; day++) {
                final String dayStr = StringUtils.padNum(day, 2, '0');
                String remotePath = baseURL + orbitType + '/' + year + '/' + monthStr + '/' + dayStr + '/';

                allRemoteOrbitFiles.addAll(getFileURLs(remotePath));
            }

            final List<RemoteOrbitFile> remoteOrbitFiles = new ArrayList<>();
            for(RemoteOrbitFile remoteOrbitFile : allRemoteOrbitFiles) {
                if(remoteOrbitFile.fileName.startsWith(mission)) {
                    remoteOrbitFiles.add(remoteOrbitFile);
                }
            }
            return remoteOrbitFiles.toArray(new RemoteOrbitFile[0]);
        }
    }
}
