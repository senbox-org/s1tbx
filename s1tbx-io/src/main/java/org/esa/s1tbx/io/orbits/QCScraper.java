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

import com.jaunt.Element;
import com.jaunt.Elements;
import com.jaunt.JauntException;
import com.jaunt.UserAgent;

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
    private static final String page = "/?page=";
    private static final String validityStartYear = "&validity_start_time=";
    private static final String validityStartMonth = "&validity_start_time=";

    private final UserAgent userAgent;
    private final String orbitType;

    public QCScraper(String orbitType) {
        userAgent = new UserAgent();
        userAgent.settings.checkSSLCerts = false;
        this.orbitType = orbitType;
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
            userAgent.visit(path);

            Elements tables = userAgent.doc.findEvery("<table class=\"table table-striped\">");
            int cnt = 1;
            for (Element table : tables) {
                Elements addrLinks = table.findEvery("<a>");
                for (Element addr : addrLinks) {
                    fileList.add(addr.getText());
                }
            }
        } catch (JauntException e) {
            e.printStackTrace();
        }
        return fileList;
    }
}
