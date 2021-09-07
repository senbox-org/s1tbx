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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.s1tbx.io.productgroup.support.ProductGroupMetadataFile;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.esa.snap.core.util.Debug.assertTrue;
import static org.junit.Assert.*;


public class TestProductGroupReader extends ProcessorTest {

    private final static String resourcePath = "org/esa/s1tbx/io/productgroups/productgroup_1";
    private final static String tmpFolder = "productgroups-productgroup_1";

    @Before
    public void setup() throws IOException {
        Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(this.getClass()).resolve(resourcePath);
        Path destDirectory = createTmpFolder(tmpFolder).toPath();

        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, destDirectory);
        resourceInstaller.install(".*", ProgressMonitor.NULL);
    }

    @Test
    public void testRead() throws Exception {
        final File targetFolder = createTmpFolder(tmpFolder);
        final File file = new File(targetFolder, ProductGroupMetadataFile.PRODUCT_GROUP_METADATA_FILE);

        Product product = ProductIO.readProduct(file);
        assertNotNull(product);

        assertEquals(4, product.getNumBands());
        assertEquals("band1", product.getBandAt(0).getName());
        assertEquals("band2", product.getBandAt(1).getName());
        assertEquals("band3", product.getBandAt(2).getName());
        assertEquals("band4", product.getBandAt(3).getName());

        product.dispose();
        assertTrue(FileUtils.deleteTree(targetFolder));
    }
}
