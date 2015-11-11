/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.GlobalTestConfig;
import org.esa.snap.GlobalTestTools;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.dataio.dimap.DimapProductWriter;
import org.esa.snap.core.dataio.dimap.DimapProductWriterPlugIn;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class BandTest extends AbstractRasterDataNodeTest {

    private Band _rsBand;
    private Band _rsBandBlaByte5050;
    private Band _rsBandBlubbUShort1010;
    private Band _rsBandTestShort2020;
    private Band _rsBandGnmpfInt1515;
    private Band _rsBandBlimFloat2323;
    private Band _rsBandZippFloat1005;
    private Band _rsBandBlepDouble100100;

    @Override
    protected void setUp() {
        _rsBand = new Band("band1", ProductData.TYPE_INT8, 20, 20);
        _rsBandBlaByte5050 = new Band("Bla", ProductData.TYPE_UINT8, 50, 50);
        _rsBandBlubbUShort1010 = new Band("Blubb", ProductData.TYPE_UINT16, 10, 10);
        _rsBandTestShort2020 = new Band("Test", ProductData.TYPE_INT16, 20, 20);
        _rsBandGnmpfInt1515 = new Band("Gnmpf", ProductData.TYPE_INT32, 15, 15);
        _rsBandBlimFloat2323 = new Band("Blim", ProductData.TYPE_FLOAT32, 23, 23);
        _rsBandZippFloat1005 = new Band("Zipp", ProductData.TYPE_FLOAT32, 10, 5);
        _rsBandBlepDouble100100 = new Band("Blep", ProductData.TYPE_FLOAT64, 100, 100);
    }

    @Override
    protected void tearDown() {
    }

    @Override
    protected RasterDataNode createRasterDataNode() {
        return new Band("Undef", ProductData.TYPE_INT8, 10, 10);
    }

    /**
     * Tests the various expected constructor failures
     */
    public void testBandConstructor() {
        try {
            new Band("Undef", ProductData.TYPE_UNDEFINED, 24, 54);
            fail("A band with datatype ProductData.TYPE_UNDEFINED should not be constructable");
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException expected
        }

        try {
            new Band("Undef", 17, 24, 54);
            fail("A band with datatype 17 should not be constructable");
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException expected
        }

        try {
            new Band(null, 17, 24, 54);
            fail("A band with null pointer as name should not be constructable");
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException expected
        }

        try {
            new Band("ab/cd", 1, 1, 1);   // slash in NameString not allowed
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException expected
        }
    }

    /**
     * Tests the correct datatypes to be returned
     */
    public void testGetDataType() {
        assertEquals(ProductData.TYPE_UINT8, _rsBandBlaByte5050.getDataType());
        assertEquals(ProductData.TYPE_UINT16, _rsBandBlubbUShort1010.getDataType());
        assertEquals(ProductData.TYPE_INT16, _rsBandTestShort2020.getDataType());
        assertEquals(ProductData.TYPE_INT32, _rsBandGnmpfInt1515.getDataType());
        assertEquals(ProductData.TYPE_FLOAT32, _rsBandBlimFloat2323.getDataType());
        assertEquals(ProductData.TYPE_FLOAT32, _rsBandZippFloat1005.getDataType());
        assertEquals(ProductData.TYPE_FLOAT64, _rsBandBlepDouble100100.getDataType());
    }

    /**
     * GuiTest_DialogAndModalDialog correct functionality for getBandOutputRasterWidth()
     */
    public void testGetWidth() {
        assertEquals(50, _rsBandBlaByte5050.getRasterWidth());
        assertEquals(10, _rsBandBlubbUShort1010.getRasterWidth());
        assertEquals(20, _rsBandTestShort2020.getRasterWidth());
        assertEquals(15, _rsBandGnmpfInt1515.getRasterWidth());
        assertEquals(23, _rsBandBlimFloat2323.getRasterWidth());
        assertEquals(10, _rsBandZippFloat1005.getRasterWidth());
        assertEquals(100, _rsBandBlepDouble100100.getRasterWidth());
    }

    /**
     * Tests the functionality for getBandOutputRasterHeight()
     */
    public void testGetHeight() {
        assertEquals(50, _rsBandBlaByte5050.getRasterHeight());
        assertEquals(10, _rsBandBlubbUShort1010.getRasterHeight());
        assertEquals(20, _rsBandTestShort2020.getRasterHeight());
        assertEquals(15, _rsBandGnmpfInt1515.getRasterHeight());
        assertEquals(23, _rsBandBlimFloat2323.getRasterHeight());
        assertEquals(5, _rsBandZippFloat1005.getRasterHeight());
        assertEquals(100, _rsBandBlepDouble100100.getRasterHeight());
    }

    /**
     * Tests the getRaster functionality.
     */
    public void testGetRaster() {
        // nothing to test here because either a raster or a null
        // are valid returns
    }

    /**
     * Tests the functionality for getWriteableRaster()
     */
    public void testGetWriteableRaster() {
        // nothing to test here because either a raster or a null
        // are valid returns
    }

    /**
     * GuiTest_DialogAndModalDialog the functionality for createColorIndexedImage
     */
    public void testCreateBufferedImage() {
        // nothing to test here because either a raster or a null
        // are valid returns
    }

    public void testAcceptVisitor() {
        LinkedListProductVisitor visitor = new LinkedListProductVisitor();
        List expectedList = new LinkedList();
        assertEquals(expectedList, visitor.getVisitedList());

        _rsBand.acceptVisitor(visitor);

        expectedList.add("band1");
        assertEquals(expectedList, visitor.getVisitedList());

        try {
            _rsBand.acceptVisitor(null);
            fail("acceptVisitor shall not accept null pointer");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests base class functionality setDescription()
     */
    public void testSetDescription() {
        testSetDescription(_rsBandBlaByte5050);
        testSetDescription(_rsBandBlubbUShort1010);
        testSetDescription(_rsBandTestShort2020);
        testSetDescription(_rsBandGnmpfInt1515);
        testSetDescription(_rsBandBlimFloat2323);
        testSetDescription(_rsBandBlepDouble100100);
    }

    /**
     * Tests base class functionality setUnit()
     */
    public void testSetUnit() {
        testSetUnit(_rsBandBlaByte5050);
        testSetUnit(_rsBandBlubbUShort1010);
        testSetUnit(_rsBandTestShort2020);
        testSetUnit(_rsBandGnmpfInt1515);
        testSetUnit(_rsBandBlimFloat2323);
        testSetUnit(_rsBandBlepDouble100100);
    }

    public void testDataFromLevelZeroImage() {
        int[] testData = new int[1024 * 1024];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = i;
        }
        ProductData rasterData = ProductData.createInstance(testData);
        final Band band = new Band("band1", ProductData.TYPE_INT32, 1024, 1024);
        band.setRasterData(rasterData);
        final MultiLevelImage sourceImage = band.getSourceImage();
        assertEquals(4, sourceImage.getModel().getLevelCount());

        final RenderedImage image = sourceImage.getImage(2);
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());

        final Raster dataL2 = image.getData();
        final Raster dataL0 = sourceImage.getImage(0).getData();

        assertEquals(2050, dataL0.getSample(2, 2, 0)); // (0,0) is (2,2) at level zero
        assertEquals(2050, dataL2.getSample(0, 0, 0)); // (0,0) is (2,2) at level zero

        assertEquals(3070, dataL0.getSample(1022, 2, 0));
        assertEquals(3070, dataL2.getSample(255, 0, 0));

        assertEquals(1047550, dataL0.getSample(1022, 1022, 0));
        assertEquals(1047550, dataL2.getSample(255, 255, 0));
    }

    public void testGetPixel() {

        final float feps = 1e-6F;
        final double deps = 1e-12;

        float[] testDataFloat = new float[]{
                10, 20, 30, 40, 50, 60, 70, 80, 90, 100,
                11, 21, 31, 41, 51, 61, 71, 81, 91, 101,
                12, 22, 32, 42, 52, 62, 72, 82, 92, 102,
                13, 23, 33, 43, 53, 63, 73, 83, 93, 103,
                14, 24, 34, 44, 54, 64, 74, 84, 94, 104
        };
        ProductData rasterDataFloat = ProductData.createInstance(ProductData.TYPE_FLOAT32, 10 * 5);
        rasterDataFloat.setElems(testDataFloat);
        _rsBandZippFloat1005.setRasterData(rasterDataFloat);
        for (int i = 0; i < testDataFloat.length; i++) {
            int x = i % 10;
            int y = i / 10;
            assertEquals((int) testDataFloat[i], _rsBandZippFloat1005.getPixelInt(x, y));
            assertEquals(testDataFloat[i], _rsBandZippFloat1005.getPixelFloat(x, y), feps);
            assertEquals((double) testDataFloat[i], _rsBandZippFloat1005.getPixelDouble(x, y), deps);
        }

        int[] testDataInt = new int[]{
                15, 25, 35, 45, 55, 65, 75, 85, 95, 105,
                16, 26, 36, 46, 56, 66, 76, 86, 96, 106,
                17, 27, 37, 47, 57, 67, 77, 87, 97, 107,
                18, 28, 38, 48, 58, 68, 78, 88, 98, 108,
                19, 29, 39, 49, 59, 69, 79, 89, 99, 109
        };
        _rsBandZippFloat1005.setPixels(0, 0, 10, 5, testDataInt);
        for (int i = 0; i < testDataInt.length; i++) {
            int x = i % 10;
            int y = i / 10;
            assertEquals(testDataInt[i], _rsBandZippFloat1005.getPixelInt(x, y));
            assertEquals((float) testDataInt[i], _rsBandZippFloat1005.getPixelFloat(x, y), feps);
            assertEquals((double) testDataInt[i], _rsBandZippFloat1005.getPixelDouble(x, y), deps);
        }
    }

    public void testSolarFlux() {
        assertEquals(0.0F, _rsBand.getSolarFlux(), 1e-6F);
        _rsBand.setSolarFlux(1.1F);
        assertEquals(1.1F, _rsBand.getSolarFlux(), 1e-6F);
    }

    public void testWaveLength() {
        assertEquals(0.0F, _rsBand.getSpectralWavelength(), 1e-6F);
        _rsBand.setSpectralWavelength(1.2F);
        assertEquals(1.2F, _rsBand.getSpectralWavelength(), 1e-6F);
    }

    public void testBandwidth() {
        assertEquals(0.0F, _rsBand.getSpectralBandwidth(), 1e-6F);
        _rsBand.setSpectralBandwidth(1.3F);
        assertEquals(1.3F, _rsBand.getSpectralBandwidth(), 1e-6F);
    }

    public void testScalingInitialValues() {
        Band bandFloat = new Band("radiance_13", ProductData.TYPE_FLOAT32, 10, 10);
        assertEquals(1.0f, bandFloat.getScalingFactor(), 1e-10f);
        assertEquals(0.0f, bandFloat.getScalingOffset(), 1e-10f);
        assertEquals(false, bandFloat.isLog10Scaled());
    }

    public void testThatScalingFactorsApplyToPixelAccessors_Int16() {
        Band bandShort = new Band("radiance_13", ProductData.TYPE_INT16, 10, 10);

        assertNull(bandShort.getData());
        bandShort.ensureRasterData();
        final ProductData data = bandShort.getData();
        assertNotNull(data);

        bandShort.setPixelInt(0, 0, 3);
        assertEquals(3, data.getElemIntAt(0));
        assertEquals(3, bandShort.getPixelInt(0, 0));
        assertEquals(3, bandShort.getPixelFloat(0, 0), 1e-6f);
        assertEquals(3, bandShort.getPixelDouble(0, 0), 1e-6d);

        bandShort.setPixelFloat(0, 0, 5.1234f);
        assertEquals(5, data.getElemIntAt(0));
        assertEquals(5, bandShort.getPixelInt(0, 0));
        assertEquals(5, bandShort.getPixelFloat(0, 0), 1e-6f);
        assertEquals(5, bandShort.getPixelDouble(0, 0), 1e-6d);

        bandShort.setPixelDouble(0, 0, 0.00347);
        assertEquals(0, data.getElemIntAt(0));
        assertEquals(0, bandShort.getPixelInt(0, 0));
        assertEquals(0, bandShort.getPixelFloat(0, 0), 1e-6f);
        assertEquals(0, bandShort.getPixelDouble(0, 0), 1e-6d);

        bandShort.setScalingFactor(.3);

        bandShort.setPixelInt(0, 0, 3);
        assertEquals(10, data.getElemIntAt(0));
        assertEquals(3, bandShort.getPixelInt(0, 0));
        assertEquals(3, bandShort.getPixelFloat(0, 0), 1e-6f);
        assertEquals(3, bandShort.getPixelDouble(0, 0), 1e-6d);

        bandShort.setPixelFloat(0, 0, 5.1234f);
        assertEquals(17, data.getElemIntAt(0));
        assertEquals(5, bandShort.getPixelInt(0, 0));
        assertEquals(5.1, bandShort.getPixelFloat(0, 0), 1e-6f);
        assertEquals(5.1, bandShort.getPixelDouble(0, 0), 1e-6d);

        bandShort.setPixelDouble(0, 0, 7.50347);
        assertEquals(25, data.getElemIntAt(0));
        assertEquals(8, bandShort.getPixelInt(0, 0));
        assertEquals(7.5, bandShort.getPixelFloat(0, 0), 1e-6f);
        assertEquals(7.5, bandShort.getPixelDouble(0, 0), 1e-6d);

        bandShort.setScalingOffset(2);

        bandShort.setPixelInt(0, 0, 10);
        assertEquals(27, data.getElemIntAt(0));
        assertEquals(10, bandShort.getPixelInt(0, 0));
        assertEquals(10.1, bandShort.getPixelFloat(0, 0), 1e-6f);
        assertEquals(10.1, bandShort.getPixelDouble(0, 0), 1e-6d);

        bandShort.setLog10Scaled(true);

        bandShort.setPixelInt(0, 0, 1250);
        assertEquals(4, data.getElemIntAt(0));
        assertEquals(1585, bandShort.getPixelInt(0, 0));
        assertEquals(1584.8932f, bandShort.getPixelFloat(0, 0), 1e-5f);
        assertEquals(1584.8932, bandShort.getPixelDouble(0, 0), 1e-5d);
    }

    public void testThatScalingFactorsApplyToPixelAccessors_Float32() {
        Band bandFloat = new Band("radiance_13", ProductData.TYPE_FLOAT32, 10, 10);

        assertNull(bandFloat.getData());
        bandFloat.ensureRasterData();
        final ProductData data = bandFloat.getData();
        assertNotNull(data);

        bandFloat.setPixelInt(0, 0, 3);
        assertEquals(3, data.getElemFloatAt(0), 1e-10f);
        assertEquals(3, bandFloat.getPixelInt(0, 0));
        assertEquals(3, bandFloat.getPixelFloat(0, 0), 1e-6f);
        assertEquals(3, bandFloat.getPixelDouble(0, 0), 1e-6d);

        bandFloat.setPixelFloat(0, 0, 5.1234f);
        assertEquals(5.1234f, data.getElemFloatAt(0), 1e-10f);
        assertEquals(5, bandFloat.getPixelInt(0, 0));
        assertEquals(5.1234, bandFloat.getPixelFloat(0, 0), 1e-6f);
        assertEquals(5.1234, bandFloat.getPixelDouble(0, 0), 1e-6d);

        bandFloat.setPixelDouble(0, 0, 0.00347f);
        assertEquals(0.00347f, data.getElemFloatAt(0), 1e-10f);
        assertEquals(0, data.getElemIntAt(0));
        assertEquals(0, bandFloat.getPixelInt(0, 0));
        assertEquals(0.00347, bandFloat.getPixelFloat(0, 0), 1e-6f);
        assertEquals(0.00347, bandFloat.getPixelDouble(0, 0), 1e-6d);

        bandFloat.setScalingFactor(.3);

        bandFloat.setPixelInt(0, 0, 3);
        assertEquals(10.0f, data.getElemFloatAt(0), 1e-10f);
        assertEquals(3, bandFloat.getPixelInt(0, 0));
        assertEquals(3, bandFloat.getPixelFloat(0, 0), 1e-6f);
        assertEquals(3, bandFloat.getPixelDouble(0, 0), 1e-6d);

        bandFloat.setPixelFloat(0, 0, 5.2f);
        assertEquals(17.33333, data.getElemFloatAt(0), 1e-5f);
        assertEquals(5, bandFloat.getPixelInt(0, 0));
        assertEquals(5.2, bandFloat.getPixelFloat(0, 0), 1e-6f);
        assertEquals(5.2, bandFloat.getPixelDouble(0, 0), 1e-6d);

        bandFloat.setPixelDouble(0, 0, 7.5);
        assertEquals(25.0, data.getElemFloatAt(0), 1e-5f);
        assertEquals(8, bandFloat.getPixelInt(0, 0));
        assertEquals(7.5, bandFloat.getPixelFloat(0, 0), 1e-6f);
        assertEquals(7.5, bandFloat.getPixelDouble(0, 0), 1e-6d);

        bandFloat.setScalingOffset(2.0);

        bandFloat.setPixelInt(0, 0, 79);
        assertEquals(256.66666f, data.getElemFloatAt(0), 1e-5f);
        assertEquals(79, bandFloat.getPixelInt(0, 0));
        assertEquals(79, bandFloat.getPixelFloat(0, 0), 1e-5f);
        assertEquals(79, bandFloat.getPixelDouble(0, 0), 1e-5d);

        bandFloat.setLog10Scaled(true);

        bandFloat.setPixelInt(0, 0, 79);
        assertEquals(-0.3412430286, data.getElemFloatAt(0), 1e-5f);
        assertEquals(79, bandFloat.getPixelInt(0, 0));
        assertEquals(79f, bandFloat.getPixelFloat(0, 0), 1e-5f);
        assertEquals(79d, bandFloat.getPixelDouble(0, 0), 1e-5d);
    }

    public void testSetAndGetPixels_UShort_Int() throws Exception {
        final Band band = new Band("radiance_4", ProductData.TYPE_UINT16, 3, 2);
        band.ensureRasterData();
        short[] testShortsRaw, trueShortsRaw;
        final ProductData data = band.getData();

        int[] testInts, trueInts;

        testInts = new int[]{1, 2, 3, 4, 5, 6};
        band.setPixels(0, 0, 3, 2, testInts);
        trueInts = band.getPixels(0, 0, 3, 2, (int[]) null, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(testInts, trueInts));
        testShortsRaw = new short[]{1, 2, 3, 4, 5, 6};
        trueShortsRaw = (short[]) data.getElems();
        assertTrue(Arrays.equals(testShortsRaw, trueShortsRaw));


        band.setScalingFactor(0.5);
        band.setScalingOffset(-13);

        testInts = new int[]{3, -5, 7, -9, 11, -13};
        band.setPixels(0, 0, 3, 2, testInts);
        trueInts = band.getPixels(0, 0, 3, 2, (int[]) null, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(testInts, trueInts));
        testShortsRaw = new short[]{32, 16, 40, 8, 48, 0};
        trueShortsRaw = (short[]) data.getElems();
        assertTrue(Arrays.equals(testShortsRaw, trueShortsRaw));
    }

    public void testSetAndGetPixels_UShort_Float() throws Exception {
        final Band band = new Band("radiance_4", ProductData.TYPE_UINT16, 3, 2);
        band.ensureRasterData();
        short[] testShortsRaw, trueShortsRaw;
        final ProductData data = band.getData();

        float[] testFloats, trueFloats;

        band.setScalingFactor(0.01);
        band.setScalingOffset(-10);

        testFloats = new float[]{1.1f, -2.2f, 3.3f, -4.4f, 5.5f, -6.6f};
        band.setPixels(0, 0, 3, 2, testFloats);
        trueFloats = band.getPixels(0, 0, 3, 2, (float[]) null, ProgressMonitor.NULL);
        for (int i = 0; i < testFloats.length; i++) {
            assertEquals(testFloats[i], trueFloats[i], 1e-6f);
        }
        testShortsRaw = new short[]{1110, 780, 1330, 560, 1550, 340};
        trueShortsRaw = (short[]) data.getElems();
        assertTrue(Arrays.equals(testShortsRaw, trueShortsRaw));
    }

    public void testSetAndGetPixels_UShort_Double() throws Exception {
        final Band band = new Band("radiance_4", ProductData.TYPE_UINT16, 3, 2);
        band.ensureRasterData();
        short[] testShortsRaw, trueShortsRaw;
        final ProductData data = band.getData();

        double[] testDoubles, trueDoubles;

        band.setScalingFactor(0.01);
        band.setScalingOffset(-10);

        testDoubles = new double[]{1.1, -2.2, 3.3, -4.4, 5.5, -6.6};
        band.setPixels(0, 0, 3, 2, testDoubles);
        trueDoubles = band.getPixels(0, 0, 3, 2, (double[]) null, ProgressMonitor.NULL);
        for (int i = 0; i < testDoubles.length; i++) {
            assertEquals(testDoubles[i], trueDoubles[i], 1e-6f);
        }
        testShortsRaw = new short[]{1110, 780, 1330, 560, 1550, 340};
        trueShortsRaw = (short[]) data.getElems();
        assertTrue(Arrays.equals(testShortsRaw, trueShortsRaw));
    }

    public final void testReadAndWritePixels() throws IOException {
        final int[] testInt8s = new int[]{3, -6, 9, -12, 15, -18};
        final int[] testInt16s = new int[]{11, -22, 33, -44, 55, -66};
        final int[] testInt32s = new int[]{111, -222, 333, -444, 555, -666};
        final int[] testUInt8s = new int[]{1, 2, 3, 4, 5, 6};
        final int[] testUInt16s = new int[]{1001, 2002, 3003, 4004, 5005, 6006};
        final int[] testUInt32s = new int[]{1111, 2222, 3333, 4444, 5555, 6666};
        final float[] testFloat32s = new float[]{1.001f, 2.002f, 3.003f, 4.004f, 5.005f, 6.006f};
        final double[] testFloat64s = new double[]{-15d, 200d, -30000d, 4440d, -5550d, -600000d};

        int[] trueInts = new int[6];
        float[] trueFloats = new float[6];
        double[] trueDoubles = new double[6];
        float[] trueScaledFloats = new float[6];
        float[] testScaledFloats;
        double[] trueScaledDoubles = new double[6];
        double[] testScaledDoubles;

        String name = "x";
        Product product = new Product(name, "NO_TYPE", 3, 2);

        Band bandInt8 = new Band("bandInt8", ProductData.TYPE_INT8, 3, 2);
        bandInt8.ensureRasterData();
        bandInt8.setPixels(0, 0, 3, 2, testInt8s);
        product.addBand(bandInt8);

        Band bandInt16 = new Band("bandInt16", ProductData.TYPE_INT16, 3, 2);
        bandInt16.ensureRasterData();
        bandInt16.setPixels(0, 0, 3, 2, testInt16s);
        product.addBand(bandInt16);

        Band bandInt32 = new Band("bandInt32", ProductData.TYPE_INT32, 3, 2);
        bandInt32.ensureRasterData();
        bandInt32.setPixels(0, 0, 3, 2, testInt32s);
        product.addBand(bandInt32);

        Band bandUInt8 = new Band("bandUInt8", ProductData.TYPE_UINT8, 3, 2);
        bandUInt8.ensureRasterData();
        bandUInt8.setPixels(0, 0, 3, 2, testUInt8s);
        product.addBand(bandUInt8);

        Band bandUInt16 = new Band("bandUInt16", ProductData.TYPE_UINT16, 3, 2);
        bandUInt16.ensureRasterData();
        bandUInt16.setPixels(0, 0, 3, 2, testUInt16s);
        product.addBand(bandUInt16);

        Band bandUInt32 = new Band("bandUint32", ProductData.TYPE_UINT32, 3, 2);
        bandUInt32.ensureRasterData();
        bandUInt32.setPixels(0, 0, 3, 2, testUInt32s);
        product.addBand(bandUInt32);

        Band bandFloat32 = new Band("bandFloat32", ProductData.TYPE_FLOAT32, 3, 2);
        bandFloat32.ensureRasterData();
        bandFloat32.setPixels(0, 0, 3, 2, testFloat32s);
        product.addBand(bandFloat32);

        Band bandFloat64 = new Band("bandFloat64", ProductData.TYPE_FLOAT64, 3, 2);
        bandFloat64.ensureRasterData();
        bandFloat64.setPixels(0, 0, 3, 2, testFloat64s);
        product.addBand(bandFloat64);

        final File outputDirectory = GlobalTestConfig.getBeamTestDataOutputDirectory();
        final File file = new File(outputDirectory, name + DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION);
        ProductIO.writeProduct(product,
                               file,
                               DimapProductConstants.DIMAP_FORMAT_NAME,
                               false,
                               ProgressMonitor.NULL);

        product = ProductIO.readProduct(file);

        final DimapProductWriter dimapProductWriter = new DimapProductWriter(new DimapProductWriterPlugIn());
        assertNull(product.getProductWriter());
        product.setProductWriter(dimapProductWriter);
        assertNotNull(product.getProductWriter());
        dimapProductWriter.writeProductNodes(product, file);

        bandInt8 = product.getBand("bandInt8");
        bandInt8.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(testInt8s, trueInts));
        bandInt8.setScalingFactor(0.1);
        bandInt8.setScalingOffset(1.25);
        testScaledFloats = new float[]{1.55f, 0.65f, 2.15f, 0.05f, 2.75f, -0.55f};
        bandInt8.readPixels(0, 0, 3, 2, trueScaledFloats, ProgressMonitor.NULL);
        for (int i = 0; i < testScaledFloats.length; i++) {
            assertEquals(testScaledFloats[i], trueScaledFloats[i], 1e-6f);
        }

        bandInt8.setScalingFactor(2);
        bandInt8.setScalingOffset(2);
        bandInt8.writePixels(0, 0, 3, 2, new int[]{9, 8, 7, 6, 5, 4}, ProgressMonitor.NULL);
        bandInt8.setScalingFactor(1);
        bandInt8.setScalingOffset(0);
        bandInt8.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertEquals(true, Arrays.equals(new int[]{4, 3, 3, 2, 2, 1}, trueInts));
        bandInt8.setScalingFactor(2);
        bandInt8.setScalingOffset(2);
        bandInt8.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(new int[]{10, 8, 8, 6, 6, 4}, trueInts));

        bandInt16 = product.getBand("bandInt16");
        bandInt16.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertEquals(true, Arrays.equals(testInt16s, trueInts));
        bandInt16.setScalingFactor(0.1);
        bandInt16.setScalingOffset(1.25);
        testScaledFloats = new float[]{2.35f, -0.95f, 4.55f, -3.15f, 6.75f, -5.35f};
        bandInt16.readPixels(0, 0, 3, 2, trueScaledFloats, ProgressMonitor.NULL);
        for (int i = 0; i < testScaledFloats.length; i++) {
            assertEquals(testScaledFloats[i], trueScaledFloats[i], 1e-6f);
        }

        bandInt16.setScalingFactor(2);
        bandInt16.setScalingOffset(2);
        bandInt16.writePixels(0, 0, 3, 2, new int[]{9, 8, 7, 6, 5, 4}, ProgressMonitor.NULL);
        bandInt16.setScalingFactor(1);
        bandInt16.setScalingOffset(0);
        bandInt16.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(new int[]{4, 3, 3, 2, 2, 1}, trueInts));
        bandInt16.setScalingFactor(2);
        bandInt16.setScalingOffset(2);
        bandInt16.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(new int[]{10, 8, 8, 6, 6, 4}, trueInts));

        bandInt32 = product.getBand("bandInt32");
        bandInt32.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(testInt32s, trueInts));
        bandInt32.setScalingFactor(0.1);
        bandInt32.setScalingOffset(1.25);
        testScaledFloats = new float[]{12.35f, -20.95f, 34.55f, -43.15f, 56.75f, -65.35f};
        bandInt32.readPixels(0, 0, 3, 2, trueScaledFloats, ProgressMonitor.NULL);
        for (int i = 0; i < testScaledFloats.length; i++) {
            assertEquals(testScaledFloats[i], trueScaledFloats[i], 1e-6f);
        }

        bandInt32.setScalingFactor(2);
        bandInt32.setScalingOffset(2);
        bandInt32.writePixels(0, 0, 3, 2, new int[]{9, 8, 7, 6, 5, 4}, ProgressMonitor.NULL);
        bandInt32.setScalingFactor(1);
        bandInt32.setScalingOffset(0);
        bandInt32.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(new int[]{4, 3, 3, 2, 2, 1}, trueInts));
        bandInt32.setScalingFactor(2);
        bandInt32.setScalingOffset(2);
        bandInt32.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(new int[]{10, 8, 8, 6, 6, 4}, trueInts));

        bandUInt8 = product.getBand("bandUInt8");
        bandUInt8.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(testUInt8s, trueInts));
        bandUInt8.setScalingFactor(0.1);
        bandUInt8.setScalingOffset(1.25);
        testScaledFloats = new float[]{1.35f, 1.45f, 1.55f, 1.65f, 1.75f, 1.85f};
        bandUInt8.readPixels(0, 0, 3, 2, trueScaledFloats, ProgressMonitor.NULL);
        for (int i = 0; i < testScaledFloats.length; i++) {
            assertEquals(testScaledFloats[i], trueScaledFloats[i], 1e-6f);
        }

        bandUInt8.setScalingFactor(2);
        bandUInt8.setScalingOffset(2);
        bandUInt8.writePixels(0, 0, 3, 2, new int[]{9, 8, 7, 6, 5, 4}, ProgressMonitor.NULL);
        bandUInt8.setScalingFactor(1);
        bandUInt8.setScalingOffset(0);
        bandUInt8.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(new int[]{4, 3, 3, 2, 2, 1}, trueInts));
        bandUInt8.setScalingFactor(2);
        bandUInt8.setScalingOffset(2);
        bandUInt8.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(new int[]{10, 8, 8, 6, 6, 4}, trueInts));

        bandUInt16 = product.getBand("bandUInt16");
        bandUInt16.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(testUInt16s, trueInts));
        bandUInt16.setScalingFactor(0.1);
        bandUInt16.setScalingOffset(1.25);
        testScaledFloats = new float[]{101.35f, 201.45f, 301.55f, 401.65f, 501.75f, 601.85f};
        bandUInt16.readPixels(0, 0, 3, 2, trueScaledFloats, ProgressMonitor.NULL);
        for (int i = 0; i < testScaledFloats.length; i++) {
            assertEquals(testScaledFloats[i], trueScaledFloats[i], 1e-5f);
        }

        bandUInt16.setScalingFactor(2);
        bandUInt16.setScalingOffset(2);
        bandUInt16.writePixels(0, 0, 3, 2, new int[]{9, 8, 7, 6, 5, 4}, ProgressMonitor.NULL);
        bandUInt16.setScalingFactor(1);
        bandUInt16.setScalingOffset(0);
        bandUInt16.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(new int[]{4, 3, 3, 2, 2, 1}, trueInts));
        bandUInt16.setScalingFactor(2);
        bandUInt16.setScalingOffset(2);
        bandUInt16.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(new int[]{10, 8, 8, 6, 6, 4}, trueInts));

        bandUInt32 = product.getBand("bandUInt32");
        bandUInt32.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(testUInt32s, trueInts));
        bandUInt32.setScalingFactor(0.1);
        bandUInt32.setScalingOffset(1.25);
        testScaledFloats = new float[]{112.35f, 223.45f, 334.55f, 445.65f, 556.75f, 667.85f};
        bandUInt32.readPixels(0, 0, 3, 2, trueScaledFloats, ProgressMonitor.NULL);
        for (int i = 0; i < testScaledFloats.length; i++) {
            assertEquals(testScaledFloats[i], trueScaledFloats[i], 1e-6f);
        }

        bandUInt32.setScalingFactor(2);
        bandUInt32.setScalingOffset(2);
        bandUInt32.writePixels(0, 0, 3, 2, new int[]{9, 8, 7, 6, 5, 4}, ProgressMonitor.NULL);
        bandUInt32.setScalingFactor(1);
        bandUInt32.setScalingOffset(0);
        bandUInt32.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(new int[]{4, 3, 3, 2, 2, 1}, trueInts));
        bandUInt32.setScalingFactor(2);
        bandUInt32.setScalingOffset(2);
        bandUInt32.readPixels(0, 0, 3, 2, trueInts, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(new int[]{10, 8, 8, 6, 6, 4}, trueInts));

        bandFloat32 = product.getBand("bandFloat32");
        bandFloat32.readPixels(0, 0, 3, 2, trueFloats, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(testFloat32s, trueFloats));
        bandFloat32.setScalingFactor(0.1);
        bandFloat32.setScalingOffset(1.25);
        testScaledFloats = new float[]{1.3501f, 1.4502f, 1.5503f, 1.6504f, 1.7505f, 1.8506f};
        bandFloat32.readPixels(0, 0, 3, 2, trueScaledFloats, ProgressMonitor.NULL);
        for (int i = 0; i < testScaledFloats.length; i++) {
            assertEquals(testScaledFloats[i], trueScaledFloats[i], 1e-6f);
        }

        bandFloat32.setScalingFactor(0.2);
        bandFloat32.setScalingOffset(3);
        final float[] testFloats = new float[]{3.5f, 4.5f, 5.5f, 6.5f, 7.5f, 8.5f};
        bandFloat32.writePixels(0, 0, 3, 2, testFloats, ProgressMonitor.NULL);
        bandFloat32.setScalingFactor(1);
        bandFloat32.setScalingOffset(0);
        bandFloat32.readPixels(0, 0, 3, 2, trueFloats, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(new float[]{2.5f, 7.5f, 12.5f, 17.5f, 22.5f, 27.5f}, trueFloats));
        bandFloat32.setScalingFactor(0.2);
        bandFloat32.setScalingOffset(3);
        bandFloat32.readPixels(0, 0, 3, 2, trueFloats, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(testFloats, trueFloats));

        bandFloat64 = product.getBand("bandFloat64");
        bandFloat64.readPixels(0, 0, 3, 2, trueDoubles, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(testFloat64s, trueDoubles));
        bandFloat64.setScalingFactor(0.1);
        bandFloat64.setScalingOffset(1.25);
        testScaledDoubles = new double[]{-0.25d, 21.25d, -2998.75d, 445.25d, -553.75d, -59998.75d};
        bandFloat64.readPixels(0, 0, 3, 2, trueScaledDoubles, ProgressMonitor.NULL);
        for (int i = 0; i < testScaledDoubles.length; i++) {
            assertEquals(testScaledDoubles[i], trueScaledDoubles[i], 1e-6f);
        }

        bandFloat64.setScalingFactor(0.2);
        bandFloat64.setScalingOffset(3);
        final double[] testDoubles = new double[]{3.5, 4.5, 5.5, 6.5, 7.5, 8.5};
        bandFloat64.writePixels(0, 0, 3, 2, testDoubles, ProgressMonitor.NULL);
        bandFloat64.setScalingFactor(1);
        bandFloat64.setScalingOffset(0);
        bandFloat64.readPixels(0, 0, 3, 2, trueDoubles, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(new double[]{2.5, 7.5, 12.5, 17.5, 22.5, 27.5}, trueDoubles));
        bandFloat64.setScalingFactor(0.2);
        bandFloat64.setScalingOffset(3);
        bandFloat64.readPixels(0, 0, 3, 2, trueDoubles, ProgressMonitor.NULL);
        assertTrue(Arrays.equals(testDoubles, trueDoubles));

        product.closeProductReader();
        product.dispose();
        GlobalTestTools.deleteTestDataInputDirectory();
    }

    public final void testThatSetPixelsMethodsThrowISE() {
        Product product = new Product("X", "NO_TYPE", 3, 2);
        Band band = new Band("band", ProductData.TYPE_INT8, 3, 2);
        product.addBand(band);

        try {
            band.setPixelInt(0, 0, 0);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            band.setPixelFloat(0, 0, 0.0f);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            band.setPixelDouble(0, 0, 0.0);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            band.setPixels(0, 0, 1, 1, new int[]{0});
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            band.setPixels(0, 0, 1, 1, new float[]{0.0f});
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            band.setPixels(0, 0, 1, 1, new double[]{0.0});
            fail();
        } catch (NullPointerException expected) {
        }
    }

    public void testThatNullSourceImageCanBeSet() {
        Product p = new Product("p", "pt", 10, 10);

        Band b = p.addBand("b", ProductData.TYPE_UINT16);

        assertFalse(b.isSourceImageSet());
        assertNotNull(b.getSourceImage());// Don't wonder at that, the image created uses the (pretended) product reader

        b.setSourceImage(null);
        assertFalse(b.isSourceImageSet());
        assertNotNull(b.getSourceImage());// Don't wonder at that, the image created uses the (pretended) product reader

        BufferedImage sourceImage = new BufferedImage(10, 10, BufferedImage.TYPE_USHORT_GRAY);
        b.setSourceImage(sourceImage);
        assertTrue(b.isSourceImageSet());
        assertNotNull(b.getSourceImage());
        assertSame(sourceImage, b.getSourceImage().getImage(0));

        b.setSourceImage(null);
        assertFalse(b.isSourceImageSet());
        assertNotNull(b.getSourceImage());// Don't wonder at that, the image created uses the (pretended) product reader
    }


    public void testBandImageChangeNotifications() {
        PNL pnl = new PNL();
        Product p = new Product("p", "pt", 10, 10);

        Band b = p.addBand("b", ProductData.TYPE_UINT16);

        p.addProductNodeListener(pnl);

        assertFalse(b.isSourceImageSet());
        assertNotNull(b.getSourceImage());

        pnl.trace = "";
        b.setSourceImage(new BufferedImage(10, 10, BufferedImage.TYPE_USHORT_GRAY));
        assertEquals("sourceImage;", pnl.trace);

// @todo - make this work and add more tests (see http://www.brockmann-consult.de/wiki/display/BEAM/BEAM+Future) (nf-20090110)
//        pnl.trace = "";
//        b.setScalingFactor(2.0);
//        assertEquals("scalingFactor;geophysicalImage;", pnl.trace);
//
//        pnl.trace = "";
//        b.setNoDataValue(-999.0);
//        b.setNoDataValueUsed(true);
//        assertEquals("noDataValue;noDataValueUsed;geophysicalImage;dataMaskImage;", pnl.trace);
    }

    private static class PNL extends ProductNodeListenerAdapter {

        String trace = "";

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            trace += event.getPropertyName() + ";";
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            trace += event.getPropertyName() + ";";
        }
    }
}
