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
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.*;
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
        // create stack product with 4 initial bands
        final int numBands = 4;
        Product product = createStackProduct(numBands);

        final File targetFolder = createTmpFolder("productgroups/group1");
        File metadataFile = ProductGroupIO.operatorWrite(product, targetFolder, "BEAM-DIMAP", ProgressMonitor.NULL);

        assertEquals(numBands, product.getNumBands());
        assertTrue(metadataFile.exists());

        ProductGroupMetadataFile productGroupMetadataFile = new ProductGroupMetadataFile();
        productGroupMetadataFile.read(metadataFile);
        assertEquals(2, productGroupMetadataFile.getAssets().length);
        assertTrue(areSameUpdateDate(productGroupMetadataFile.getAssets()));

        Product productGroup = ProductIO.readProduct(metadataFile);
        assertEquals(numBands, productGroup.getNumBands());

        productGroup.dispose();
        product.dispose();
        //assertTrue(FileUtils.deleteTree(targetFolder));
    }

    @Test
    public void testWrite_with_writerPlugIn() throws Exception {
        // create stack product with 4 initial bands
        final int numBands = 4;
        Product product = createStackProduct(numBands);

        final File targetFolder = createTmpFolder("productgroups/group2");
        ProductIO.writeProduct(product, targetFolder, "ProductGroup", false, ProgressMonitor.NULL);

        File metadataFile = new File(targetFolder, ProductGroupMetadataFile.PRODUCT_GROUP_METADATA_FILE);
        Product productGroup = ProductIO.readProduct(metadataFile);
        assertEquals(numBands, productGroup.getNumBands());

        productGroup.dispose();
        product.dispose();
        //assertTrue(FileUtils.deleteTree(targetFolder));
    }

    @Test
    public void testWrite_Append() throws Exception {
        // create stack product with 4 initial bands
        final int numBands = 4;
        Product product = createStackProduct(numBands);

        final File targetFolder = createTmpFolder("productgroups/group3");
        File metadataFile = ProductGroupIO.operatorWrite(product, targetFolder, "BEAM-DIMAP", ProgressMonitor.NULL);

        ProductGroupMetadataFile productGroupMetadataFile = new ProductGroupMetadataFile();
        productGroupMetadataFile.read(metadataFile);
        assertEquals(2, productGroupMetadataFile.getAssets().length);
        assertTrue(areSameUpdateDate(productGroupMetadataFile.getAssets()));

        // add new band
        String date = "_01Mar21";
        TestUtils.createBand(product, "band_slv" + 4 + date, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        addStackMetadata(product, "product3"+date);
        assertEquals(numBands + 1, product.getNumBands());

        // rewrite only new band
        ProductGroupIO.operatorWrite(product, targetFolder, "BEAM-DIMAP", ProgressMonitor.NULL);

        productGroupMetadataFile = new ProductGroupMetadataFile();
        productGroupMetadataFile.read(metadataFile);
        assertEquals(3, productGroupMetadataFile.getAssets().length);
        assertFalse(areSameUpdateDate(productGroupMetadataFile.getAssets()));

        // read product group
        Product productGroup = ProductIO.readProduct(metadataFile);
        assertEquals(numBands + 1, productGroup.getNumBands());

        productGroup.dispose();
        product.dispose();
        //assertTrue(FileUtils.deleteTree(targetFolder));
    }

    @Test
    public void testWrite_Update_Processing_History() throws Exception {
        // create stack product with 4 initial bands
        final int numBands = 4;
        Product product = createStackProduct(numBands);

        final File targetFolder = createTmpFolder("productgroups/group4");
        File metadataFile = ProductGroupIO.operatorWrite(product, targetFolder, "BEAM-DIMAP", ProgressMonitor.NULL);

        ProductGroupMetadataFile productGroupMetadataFile = new ProductGroupMetadataFile();
        productGroupMetadataFile.read(metadataFile);
        assertEquals(2, productGroupMetadataFile.getAssets().length);
        assertTrue(areSameUpdateDate(productGroupMetadataFile.getAssets()));

        String date = "_01Mar21";
        TestUtils.createBand(product, "band_slv" + 4 + date, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        addStackMetadata(product, "product3"+date);

        ProductGroupIO.operatorWrite(product, targetFolder, "BEAM-DIMAP", ProgressMonitor.NULL);

        productGroupMetadataFile = new ProductGroupMetadataFile();
        productGroupMetadataFile.read(metadataFile);
        assertEquals(2, productGroupMetadataFile.getAssets().length);
        assertFalse(areSameUpdateDate(productGroupMetadataFile.getAssets()));

        PassThroughOp passThroughOp = new PassThroughOp();
        passThroughOp.setSourceProduct(product);
        Product processedProduct = passThroughOp.getTargetProduct();

        ProductGroupIO.operatorWrite(processedProduct, targetFolder, "BEAM-DIMAP", ProgressMonitor.NULL);

        processedProduct.dispose();

        product.dispose();
        //assertTrue(FileUtils.deleteTree(targetFolder));
    }


    private boolean areSameUpdateDate(final ProductGroupAsset[] assets) {
        ProductData.UTC updatedUTC = assets[0].getUpdatedUTC();
        for(ProductGroupAsset asset : assets) {
            if(!updatedUTC.equals(asset.getUpdatedUTC()))
                return false;
        }
        return true;
    }

    private Product createStackProduct(final int numBands) {
        final int w = 10000, h = 10000;
        final Product product = TestUtils.createProduct("type", w, h);

        String date = "_01Jan21";
        TestUtils.createBand(product, "band_mst" + 1 + date, w, h);
        Band intensityBand = SARReader.createVirtualIntensityBand(product, product.getBandAt(0), "");
        intensityBand.setName(intensityBand.getName() + "_mst" + 1 + date);

        date = "_01Feb21";
        for(int i=2; i < numBands; ++i) {
            TestUtils.createBand(product, "band_slv" + i + date, w, h);
        }
        addStackMetadata(product, "product2"+date);

        return product;
    }

    private void addStackMetadata(final Product product, final String secondaryProductName) {
        final MetadataElement root = product.getMetadataRoot();
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        absRoot.setAttributeInt(AbstractMetadata.coregistered_stack, 1);

        final MetadataElement slv1Meta = absRoot.createDeepClone();
        slv1Meta.setName(secondaryProductName);
        slv1Meta.setAttributeString(AbstractMetadata.PRODUCT, secondaryProductName);

        MetadataElement slaveMetadata;
        if(root.containsElement("Slave_Metadata")) {
            slaveMetadata = root.getElement("Slave_Metadata");
        } else {
            slaveMetadata = new MetadataElement("Slave_Metadata");
            root.addElement(slaveMetadata);
        }
        slaveMetadata.addElement(slv1Meta);
    }
}
