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

import org.esa.snap.engine_utilities.db.DBQuery;
import org.esa.snap.engine_utilities.db.ProductEntry;
import org.esa.snap.engine_utilities.db.ProductQueryInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Search interface for Products from a repository
 */
public class CopernicusProductQuery implements ProductQueryInterface {

    private static CopernicusProductQuery instance;

    private static final String COPERNICUS_HOST = "https://scihub.copernicus.eu";
    private static final String searchURL = COPERNICUS_HOST +"/dhus/search?q=( footprint:\"Intersects(POLYGON((-74.24323771090575 -34.81331346157173,-31.2668365052604 -34.81331346157173,-31.2668365052604 5.647318588641241,-74.24323771090575 5.647318588641241,-74.24323771090575 -34.81331346157173)))\" ) AND ( beginPosition:[2016-01-25T00:00:00.000Z TO 2016-01-25T23:59:59.999Z] AND endPosition:[2016-01-25T00:00:00.000Z TO 2016-01-25T23:59:59.999Z] ) AND (platformname:Sentinel-1 AND producttype:GRD)";


    private ProductEntry[] productEntryList = null;

    private static final String[] emptyStringList = new String[] {};
    private static final String[] COPERNICUS_MISSIONS = new String[] { "Sentinel-1", "Sentinel-2", "Sentinel-3"};


    private CopernicusProductQuery() {

    }

    public static CopernicusProductQuery instance() {
        if(instance == null) {
            instance = new CopernicusProductQuery();
        }
        return instance;
    }

    public boolean isReady() {
        return true;
    }

    public void query(final DBQuery dbQuery) throws Exception {
        final OpenSearch openSearch = new OpenSearch(COPERNICUS_HOST);
        OpenSearch.Result result = openSearch.getPages(searchURL);

        final String[] productIds = openSearch.getProductIDs(result);

        final List<ProductEntry> resultList = new ArrayList<>();
        for(String id : productIds) {
            resultList.add(new ProductEntry(-1, id, null));
        }
        productEntryList = resultList.toArray(new ProductEntry[resultList.size()]);
    }

    public ProductEntry[] getProductEntryList() {
        if(productEntryList == null) {
            productEntryList = new ProductEntry[] {};
        }
        return productEntryList;
    }

    public String[] getAllMissions() {
        return COPERNICUS_MISSIONS;
    }

    public String[] getAllProductTypes(final String[] missions) {
        return emptyStringList;
    }

    public String[] getAllAcquisitionModes(final String[] missions) {
        return emptyStringList;
    }
}
