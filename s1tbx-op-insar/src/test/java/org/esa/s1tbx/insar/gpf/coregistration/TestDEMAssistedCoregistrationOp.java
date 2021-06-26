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
package org.esa.s1tbx.insar.gpf.coregistration;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test for DEMAssistedCoregistrationOp.
 */
public class TestDEMAssistedCoregistrationOp extends ProcessorTest {

    private final static OperatorSpi spi = new DEMAssistedCoregistrationOp.Spi();

    @Test
    public void testDEMAssistedCoregistration() throws Exception {

        final DEMAssistedCoregistrationOp op = (DEMAssistedCoregistrationOp) spi.createOperator();
        assertNotNull(op);

        int refW = 30, refH = 30;
        final Product refProduct = createTestProduct(refW, refH);
        final Product secProduct1 = createTestProduct(refW+10, refH+10);

        op.setSourceProducts(refProduct, secProduct1);
        op.setParameter("resamplingType", ResamplingFactory.BICUBIC_INTERPOLATION_NAME);

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();
        assertNotNull(targetProduct);
        assertEquals("getSceneRasterWidth", 30, targetProduct.getSceneRasterWidth());
        assertEquals("getSceneRasterHeight", 30, targetProduct.getSceneRasterHeight());

        final Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels gets computeTiles to be executed
        float[] pixels = new float[refW*refH];
        band.readPixels(0, 0, refW, refH, pixels, ProgressMonitor.NULL);

        assertEquals("pixels[0]", 1.5f, pixels[0], 0.0001f);
        assertEquals("pixels[10]", 11.5f, pixels[10], 0.0001f);
        assertEquals("pixels[100]", 101.5f, pixels[100], 0.0001f);
    }

    private static Product createTestProduct(final int w, final int h) {

        Product product = TestUtils.createProduct("ASA_IMP_1P", w, h);
        MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        absRoot.setAttributeDouble(AbstractMetadata.radar_frequency, 0.05623564240544047);
        absRoot.setAttributeDouble(AbstractMetadata.first_line_time, 1551.8375983581714);
        absRoot.setAttributeDouble(AbstractMetadata.last_line_time, 1551.8376221729052);
        absRoot.setAttributeDouble(AbstractMetadata.line_time_interval, 7.004336006944444E-9);
        absRoot.setAttributeDouble(AbstractMetadata.slant_range_to_first_pixel, 841839.6713948989);

        absRoot.setAttributeDouble(AbstractMetadata.range_spacing, 7.80397367);
        absRoot.setAttributeDouble(AbstractMetadata.azimuth_spacing, 4.04733086);

        TestUtils.createBand(product, "amplitude", ProductData.TYPE_FLOAT32, Unit.AMPLITUDE, w, h, true);
        return product;
    }
}
