/*
 * Copyright (C) 2021 SkyWatch. https://www.skywatch.com
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
package org.esa.s1tbx.io.productgroup;

import org.esa.s1tbx.commons.io.JSONUtils;
import org.esa.snap.core.datamodel.ProductData;
import org.json.simple.JSONObject;

import java.io.File;
import java.util.*;

@SuppressWarnings("unchecked")
public class ProductGroupMetadataFile {

    public static final String PRODUCT_GROUP_METADATA_FILE = "product_group.json";

    private final List<Asset> assetList = new ArrayList<>();
    private String productName;
    private String productType;

    private static final String ASSETS = "assets";
    private static final String HREF = "href";
    private static final String FORMAT = "snap:format";
    private static final String ASSET_INDEX = "snap:asset_index";                       //optional to enforce band order
    private static final String UPDATED_DATE = "snap:updated_date";
    private static final String PRODUCT_GROUP_VERSION = "snap:product_group_version";
    private static final String ID = "id";
    private static final String PRODUCT_TYPE = "snap:product_type";

    private static final String LATEST_VERSION = "0.1";

    public ProductGroupMetadataFile() {
    }

    public void addAsset(final Asset asset) {
        assetList.add(asset);
    }

    public Asset findAsset(final Asset newAsset) {
        for(Asset asset : assetList) {
            if(asset.equals(newAsset)) {
                return asset;
            }
        }
        return null;
    }

    public Asset[] getAssets() {
        return assetList.toArray(new Asset[0]);
    }

    public String getProductName() {
        return productName;
    }

    public String getProductType() {
        return productType;
    }

    public void read(final File file) throws Exception {
        final JSONObject json = (JSONObject)JSONUtils.loadJSONFile(file);
        if(json.containsKey(ID)) {
            productName = (String)json.get(ID);
        }
        if(json.containsKey(PRODUCT_TYPE)) {
            productType = (String)json.get(PRODUCT_TYPE);
        }

        if(json.containsKey(ASSETS)) {
            final JSONObject assets = (JSONObject) json.get(ASSETS);
            int cnt = 0;
            for(Object key : assets.keySet()) {
                final JSONObject assetJSON = (JSONObject) assets.get(key);
                String name = (String)key;
                String path = (String)assetJSON.get(HREF);
                String format = (String)assetJSON.get(FORMAT);

                final Asset asset = new Asset(name, path, format);
                asset.index = cnt;
                if(assetJSON.containsKey(ASSET_INDEX)) {
                    asset.index = ((Long)assetJSON.get(ASSET_INDEX)).intValue();
                }
                assetList.add(asset);
                ++cnt;
            }
            assetList.sort(Comparator.comparingInt(o -> o.index));
        }
    }

    public File write(final String productName, final String productType, final File file) throws Exception {
        final JSONObject json = new JSONObject();
        json.put(PRODUCT_GROUP_VERSION, LATEST_VERSION);
        json.put(ID, productName);
        json.put(PRODUCT_TYPE, productType);

        final Map<Object, Object> assets = new LinkedHashMap<>();
        json.put(ASSETS, assets);

        int index = 1;
        for(Asset asset : assetList) {
            final JSONObject assetJSON = new JSONObject();
            assets.put(asset.name, assetJSON);
            assetJSON.put(HREF, asset.path);
            assetJSON.put(FORMAT, asset.format);
            assetJSON.put(ASSET_INDEX, index++);

            if(asset.updatedUTC == null) {
                asset.updatedUTC = ProductData.UTC.create(new Date(), 0);
            }
            assetJSON.put(UPDATED_DATE, asset.updatedUTC.format());
        }

        JSONUtils.writeJSON(json, file);
        return file;
    }

    public static class Asset {
        final String name;
        final String path;
        final String format;
        ProductData.UTC updatedUTC = null;
        int index = -1;

        public Asset(final String name, final String path, final String format) {
            this.name = name;
            this.path = path;
            this.format = format;
        }

        @Override
        public boolean equals(final Object obj) {
            if(super.equals(obj))
                return true;
            if(obj instanceof Asset) {
                Asset newAsset = (Asset)obj;
                return this.name.equals(newAsset.name) && this.path.equals(newAsset.path)
                        && this.format.equals(newAsset.format);
            }
            return false;
        }
    }
}
