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

import org.esa.s1tbx.commons.io.JSONUtils;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.*;


public class TestProductGroupMetadataFile {

    @Test
    public void testWrite() throws Exception {
        final ProductGroupMetadataFile metadataFile = new ProductGroupMetadataFile();

        metadataFile.addAsset(new ProductGroupAsset(
                    "name", "path_to_file", "BEAM-DIMAP"));

        final File file = new File(Files.createTempDirectory("productgroups").toFile(),
                ProductGroupMetadataFile.PRODUCT_GROUP_METADATA_FILE);
        metadataFile.write("productName", "productType", file);
        assertTrue(file.exists());

        final JSONObject json = (JSONObject)JSONUtils.loadJSONFile(file);
        assertEquals("productName", json.get(ProductGroupMetadataFile.ID));
        assertEquals("productType", json.get(ProductGroupMetadataFile.PRODUCT_TYPE));
        assertEquals(ProductGroupMetadataFile.LATEST_VERSION, json.get(ProductGroupMetadataFile.PRODUCT_GROUP_VERSION));

        final JSONObject assets = (JSONObject)json.get(ProductGroupMetadataFile.ASSETS);
        assertEquals(1, assets.size());
        JSONObject asset1 = (JSONObject)assets.get("name");
        assertNotNull(asset1);

        assertTrue(asset1.containsKey(ProductGroupAsset.UPDATED_DATE));
        assertEquals(1L, asset1.get(ProductGroupAsset.ASSET_INDEX));
        assertEquals("BEAM-DIMAP", asset1.get(ProductGroupAsset.FORMAT));
        assertEquals("path_to_file", asset1.get(ProductGroupAsset.HREF));

        file.delete();
    }
}
