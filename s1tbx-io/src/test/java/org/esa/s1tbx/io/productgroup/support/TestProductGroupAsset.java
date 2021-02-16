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

import org.json.simple.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

@SuppressWarnings("unchecked")
public class TestProductGroupAsset {

    @Test
    public void testCreateAsset() {
        ProductGroupAsset asset = new ProductGroupAsset("name", "path_to_file", "format");

        assertEquals("name", asset.getName());
        assertEquals("format", asset.getFormat());
        assertEquals("path_to_file", asset.getPath());
        assertEquals(-1, asset.getIndex());
        assertTrue(asset.isModified());
        assertNull(asset.getUpdatedUTC());
    }

    @Test
    public void testCreateAssetFromJson() throws Exception {
        JSONObject json = new JSONObject();
        json.put(ProductGroupAsset.HREF, "path_to_file");
        json.put(ProductGroupAsset.FORMAT, "format");
        json.put(ProductGroupAsset.ASSET_INDEX, 4);
        json.put(ProductGroupAsset.UPDATED_DATE, "16-FEB-2021 00:23:20.000000");

        ProductGroupAsset asset = ProductGroupAsset.createAsset("name", 3, json);

        assertEquals("name", asset.getName());
        assertEquals("format", asset.getFormat());
        assertEquals("path_to_file", asset.getPath());
        assertEquals(4, asset.getIndex());
        assertFalse(asset.isModified());
        assertNotNull(asset.getUpdatedUTC());
        assertEquals("16-FEB-2021 00:23:20.000000", asset.getUpdatedUTC().format());
    }

    @Test
    public void testWrite() {
        ProductGroupAsset asset = new ProductGroupAsset("name", "path_to_file", "format");

        JSONObject json = new JSONObject();
        asset.write(json);

        assertEquals("path_to_file", json.get(ProductGroupAsset.HREF));
        assertEquals("format", json.get(ProductGroupAsset.FORMAT));
        assertEquals(-1, json.get(ProductGroupAsset.ASSET_INDEX));
        assertNotNull(json.get(ProductGroupAsset.UPDATED_DATE));
    }
}
