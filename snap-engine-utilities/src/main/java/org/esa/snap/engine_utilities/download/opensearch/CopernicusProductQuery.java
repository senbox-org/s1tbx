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
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.engine_utilities.datamodel.Credentials;
import org.esa.snap.engine_utilities.db.DBQuery;
import org.esa.snap.engine_utilities.db.ProductEntry;
import org.esa.snap.engine_utilities.db.ProductQueryInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Search interface for Products from a repository
 */
public class CopernicusProductQuery implements ProductQueryInterface {

    private static CopernicusProductQuery instance;

    private ProductEntry[] productEntryList = null;

    public static final String NAME = "ESA SciHub";
    public static final String COPERNICUS_HOST = "https://scihub.copernicus.eu";

    private static final String[] emptyStringList = new String[]{};
    private static final String[] COPERNICUS_MISSIONS = new String[]{"Sentinel-1", "Sentinel-2", "Sentinel-3"};

    private static final String[] S1_PRODUCT_TYPES = new String[]{"SLC", "GRD", "OCN"};
    private static final String[] S2_PRODUCT_TYPES = new String[]{"S2MSI1C"};
    private static final String[] S3_PRODUCT_TYPES = new String[]{"OLCI"};

    private static final String[] S1_MODES = new String[]{"SM", "IW", "EW", "WV"};
    private static final String[] S2_MODES = new String[]{"S2MSI1C"};
    private static final String[] S3_MODES = new String[]{"OLCI"};

    private CopernicusProductQuery() {
    }

    public static CopernicusProductQuery instance() {
        if (instance == null) {
            instance = new CopernicusProductQuery();
        }
        return instance;
    }

    public boolean isReady() {
        return true;
    }

    public boolean partialQuery(final DBQuery dbQuery) throws Exception {
        return false;
    }

    public boolean fullQuery(final DBQuery dbQuery, final ProgressMonitor pm) throws Exception {
        pm.beginTask("Searching " + NAME + "...", 3);
        try {
            pm.worked(1);

            final OpenSearch openSearch = new OpenSearch(COPERNICUS_HOST);
            final CopernicusQueryBuilder queryBuilder = new CopernicusQueryBuilder(dbQuery);
            final OpenSearch.PageResult pageResult = openSearch.getPages(queryBuilder.getSearchURL());
            pm.worked(1);

            final OpenSearch.ProductResult[] productResults = openSearch.getProductResults(pageResult);
            pm.worked(1);

            final List<ProductEntry> resultList = new ArrayList<>();
            for (OpenSearch.ProductResult result : productResults) {
                resultList.add(new ProductEntry(result));
            }
            productEntryList = resultList.toArray(new ProductEntry[resultList.size()]);
            return true;
        } finally {
            pm.done();
        }
    }

    private static Credentials.CredentialInfo getCredentials(final String host) throws IOException {
        Credentials.CredentialInfo credentialInfo = Credentials.instance().get(host);
        if (credentialInfo == null) {
            throw new IOException("Credentials for " + host + " not found.");
        }
        return credentialInfo;
    }

    public ProductEntry[] getProductEntryList() {
        if (productEntryList == null) {
            productEntryList = new ProductEntry[]{};
        }
        return productEntryList;
    }

    public String[] getAllMissions() {
        return COPERNICUS_MISSIONS;
    }

    public String[] getAllProductTypes(final String[] missions) {
        if (missions == null) {
            return emptyStringList;
        }
        final List<String> productTypeList = new ArrayList<>();
        if (StringUtils.contains(missions, COPERNICUS_MISSIONS[0])) {
            productTypeList.addAll(Arrays.asList(S1_PRODUCT_TYPES));
        }
        if (StringUtils.contains(missions, COPERNICUS_MISSIONS[1])) {
            productTypeList.addAll(Arrays.asList(S2_PRODUCT_TYPES));
        }
        if (StringUtils.contains(missions, COPERNICUS_MISSIONS[2])) {
            productTypeList.addAll(Arrays.asList(S3_PRODUCT_TYPES));
        }
        return productTypeList.toArray(new String[productTypeList.size()]);
    }

    public String[] getAllAcquisitionModes(final String[] missions) {
        if (missions == null) {
            return emptyStringList;
        }
        final List<String> modesList = new ArrayList<>();
        if (StringUtils.contains(missions, COPERNICUS_MISSIONS[0])) {
            modesList.addAll(Arrays.asList(S1_MODES));
        }
        if (StringUtils.contains(missions, COPERNICUS_MISSIONS[1])) {
            modesList.addAll(Arrays.asList(S2_MODES));
        }
        if (StringUtils.contains(missions, COPERNICUS_MISSIONS[2])) {
            modesList.addAll(Arrays.asList(S3_MODES));
        }
        return modesList.toArray(new String[modesList.size()]);
    }
}
