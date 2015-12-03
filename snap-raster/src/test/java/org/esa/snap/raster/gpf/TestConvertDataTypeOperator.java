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
package org.esa.snap.raster.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for ConvertDatatypeOperator.
 */
public class TestConvertDataTypeOperator {

    private final static OperatorSpi spi = new ConvertDataTypeOp.Spi();
    private final static int width = 4;
    private final static int height = 2;

    /**
     * @throws Exception general exception
     */
    @Test
    public void testDoubleToInt32Linear() throws Exception {

        final double[] values = convert(ProductData.TYPESTRING_INT32, ConvertDataTypeOp.SCALING_LINEAR);

        final double[] expectedValues = {100000.0, 150000.0, 200000.0, 250000.0, 300000.0, 350000.0, 400000.0, 450000.0};
        assertTrue(Arrays.equals(expectedValues, values));
    }

    @Test
    public void testDoubleToInt16Linear() throws Exception {

        final double[] values = convert(ProductData.TYPESTRING_INT16, ConvertDataTypeOp.SCALING_LINEAR);

        final double[] expectedValues = {-32768.0, -23406.0, -14044.0, -4682.0, 4681.0, 14043.0, 23405.0, 32767.0};
        assertTrue(Arrays.equals(expectedValues, values));
    }

    @Test
    public void testDoubleToInt8Linear() throws Exception {

        final double[] values = convert(ProductData.TYPESTRING_INT8, ConvertDataTypeOp.SCALING_LINEAR);

        final double[] expectedValues = {-128.0, -92.0, -55.0, -19.0, 18.0, 54.0, 91.0, 127.0};
        assertTrue(Arrays.equals(expectedValues, values));
    }

    @Test
    public void testDoubleToUInt32Linear() throws Exception {

        final double[] values = convert(ProductData.TYPESTRING_UINT32, ConvertDataTypeOp.SCALING_TRUNCATE);

        final double[] expectedValues = {100000.0, 150000.0, 200000.0, 250000.0, 300000.0, 350000.0, 400000.0, 450000.0};
        assertTrue(Arrays.equals(expectedValues, values));
    }

    @Test
    public void testDoubleToUInt16Linear() throws Exception {

        final double[] values = convert(ProductData.TYPESTRING_UINT16, ConvertDataTypeOp.SCALING_LINEAR);

        final double[] expectedValues = {0.0, 9362.0, 18724.0, 28086.0, 37449.0, 46811.0, 56173.0, 65535.0};
        assertTrue(Arrays.equals(expectedValues, values));
    }

    @Test
    public void testDoubleToUInt8Linear() throws Exception {

        final double[] values = convert(ProductData.TYPESTRING_UINT8, ConvertDataTypeOp.SCALING_LINEAR);

        final double[] expectedValues = {0.0, 36.0, 73.0, 109.0, 146.0, 182.0, 219.0, 255.0};
        assertTrue(Arrays.equals(expectedValues, values));
    }

    @Test
    public void testDoubleToInt32Truncate() throws Exception {

        final double[] values = convert(ProductData.TYPESTRING_INT32, ConvertDataTypeOp.SCALING_TRUNCATE);

        final double[] expectedValues = {100000.0, 150000.0, 200000.0, 250000.0, 300000.0, 350000.0, 400000.0, 450000.0};
        assertTrue(Arrays.equals(expectedValues, values));
    }

    @Test
    public void testDoubleToInt16Truncate() throws Exception {

        final double[] values = convert(ProductData.TYPESTRING_INT16, ConvertDataTypeOp.SCALING_TRUNCATE);

        final double[] expectedValues = {32767.0, 32767.0, 32767.0, 32767.0, 32767.0, 32767.0, 32767.0, 32767.0};
        assertTrue(Arrays.equals(expectedValues, values));
    }

    @Test
    public void testDoubleToInt8Truncate() throws Exception {

        final double[] values = convert(ProductData.TYPESTRING_INT8, ConvertDataTypeOp.SCALING_TRUNCATE);

        final double[] expectedValues = {127.0, 127.0, 127.0, 127.0, 127.0, 127.0, 127.0, 127.0};
        assertTrue(Arrays.equals(expectedValues, values));
    }

    @Test
    public void testDoubleToInt32LogScale() throws Exception {

        final double[] values = convert(ProductData.TYPESTRING_INT32, ConvertDataTypeOp.SCALING_LOGARITHMIC);

        final double[] expectedValues = {100000.0, 150000.0, 200000.0, 250000.0, 300000.0, 350000.0, 400000.0, 450000.0};
        assertTrue(Arrays.equals(expectedValues, values));
    }

    @Test
    public void testDoubleToInt16LogScale() throws Exception {

        final double[] values = convert(ProductData.TYPESTRING_INT16, ConvertDataTypeOp.SCALING_LOGARITHMIC);

        final double[] expectedValues = {0.0, 0.0, 0.0, 0.0, 37.0, 41.0, 44.0, 45.0};
        assertTrue(Arrays.equals(expectedValues, values));
    }

    @Test
    public void testDoubleToInt8LogScale() throws Exception {

        final double[] values = convert(ProductData.TYPESTRING_INT8, ConvertDataTypeOp.SCALING_LOGARITHMIC);

        final double[] expectedValues = {0.0, 0.0, 0.0, 0.0, 12.0, 17.0, 20.0, 21.0};
        assertTrue(Arrays.equals(expectedValues, values));
    }

    private double[] convert(final String targetType, final String scaling) throws Exception {
        final Product sourceProduct = createTestProduct(width, height, 100000, 500000);

        final ConvertDataTypeOp op = (ConvertDataTypeOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.setTargetDataType(targetType);
        op.setScaling(scaling);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);

        final Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels: execute computeTiles()
        final double[] values = new double[width * height];
        band.readPixels(0, 0, width, height, values, ProgressMonitor.NULL);

        //outputValues(band.getName() + " op values:", values);

        return values;
    }

    private static void outputValues(final String title, final double[] values) {
        System.out.println(title);
        for (double v : values) {
            System.out.print(v + ", ");
        }
        System.out.println();
    }

    /**
     * Creates a new product
     *
     * @param w   width
     * @param h   height
     * @param min minimum value
     * @param max maximum value
     * @return the created product
     */
    private static Product createTestProduct(final int w, final int h, final int min, final int max) {

        final Product testProduct = TestUtils.createProduct("ASA_IMM_1P", w, h);

        // create a Band: band1
        final Band band1 = testProduct.addBand("band1", ProductData.TYPE_FLOAT64);
        band1.setUnit(Unit.AMPLITUDE);
        final int range = w * h;
        final double[] values = new double[range];
        final double slope = (max - min) / range;
        for (int i = 0; i < range; i++) {
            values[i] = i * slope + min;
        }
        band1.setData(ProductData.createInstance(values));

        //outputValues(band1.getName() + " values:", values);

        return testProduct;
    }
}
