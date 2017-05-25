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
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by lveci on 2/22/2017.
 */
public class TestASFVertexOpenSearch {

    private static final String ASF_VERTEX_HOST = "https://api.daac.asf.alaska.edu";
    private static final String searchURL = "https://api.daac.asf.alaska.edu/services/search/param?polygon=-155.08,65.82,-153.28,64.47,-149.94,64.55,-149.50,63.07,-153.5,61.91\\&platform=Sentinel-1A\\&maxResults=10";

    @Test
    @Ignore
    public void testConnect() throws IOException {
        final OpenSearch openSearch = new OpenSearch(ASF_VERTEX_HOST);
        final OpenSearch.PageResult pageResult = openSearch.getPages(searchURL);

        try {
            final OpenSearch.ProductResult[] productResults = openSearch.getProductResults(pageResult, ProgressMonitor.NULL);

            System.out.println("Retrieved Product Ids");
            for (OpenSearch.ProductResult result : productResults) {
                System.out.println("id: " + result.id);
            }
        } catch (Exception e) {
            System.out.println("TestASFVertexOpenSearch.testConnect: caught exception:" + e.getMessage());
            if (e instanceof IOException) throw new IOException(e.getMessage());
        }

//        final OpenData openData = new OpenData(ASF_VERTEX_HOST);
//        for(OpenSearch.ProductResult result : productResults){
//            try {
//                openData.getProductByID(result.id, "", "", "");
//            } catch (IOException e) {
//                throw e;
//            }
//        }
    }
}
