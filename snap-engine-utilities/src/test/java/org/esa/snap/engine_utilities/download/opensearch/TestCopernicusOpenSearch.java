/*
 * Copyright (C) 2017 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.download.opensearch;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.download.opendata.OpenData;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by lveci on 2/22/2017.
 */
public class TestCopernicusOpenSearch {

    private static final String COPERNICUS_HOST = "https://scihub.copernicus.eu";
    private static final String searchURL = "https://scihub.copernicus.eu/dhus/search?q=( footprint:\"Intersects(POLYGON((-74.24323771090575 -34.81331346157173,-31.2668365052604 -34.81331346157173,-31.2668365052604 5.647318588641241,-74.24323771090575 5.647318588641241,-74.24323771090575 -34.81331346157173)))\" ) AND ( beginPosition:[2016-01-25T00:00:00.000Z TO 2016-01-25T23:59:59.999Z] AND endPosition:[2016-01-25T00:00:00.000Z TO 2016-01-25T23:59:59.999Z] ) AND (platformname:Sentinel-1 AND producttype:GRD)";

    private static final String COPERNICUS_ODATA_ROOT = "https://scihub.copernicus.eu/dhus/odata/v1/";

    private static final String outputFolder = "e:\\tmp\\";

    @Test
    @Ignore
    public void testConnect() throws IOException {
        final OpenSearch openSearch = new OpenSearch(COPERNICUS_HOST);
        final OpenSearch.PageResult pageResult = openSearch.getPages(searchURL);

        try {
            final OpenSearch.ProductResult[] productResults = openSearch.getProductResults(pageResult, ProgressMonitor.NULL);

            SystemUtils.LOG.info("Retrieved " + productResults.length + " Product Ids");
            for (OpenSearch.ProductResult result : productResults) {
                //System.out.println("id: " + result.id);
            }

            final OpenData openData = new OpenData(COPERNICUS_HOST, COPERNICUS_ODATA_ROOT);
            for (OpenSearch.ProductResult result : productResults) {
                try {
                    OpenData.Entry entry = openData.getEntryByID(result.id);
                    SystemUtils.LOG.info(entry.fileName);

                    //openData.getManifest(result.id, entry, outputFolder);

                    //openData.getProduct(result.id, entry, outputFolder);

                } catch (IOException e) {
                    throw e;
                }
            }
        } catch (Exception e) {
            System.out.println("TestCopernicusOpenSearch.testConnect: caught exception:" + e.getMessage());
            if (e instanceof IOException) throw new IOException(e.getMessage());
        }
    }
}
