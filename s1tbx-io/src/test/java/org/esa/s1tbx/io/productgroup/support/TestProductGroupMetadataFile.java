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

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.esa.snap.core.util.Debug.assertTrue;

public class TestProductGroupMetadataFile {

    @Test
    public void testWrite() throws Exception {
        final ProductGroupMetadataFile metadataFile = new ProductGroupMetadataFile();

        metadataFile.addAsset(new ProductGroupAsset(
                    "name", "path_to_file", "BEAM-DIMAP"));

        final File file = new File(Files.createTempDirectory("productgroups").toFile(), "product_group.json");
        metadataFile.write("productName", "productType", file);
        assertTrue(file.exists());

        file.delete();
    }
}
