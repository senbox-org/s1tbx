/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.dem.gpf.AddElevationOp;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.util.SystemUtils;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for MultilookOperator.
 */
public class TestCreateElevationOp {

    public final static String sep = File.separator;
    public final static String rootPathTestProducts = SystemUtils.getApplicationHomeDir()+sep+".."+sep+".."+sep+".."+sep+".."+sep+"testdata";
    public final static String inputSAR = rootPathTestProducts + sep + "input" + sep + "SAR" + sep;
    public final static File inputASAR_WSM = new File(inputSAR + "ASAR" + sep + "subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977.dim");

    private final static OperatorSpi spi = new AddElevationOp.Spi();

    private static double[] expectedValues = {
            1526.9168701171875,
            1527.808349609375,
            1536.52783203125,
            1548.83837890625,
            1525.9425048828125,
            1520.9635009765625,
            1530.3455810546875,
            1552.3145751953125,
    };

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    public void testProcessing() throws Exception {
        final File inputFile =  inputASAR_WSM;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final AddElevationOp op = (AddElevationOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true, true);

        final Band elevBand = targetProduct.getBand("elevation");
        assertNotNull(elevBand);

        final double[] demValues = new double[8];
        elevBand.readPixels(0, 0, 4, 2, demValues, ProgressMonitor.NULL);

        assertTrue(Arrays.equals(expectedValues, demValues));

        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);
        //TestUtils.attributeEquals(abs, AbstractMetadata.DEM, "SRTM");
    }
}
