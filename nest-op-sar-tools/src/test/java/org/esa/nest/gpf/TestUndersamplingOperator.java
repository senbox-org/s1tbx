/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.util.TestUtils;

import java.util.Arrays;

/**
 * Unit test for UndersamplingOperator.
 */
public class TestUndersamplingOperator extends TestCase {

    private OperatorSpi spi;

    @Override
    protected void setUp() throws Exception {
        spi = new UndersamplingOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(spi);
    }

    /**
     * Tests sub-sampling method in undersampling operator with a 6x12 "DETECTED" test product.
     * @throws Exception general exception
     */
    public void testUndersamplingWithSubSampling() throws Exception {

        Product sourceProduct = createTestProduct(12, 6);
        
        UndersamplingOp op = (UndersamplingOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        op.setUndersamplingMethod(op.SUB_SAMPLING);
        op.setSubSamplingRate(3, 3);

        // get targetProduct: execute initialize()
        Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);
        
        Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels: execute computeTiles()
        float[] floatValues = new float[8];
        band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs:
        float[] expectedValues = {1.0f, 4.0f, 7.0f, 10.0f, 37.0f, 40.0f, 43.0f, 46.0f};
        assertTrue(Arrays.equals(expectedValues, floatValues));

        // compare updated metadata
        MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);
        TestUtils.attributeEquals(abs, AbstractMetadata.azimuth_spacing, 4.5);
        TestUtils.attributeEquals(abs, AbstractMetadata.range_spacing, 6.0);
        TestUtils.attributeEquals(abs, AbstractMetadata.line_time_interval, 0.03);

        TestUtils.attributeEquals(abs, AbstractMetadata.first_line_time, "10-MAY-2008 20:30:46.890683");
    }

    /**
     * Tests low pass kernel filtering in undersampling operator with a 6x12 "DETECTED" test product.
     * @throws Exception general exception
     */
    public void testUndersamplingWithLowPassKernel() throws Exception {

        Product sourceProduct = createTestProduct(12, 6);

        UndersamplingOp op = (UndersamplingOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        op.setUndersamplingMethod(op.KERNEL_FILTERING);
        op.setFilterType(op.LOW_PASS);
        op.setFilterSize(op.FILTER_SIZE_3x3);
        op.setOutputImageBy(op.IMAGE_SIZE);
        op.setOutputImageSize(2, 4);

        // get targetProduct: execute initialize()
        Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);

        Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels: execute computeTiles()
        float[] floatValues = new float[8];
        band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs:
        float[] expectedValues = {14.0f, 17.0f, 20.0f, 23.0f, 50.0f, 53.0f, 56.0f, 59.0f};
        assertTrue(Arrays.equals(expectedValues, floatValues));

        // compare updated metadata
        MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);
        TestUtils.attributeEquals(abs, AbstractMetadata.azimuth_spacing, 4.5);
        TestUtils.attributeEquals(abs, AbstractMetadata.range_spacing, 6.0);
        TestUtils.attributeEquals(abs, AbstractMetadata.line_time_interval, 0.03);
        TestUtils.attributeEquals(abs, AbstractMetadata.first_line_time, "10-MAY-2008 20:30:46.900682");
    }

    /**
     * Tests high pass kernel filtering in undersampling operator with a 6x12 "DETECTED" test product.
     * @throws Exception general exception
     */
  /*  public void testUndersamplingWithHighPassKernel() throws Exception {

        Product sourceProduct = createTestProduct(12, 6);

        UndersamplingOp op = (UndersamplingOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        op.setUndersamplingMethod(op.KERNEL_FILTERING);
        op.setFilterType(op.HIGH_PASS);
        op.setFilterSize(op.FILTER_SIZE_3x3);
        op.setOutputImageBy(op.IMAGE_SIZE);
        op.setOutputImageSize(2, 4);

        // get targetProduct: execute initialize()
        Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);

        Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels: execute computeTiles()
        float[] floatValues = new float[8];
        band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs:
        float[] expectedValues = {1.5555555f, 1.8888888f, 2.222222f, 2.5555553f, 5.5555553f, 5.8888884f, 6.222222f, 6.5555553f};
        assertTrue(Arrays.equals(expectedValues, floatValues));

        // compare updated metadata
        MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);
        TestUtils.attributeEquals(abs, AbstractMetadata.azimuth_spacing, 4.5);
        TestUtils.attributeEquals(abs, AbstractMetadata.range_spacing, 6.0);
        TestUtils.attributeEquals(abs, AbstractMetadata.line_time_interval, 0.03);
        TestUtils.attributeEquals(abs, AbstractMetadata.first_line_time, "10-MAY-2008 20:30:46.900682");
    }     */

    /**
     * Tests edge detect kernel filtering in undersampling operator with a 6x12 "DETECTED" test product.
     * @throws Exception general exception
     */
 /*   public void testUndersamplingWithEdgeDetectKernel() throws Exception {

        Product sourceProduct = createTestProduct(12, 6);

        UndersamplingOp op = (UndersamplingOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        op.setUndersamplingMethod(op.KERNEL_FILTERING);
        op.setFilterType(op.EDGE_DETECT);
        op.setFilterSize(op.FILTER_SIZE_3x3);
        op.setOutputImageBy(op.IMAGE_SIZE);
        op.setOutputImageSize(2, 4);

        // get targetProduct: execute initialize()
        Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);

        Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels: execute computeTiles()
        float[] floatValues = new float[8];
        band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs:
        float[] expectedValues = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        assertTrue(Arrays.equals(expectedValues, floatValues));

        // compare updated metadata
        MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);
        TestUtils.attributeEquals(abs, AbstractMetadata.azimuth_spacing, 4.5);
        TestUtils.attributeEquals(abs, AbstractMetadata.range_spacing, 6.0);
        TestUtils.attributeEquals(abs, AbstractMetadata.line_time_interval, 0.03);
        TestUtils.attributeEquals(abs, AbstractMetadata.first_line_time, "10-MAY-2008 20:30:46.900682");
    }         */

    /**
     * Tests edge enhance kernel filtering in undersampling operator with a 6x12 "DETECTED" test product.
     * @throws Exception general exception
     */
  /*  public void testUndersamplingWithEdgeEnhanceKernel() throws Exception {

        Product sourceProduct = createTestProduct(12, 6);

        UndersamplingOp op = (UndersamplingOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        op.setUndersamplingMethod(op.KERNEL_FILTERING);
        op.setFilterType(op.EDGE_ENHANCEMENT);
        op.setFilterSize(op.FILTER_SIZE_3x3);
        op.setOutputImageBy(op.IMAGE_SIZE);
        op.setOutputImageSize(2, 4);

        // get targetProduct: execute initialize()
        Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);

        Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels: execute computeTiles()
        float[] floatValues = new float[8];
        band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs:
        float[] expectedValues = {13.999999f, 16.999998f, 19.999998f, 22.999998f, 49.999996f, 52.999996f, 55.999996f, 58.999996f};
        assertTrue(Arrays.equals(expectedValues, floatValues));

        // compare updated metadata
        MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);
        TestUtils.attributeEquals(abs, AbstractMetadata.azimuth_spacing, 4.5);
        TestUtils.attributeEquals(abs, AbstractMetadata.range_spacing, 6.0);
        TestUtils.attributeEquals(abs, AbstractMetadata.line_time_interval, 0.03);
        TestUtils.attributeEquals(abs, AbstractMetadata.first_line_time, "10-MAY-2008 20:30:46.900682");
    }          */

    /**
     * Tests horizontal kernel filtering in undersampling operator with a 6x12 "DETECTED" test product.
     * @throws Exception general exception
     */
    public void testUndersamplingWithHorizontalKernel() throws Exception {

        Product sourceProduct = createTestProduct(12, 6);

        UndersamplingOp op = (UndersamplingOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        op.setUndersamplingMethod(op.KERNEL_FILTERING);
        op.setFilterType(op.HORIZONTAL);
        op.setFilterSize(op.FILTER_SIZE_3x3);
        op.setOutputImageBy(op.IMAGE_SIZE);
        op.setOutputImageSize(2, 4);

        // get targetProduct: execute initialize()
        Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);

        Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels: execute computeTiles()
        float[] floatValues = new float[8];
        band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs:
        float[] expectedValues = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        assertTrue(Arrays.equals(expectedValues, floatValues));

        // compare updated metadata
        MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);
        TestUtils.attributeEquals(abs, AbstractMetadata.azimuth_spacing, 4.5);
        TestUtils.attributeEquals(abs, AbstractMetadata.range_spacing, 6.0);
        TestUtils.attributeEquals(abs, AbstractMetadata.line_time_interval, 0.03);
        TestUtils.attributeEquals(abs, AbstractMetadata.first_line_time, "10-MAY-2008 20:30:46.900682");
    }

    /**
     * Tests vertical kernel filtering in undersampling operator with a 6x12 "DETECTED" test product.
     * @throws Exception general exception
     */
    public void testUndersamplingWithVerticalKernel() throws Exception {

        final Product sourceProduct = createTestProduct(12, 6);

        final UndersamplingOp op = (UndersamplingOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        op.setUndersamplingMethod(op.KERNEL_FILTERING);
        op.setFilterType(op.VERTICAL);
        op.setFilterSize(op.FILTER_SIZE_3x3);
        op.setOutputImageBy(op.IMAGE_SIZE);
        op.setOutputImageSize(2, 4);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);

        final Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels: execute computeTiles()
        final float[] floatValues = new float[8];
        band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs:
        final float[] expectedValues = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        assertTrue(Arrays.equals(expectedValues, floatValues));

        // compare updated metadata
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);
        TestUtils.attributeEquals(abs, AbstractMetadata.azimuth_spacing, 4.5);
        TestUtils.attributeEquals(abs, AbstractMetadata.range_spacing, 6.0);
        TestUtils.attributeEquals(abs, AbstractMetadata.line_time_interval, 0.03);
        TestUtils.attributeEquals(abs, AbstractMetadata.first_line_time, "10-MAY-2008 20:30:46.900682");
    }

    /**
     * Tests summary kernel filtering in undersampling operator with a 6x12 "DETECTED" test product.
     * @throws Exception general exception
     */
    public void testUndersamplingWithSummaryKernel() throws Exception {

        Product sourceProduct = createTestProduct(12, 6);

        UndersamplingOp op = (UndersamplingOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        op.setUndersamplingMethod(op.KERNEL_FILTERING);
        op.setFilterType(op.SUMMARY);
        op.setFilterSize(op.FILTER_SIZE_3x3);
        op.setOutputImageBy(op.IMAGE_SIZE);
        op.setOutputImageSize(2, 4);

        // get targetProduct: execute initialize()
        Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);

        Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels: execute computeTiles()
        float[] floatValues = new float[8];
        band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs:
        float[] expectedValues = {3.1111116f, 3.7777786f, 4.4444456f, 5.111112f, 11.111114f, 11.777781f, 12.444447f, 13.111114f};
        assertTrue(Arrays.equals(expectedValues, floatValues));

        // compare updated metadata
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);
        TestUtils.attributeEquals(abs, AbstractMetadata.azimuth_spacing, 4.5);
        TestUtils.attributeEquals(abs, AbstractMetadata.range_spacing, 6.0);
        TestUtils.attributeEquals(abs, AbstractMetadata.line_time_interval, 0.03);
        TestUtils.attributeEquals(abs, AbstractMetadata.first_line_time, "10-MAY-2008 20:30:46.900682");
    }


    /**
     * Creates a 6-by-12 test product as shown below:
     *  1  2  3  4  5  6  7  8  9 10 11 12
     * 13 14 15 16 17 18 19 20 21 22 23 24
     * 25 26 27 28 29 30 31 32 33 34 35 36
     * 37 38 39 40 41 42 43 44 45 46 47 48
     * 49 50 51 52 53 54 55 56 57 58 59 60
     * 61 62 63 64 65 66 67 68 69 70 71 72
     * @param w width
     * @param h height
     * @return the created product
     */
    private static Product createTestProduct(int w, int h) {

        Product testProduct = TestUtils.createProduct("ASA_APG_1P", w, h);

        // create a Band: band1
        Band band1 = testProduct.addBand("band1", ProductData.TYPE_INT32);
        band1.setUnit(Unit.AMPLITUDE);
        int[] intValues = new int[w * h];
        for (int i = 0; i < w * h; i++) {
            intValues[i] = i + 1;
        }
        band1.setData(ProductData.createInstance(intValues));

        // create abstracted metadata
        MetadataElement abs = AbstractMetadata.getAbstractedMetadata(testProduct);

        AbstractMetadata.setAttribute(abs, AbstractMetadata.range_spacing, 2.0F);
        AbstractMetadata.setAttribute(abs, AbstractMetadata.azimuth_spacing, 1.5F);
        AbstractMetadata.setAttribute(abs, AbstractMetadata.line_time_interval, 0.01F);
        AbstractMetadata.setAttribute(abs, AbstractMetadata.first_line_time,
                AbstractMetadata.parseUTC("10-MAY-2008 20:30:46.890683"));
        
        return testProduct;
    }

}
