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
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.Credentials;
import org.esa.snap.engine_utilities.db.DBQuery;
import org.esa.snap.engine_utilities.db.ProductEntry;
import org.esa.snap.engine_utilities.db.ProductQueryInterface;
import org.esa.snap.engine_utilities.download.opendata.OpenData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Search interface for Products from a repository
 */
public class CopernicusProductQuery implements ProductQueryInterface {

    private static CopernicusProductQuery instance;

    private ProductEntry[] productEntryListFull = null;
    private ProductEntry[] productEntryList = null;

    public static final String NAME = "ESA SciHub";
    public static final String COPERNICUS_HOST = "https://scihub.copernicus.eu";
    private static final String COPERNICUS_ODATA_ROOT = "https://scihub.copernicus.eu/dhus/odata/v1/";
    //private static final String COPERNICUS_ODATA_ROOT = "https://scihub.copernicus.eu/apihub/odata/v1/";

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

        ProductEntry[] productEntries = dbQuery.intersectMapSelection(productEntryListFull, true);
        if (productEntries != null) {
            //System.out.println("CopernicusProductQuery.partialQuery productEntries.length = " + productEntries.length);
            productEntryList = productEntries;
        } /*else {
            System.out.println("CopernicusProductQuery.partialQuery productEntries is null");
        }*/

        return true;
    }

    public boolean fullQuery(final DBQuery dbQuery, final ProgressMonitor pm) throws Exception {
        pm.beginTask("Searching " + NAME + "...", 20);
        try {
            final OpenSearch openSearch = new OpenSearch(COPERNICUS_HOST);
            pm.worked(1);
            if(pm.isCanceled()) {
                return true;
            }

            final CopernicusQueryBuilder queryBuilder = new CopernicusQueryBuilder(dbQuery);
            final OpenSearch.PageResult pageResult = openSearch.getPages(queryBuilder.getSearchURL());
            pm.setTaskName("Searching " + NAME + " (" + pageResult.totalResults + " entries)...");
            pm.worked(1);
            if(pm.isCanceled()) {
                return true;
            }

            final OpenSearch.ProductResult[] productResults = openSearch.getProductResults(pageResult, SubProgressMonitor.create(pm, 7));
            if (productResults == null) {
                throw new Exception("SciHub search failed");
            }

            final OpenData openData = new OpenData(COPERNICUS_HOST, COPERNICUS_ODATA_ROOT);
            SystemUtils.LOG.info("CopernicusProductQuery.fullQuery: after openData");
            pm.worked(1);
            if(pm.isCanceled()) {
                return true;
            }

            final ProgressMonitor pm2 = SubProgressMonitor.create(pm, 10);
            pm2.beginTask("Retrieving entry information...", productResults.length);
            pm2.setSubTaskName("Retrieving entry information (" + productResults.length + " entries)..."); // without this, the task name does hot show up

            final List<ProductEntry> resultList = new ArrayList<>();
            int cnt = 0;
            int pct = 1;
            final int part = Math.max(50, (int) (((double) productResults.length / 100.0) + 0.5));
            SystemUtils.LOG.info("CopernicusProductQuery.fullQuery: part = " + part);
            for (OpenSearch.ProductResult result : productResults) {
                if  (pm.isCanceled()) {
                    break;
                }
                if (cnt >= pct * part) {
                    SystemUtils.LOG.info("CopernicusProductQuery.fullQuery: get entries " + cnt + " out of " + productResults.length + " done");
                    pct++;
                }

                if(pm.isCanceled()) {
                    break;
                }

                final ProductEntry productEntry = new ProductEntry(result);

                final OpenData.Entry entry = openData.getEntryByID(result.id);
                productEntry.setGeoBoundary(entry.footprint);

                resultList.add(productEntry);
                cnt++;
                pm2.worked(1);
            }
            if (cnt == productResults.length) {
                SystemUtils.LOG.info("CopernicusProductQuery.fullQuery: get entries " + cnt + " all of " + productResults.length + " done");
            }
            pm2.done();

            if(pm.isCanceled()) {
                return true;
            }

            productEntryList = resultList.toArray(new ProductEntry[resultList.size()]);
            productEntryListFull = productEntryList.clone();
            pm.worked(1);
            
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
