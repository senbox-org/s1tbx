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
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.s1tbx.io.productgroup.support.ProductGroupAsset;
import org.esa.s1tbx.io.productgroup.support.ProductGroupIO;
import org.esa.s1tbx.io.productgroup.support.ProductGroupMetadataFile;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.common.PassThroughOp;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static org.esa.snap.core.util.Debug.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestProductGroupWriter extends ProcessorTest {

    @Test
    public void testWrite() throws Exception {
        Product product = createStackProduct();
        addMetadata(product);

        final File targetFolder = createTmpFolder("productgroups/group1");
        File metadataFile = ProductGroupIO.operatorWrite(product, targetFolder, "BEAM-DIMAP", ProgressMonitor.NULL);

        assertEquals(4, product.getNumBands());
        assertTrue(metadataFile.exists());

        ProductGroupMetadataFile productGroupMetadataFile = new ProductGroupMetadataFile();
        productGroupMetadataFile.read(metadataFile);
        assertEquals(3, productGroupMetadataFile.getAssets().length);
        assertTrue(areSameUpdateDate(productGroupMetadataFile.getAssets()));

        product.dispose();
        assertTrue(FileUtils.deleteTree(targetFolder));
    }

    @Test
    public void testWrite_Append() throws Exception {
        Product product = createStackProduct();
        addMetadata(product);

        final File targetFolder = createTmpFolder("productgroups/group2");
        File metadataFile = ProductGroupIO.operatorWrite(product, targetFolder, "BEAM-DIMAP", ProgressMonitor.NULL);

        ProductGroupMetadataFile productGroupMetadataFile = new ProductGroupMetadataFile();
        productGroupMetadataFile.read(metadataFile);
        assertEquals(3, productGroupMetadataFile.getAssets().length);
        assertTrue(areSameUpdateDate(productGroupMetadataFile.getAssets()));

        TestUtils.createBand(product, "band" + 4, product.getSceneRasterWidth(), product.getSceneRasterHeight());

        ProductGroupIO.operatorWrite(product, targetFolder, "BEAM-DIMAP", ProgressMonitor.NULL);

        productGroupMetadataFile = new ProductGroupMetadataFile();
        productGroupMetadataFile.read(metadataFile);
        assertEquals(4, productGroupMetadataFile.getAssets().length);
        assertFalse(areSameUpdateDate(productGroupMetadataFile.getAssets()));

        product.dispose();
        //assertTrue(FileUtils.deleteTree(targetFolder));
    }

    @Test
    public void testWrite_Update_Processing_History() throws Exception {
        Product product = createStackProduct();
        addMetadata(product);

        final File targetFolder = createTmpFolder("productgroups/group2");
        File metadataFile = ProductGroupIO.operatorWrite(product, targetFolder, "BEAM-DIMAP", ProgressMonitor.NULL);

        ProductGroupMetadataFile productGroupMetadataFile = new ProductGroupMetadataFile();
        productGroupMetadataFile.read(metadataFile);
        assertEquals(3, productGroupMetadataFile.getAssets().length);
        assertTrue(areSameUpdateDate(productGroupMetadataFile.getAssets()));

        TestUtils.createBand(product, "band" + 4, product.getSceneRasterWidth(), product.getSceneRasterHeight());

        ProductGroupIO.operatorWrite(product, targetFolder, "BEAM-DIMAP", ProgressMonitor.NULL);

        productGroupMetadataFile = new ProductGroupMetadataFile();
        productGroupMetadataFile.read(metadataFile);
        assertEquals(4, productGroupMetadataFile.getAssets().length);
        assertFalse(areSameUpdateDate(productGroupMetadataFile.getAssets()));

        PassThroughOp passThroughOp = new PassThroughOp();
        passThroughOp.setSourceProduct(product);
        Product processedProduct = passThroughOp.getTargetProduct();

        ProductGroupIO.operatorWrite(processedProduct, targetFolder, "BEAM-DIMAP", ProgressMonitor.NULL);

        processedProduct.dispose();

        product.dispose();
        assertTrue(FileUtils.deleteTree(targetFolder));
    }


    private boolean areSameUpdateDate(final ProductGroupAsset[] assets) {
        ProductData.UTC updatedUTC = assets[0].getUpdatedUTC();
        for(ProductGroupAsset asset : assets) {
            if(!updatedUTC.equals(asset.getUpdatedUTC()))
                return false;
        }
        return true;
    }

    private Product createStackProduct() {
        final int w = 10, h = 10;
        final Product product = TestUtils.createProduct("type", w, h);

        for(int i=1; i < 4; ++i) {
            TestUtils.createBand(product, "band" + i, w, h);
        }
        SARReader.createVirtualIntensityBand(product, product.getBandAt(0), "");

        return product;
    }

    private void addMetadata(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        MetadataAttribute attribute = AbstractMetadata.addAbstractedAttribute(absRoot, AbstractMetadata.collocated_stack, ProductData.TYPE_INT8, "", "");
        attribute.getData().setElemInt(1);

    }
}
