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


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Download orbit files directly from QC website
 */
public class QCScraper {

    public static final String POEORB = "aux_poeorb";
    public static final String RESORB = "aux_resorb";

    private static final String qcUrl = "https://qc.sentinel1.eo.esa.int/";
    private static final String page = "/?page=";
    private static final String validityStartYear = "&validity_start_time=";
    private static final String validityStartMonth = "&validity_start_time=";

    private final String orbitType;

    public QCScraper(String orbitType) {
        this.orbitType = orbitType;
        System.setProperty("jsse.enableSNIExtension", "false");
    }

    public String getQCURL() {
        return qcUrl + orbitType + '/';
    }

    public String[] getFileURLs(int year, int month) {
        final Set<String> fileList = new HashSet<>();

        // for each day of the month
        final int maxPage = orbitType.equals(RESORB) ? 32 : 3;
        for (int day = 1; day <= maxPage; ++day) {
            final String path = qcUrl + orbitType + page + day;
            final String validity = validityStartYear + year + validityStartMonth + year + '-' + month;

            fileList.addAll(getFileURLs(path + validity));
        }

        return fileList.toArray(new String[fileList.size()]);
    }

    private List<String> getFileURLs(String path) {
        final List<String> fileList = new ArrayList<>();
        try {
            final Document doc = Jsoup.connect(path).validateTLSCertificates(false).get();

            Element table = doc.select("table").first();
            Elements tbRows = table.select("tr");

            for(Element row : tbRows) {
                Elements tbCols = row.select("td");
                for(Element col : tbCols) {
                    Elements elems = col.getElementsByTag("a");
                    for(Element elem : elems) {
                        fileList.add(elem.text());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileList;
    }
}
