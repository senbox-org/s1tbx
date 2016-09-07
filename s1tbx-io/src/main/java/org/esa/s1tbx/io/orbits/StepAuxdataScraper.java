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
public class StepAuxdataScraper {

    private static final String stepS1OrbitsUrl = "http://step.esa.int/auxdata/orbits/Sentinel-1/";

    private final String orbitType;
    private String remotePath;

    public StepAuxdataScraper(final String orbitType) {
        this.orbitType = orbitType;
        System.setProperty("jsse.enableSNIExtension", "false");
    }

    public String getRemoteURL() {
        return remotePath;
    }

    public String[] getFileURLs(final String mission, final int year, final int month) {
        final String monthStr = StringUtils.padNum(month, 2, '0');

        remotePath = stepS1OrbitsUrl + orbitType + '/' + mission + '/' + year + '/' + monthStr + '/';

        final List<String> newLinks = getFileURLs(remotePath);

        return newLinks.toArray(new String[newLinks.size()]);
    }

    private List<String> getFileURLs(String path) {
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
                        if(link.endsWith(".zip")) {
                            fileList.add(elem.text());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileList;
    }
}
