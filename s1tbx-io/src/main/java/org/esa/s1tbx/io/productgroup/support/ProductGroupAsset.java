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

import org.esa.snap.core.datamodel.ProductData;
import org.json.simple.JSONObject;

import java.util.Date;

@SuppressWarnings("unchecked")
public class ProductGroupAsset {
    private static final String HREF = "href";
    private static final String FORMAT = "snap:format";
    private static final String ASSET_INDEX = "snap:asset_index";                       //optional to enforce band order
    private static final String UPDATED_DATE = "snap:updated_date";

    private final String name;
    private final String path;
    private final String format;
    private ProductData.UTC updatedUTC = null;
    private boolean modified;
    private int index = -1;

    public ProductGroupAsset(final String name, final String path, final String format) {
        this.name = name;
        this.path = path;
        this.format = format;
        this.modified = true;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(final int index) {
        this.index = index;
    }

    public static ProductGroupAsset createAsset(final String name, int cnt, final JSONObject assetJSON) throws Exception  {
        String path = (String) assetJSON.get(HREF);
        String format = (String) assetJSON.get(FORMAT);

        final ProductGroupAsset asset = new ProductGroupAsset(name, path, format);
        asset.modified = false;

        if (assetJSON.containsKey(UPDATED_DATE)) {
            String updatedDate = (String) assetJSON.get(UPDATED_DATE);
            asset.updatedUTC = ProductData.UTC.parse(updatedDate);
        }
        asset.index = cnt;
        if (assetJSON.containsKey(ASSET_INDEX)) {
            asset.index = ((Long) assetJSON.get(ASSET_INDEX)).intValue();
        }
        return asset;
    }

    public void write(final JSONObject assetJSON) {
        assetJSON.put(HREF, path);
        assetJSON.put(FORMAT, format);
        assetJSON.put(ASSET_INDEX, index);

        if (updatedUTC == null || modified) {
            updatedUTC = ProductData.UTC.create(new Date(), 0);
        }
        assetJSON.put(UPDATED_DATE, updatedUTC.format());
    }

    @Override
    public boolean equals(final Object obj) {
        if (super.equals(obj))
            return true;
        if (obj instanceof ProductGroupAsset) {
            ProductGroupAsset newAsset = (ProductGroupAsset) obj;
            return this.name.equals(newAsset.name) && this.path.equals(newAsset.path)
                    && this.format.equals(newAsset.format);
        }
        return false;
    }
}
