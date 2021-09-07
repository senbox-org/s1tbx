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
package org.esa.s1tbx.io.productgroup.support;

import org.esa.s1tbx.commons.io.JSON;
import org.json.simple.JSONObject;

import java.io.File;
import java.util.*;

@SuppressWarnings("unchecked")
public class ProductGroupMetadataFile {

    public static final String PRODUCT_GROUP_METADATA_FILE = "product_group.json";
    public static final String ASSETS = "assets";
    public static final String PRODUCT_GROUP_VERSION = "snap:product_group_version";
    public static final String ID = "id";
    public static final String PRODUCT_TYPE = "snap:product_type";
    public static final String LATEST_VERSION = "0.1";

    private final List<ProductGroupAsset> assetList = new ArrayList<>();
    private String productName;
    private String productType;

    public ProductGroupMetadataFile() {
    }

    public void addAsset(final ProductGroupAsset asset) {
        assetList.add(asset);
    }

    public ProductGroupAsset findAsset(final ProductGroupAsset newAsset) {
        for(ProductGroupAsset asset : assetList) {
            if(asset.equals(newAsset)) {
                return asset;
            }
        }
        return null;
    }

    public ProductGroupAsset[] getAssets() {
        return assetList.toArray(new ProductGroupAsset[0]);
    }

    public String getProductName() {
        return productName;
    }

    public String getProductType() {
        return productType;
    }

    public void read(final File file) throws Exception {
        final JSONObject json = (JSONObject)JSON.loadJSON(file);
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
                ProductGroupAsset asset = ProductGroupAsset.createAsset((String)key, cnt, assetJSON);
                assetList.add(asset);
                ++cnt;
            }
            assetList.sort(Comparator.comparingInt(ProductGroupAsset::getIndex));
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
        for(ProductGroupAsset asset : assetList) {
            asset.setIndex(index++);
            final JSONObject assetJSON = new JSONObject();
            assets.put(asset.getName(), assetJSON);
            asset.write(assetJSON);
        }

        JSON.write(json, file);
        return file;
    }

}
