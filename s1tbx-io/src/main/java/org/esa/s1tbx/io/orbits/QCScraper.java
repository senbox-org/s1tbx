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
package org.esa.s1tbx.io.orbits;


import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Download orbit files directly from QC website
 */
public class QCScraper {

    public static final String POEORB = "aux_poeorb";
    public static final String RESORB = "aux_resorb";

    private static final String qcUrl = "https://qc.sentinel1.eo.esa.int/";
    private static final String pageVar = "/?page=";
    private static final String missionVar = "&mission=";
    private static final String validityStartYearVar = "&validity_start_time=";
    private static final String validityStartMonthVar = "&validity_start_time=";

    private final String orbitType;

    public QCScraper(final String orbitType) {
        this.orbitType = orbitType;
        System.setProperty("jsse.enableSNIExtension", "false");
    }

    public String getRemoteURL() {
        return qcUrl + orbitType + '/';
    }

    public String[] getFileURLs(final String mission, final int year, final int month) {
        final Set<String> fileList = new HashSet<>();
        String monthStr = StringUtils.padNum(month, 2, '0');

        // for each day of the month
        final int maxPage = orbitType.equals(RESORB) ? 70 : 6;
        for (int day = 1; day <= maxPage; ++day) {
            final String path = qcUrl + orbitType + pageVar + day + missionVar + mission;
            final String validity = validityStartYearVar + year + validityStartMonthVar + year + '-' + monthStr;

            final List<String> newLinks = getFileURLs(path + validity, fileList);
            if(newLinks.isEmpty()) {
                break;
            }
            fileList.addAll(newLinks);
        }

        return fileList.toArray(new String[fileList.size()]);
    }

    private static List<String> getFileURLs(String path, final Set<String> currentList) {
        final List<String> fileList = new ArrayList<>();
        try {
            final Document doc = Jsoup.connect(path).timeout(10*1000).validateTLSCertificates(false).get();

            final Element table = doc.select("table").first();
            final Elements tbRows = table.select("tr");

            for(Element row : tbRows) {
                Elements tbCols = row.select("td");
                for(Element col : tbCols) {
                    Elements elems = col.getElementsByTag("a");
                    for(Element elem : elems) {
                        String link = elem.text();
                        if(!currentList.contains(link)) {
                            fileList.add(elem.text());
                        }
                    }
                }
            }
        } catch (Exception e) {
            SystemUtils.LOG.warning("Unable to connect to "+path+ ": "+e.getMessage());
        }
        return fileList;
    }
}
