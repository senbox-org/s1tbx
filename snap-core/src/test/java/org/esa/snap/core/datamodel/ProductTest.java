/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.snap.core.dataop.maptransf.MapInfo;
import org.esa.snap.core.dataop.maptransf.MapProjection;
import org.esa.snap.core.dataop.maptransf.MapTransform;
import org.esa.snap.core.util.ProductUtilsTest;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;


public class ProductTest {

    private static final String _prodType = "TestProduct";
    private static final int _sceneWidth = 20;
    private static final int _sceneHeight = 30;

    private Product product;

    @Before
    public void setUp() throws Exception {
        product = new Product("product", _prodType, _sceneWidth, _sceneHeight);
        product.setModified(false);
    }

    @Test
    public void testAcceptVisitor() {
        LinkedListProductVisitor visitor = new LinkedListProductVisitor();
        product.acceptVisitor(visitor);
        List<String> visitedList = visitor.getVisitedList();

        assertEquals(true, visitedList.contains("bands"));
        assertEquals(true, visitedList.contains("tie_point_grids"));
        assertEquals(true, visitedList.contains("masks"));
        assertEquals(true, visitedList.contains("index_codings"));
        assertEquals(true, visitedList.contains("flag_codings"));
        assertEquals(true, visitedList.contains("pins"));
        assertEquals(true, visitedList.contains("ground_control_points"));
        assertEquals(true, visitedList.contains("vector_data"));

        try {
            product.acceptVisitor(null);
            fail("Null argument for visitor not allowed");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testAddProductNodeListener() {
        ProductNodeListener listener = new DummyProductNodeListener();
        assertEquals("addProductNodeListener null", false, product.addProductNodeListener(null));
        assertEquals("addProductNodeListener", true, product.addProductNodeListener(listener));
        assertEquals("addProductNodeListener contained listener", false, product.addProductNodeListener(listener));
    }

    @Test
    public void testSetAndGetReader() {
        Product product = new Product("name", "MER_RR__1P", 312, 213);

        assertNull(product.getProductReader());

        DummyProductReader reader1 = new DummyProductReader(new DummyProductReaderPlugIn());
        product.setProductReader(reader1);
        assertSame(reader1, product.getProductReader());

        DummyProductReader reader2 = new DummyProductReader(new DummyProductReaderPlugIn());
        product.setProductReader(reader2);
        assertSame(reader2, product.getProductReader());

        try {
            product.setProductReader(null);
            fail("IllegalArgumentException expected since the parameter is null");
        } catch (IllegalArgumentException ignored) {
            //IllegalArgumentException expected since the parameter is null
        }
    }

    @Test
    public void testAddBandWithBandParameters() {
        assertEquals(0, product.getNumBands());
        assertEquals(0, product.getBandNames().length);
        assertNull(product.getBand("band1"));
        assertEquals(false, product.containsBand("band1"));

        product.addBand(new Band("band1", ProductData.TYPE_FLOAT32, _sceneWidth, _sceneHeight));

        assertEquals(1, product.getNumBands());
        assertEquals("band1", product.getBandNames()[0]);
        assertEquals(true, product.containsBand("band1"));
        assertNotNull(product.getBandAt(0));
        assertEquals("band1", product.getBandAt(0).getName());
        assertNotNull(product.getBand("band1"));
        assertEquals("band1", product.getBand("band1").getName());
    }

    @Test
    public void addMask() {
        final Mask mask = product.addMask("maskName", "sin(X)", "description", Color.BLUE, 0.5);
        assertNotNull(mask);
        assertEquals("maskName", mask.getName());
        assertEquals("sin(X)", Mask.BandMathsType.getExpression(mask));
        assertEquals("description", mask.getDescription());
        assertEquals(Color.BLUE, mask.getImageColor());
        assertEquals(0.5, mask.getImageTransparency(), 1e-12);
    }

    @Test
    public void addMask_FailsWithExpressionInvolvingBandsOfDifferentSizes() {
        product.addBand(new Band("band1", ProductData.TYPE_INT8, _sceneWidth + 5, _sceneHeight + 5));
        product.addBand(new Band("band2", ProductData.TYPE_INT8, _sceneWidth + 10, _sceneHeight + 10));
        try {
            product.addMask("mask", "band1 + band2", "description", Color.BLUE, 0.5);
            fail("Exception expected");
        } catch (Exception e) {
            assertEquals("Expression must not reference rasters of different sizes", e.getMessage());
        }
    }

    @Test
    public void testGetType() {
        Product prod;

        prod = new Product("TestName", "TEST", _sceneWidth, _sceneHeight);
        assertEquals("TEST", prod.getProductType());

        prod = new Product("TestName", "TEST", _sceneWidth, _sceneHeight, null);
        assertEquals("TEST", prod.getProductType());
    }

    @Test
    public void testGetSceneRasterWidth() {
        Product prod;

        prod = new Product("TestName", _prodType, 243, _sceneHeight);
        assertEquals(243, prod.getSceneRasterWidth());

        prod = new Product("TestName", _prodType, 789, _sceneHeight, null);
        assertEquals(789, prod.getSceneRasterWidth());
    }

    @Test
    public void testGetSceneRasterHeight() {
        Product prod;

        prod = new Product("TestName", _prodType, _sceneWidth, 373);
        assertEquals(373, prod.getSceneRasterHeight());

        prod = new Product("TestName", _prodType, _sceneWidth, 427, null);
        assertEquals(427, prod.getSceneRasterHeight());
    }

    @Test
    public void testBitmaskHandlingByte() {
        Product product = new Product("Y", "X", 4, 4);

        Band band = product.addBand("flags", ProductData.TYPE_INT8);
        final byte F1 = 0x01;
        final byte F2 = 0x02;
        final byte F3 = 0x04;

        FlagCoding flagCoding = new FlagCoding("flags");
        flagCoding.addFlag("F1", F1, null);
        flagCoding.addFlag("F2", F2, null);
        flagCoding.addFlag("F3", F3, null);

        product.getFlagCodingGroup().add(flagCoding);
        band.setSampleCoding(flagCoding);

        band.ensureRasterData();
        final byte[] elems = new byte[]{
                0, F1, F2, F3,
                F1, 0, F1 + F2, F1 + F3,
                F2, F1 + F2, 0, F2 + F3,
                F3, F1 + F3, F2 + F3, 0,
        };
        band.getRasterData().setElems(elems);
        product.setModified(false);

        final int[] F1_MASK = new int[]{
                0, 255, 0, 0,
                255, 0, 255, 255,
                0, 255, 0, 0,
                0, 255, 0, 0
        };
        testBitmaskHandling(product, "flags.F1", F1_MASK);

        final int[] F1_AND_F2_MASK = new int[]{
                0, 0, 0, 0,
                0, 0, 255, 0,
                0, 255, 0, 0,
                0, 0, 0, 0
        };
        testBitmaskHandling(product, "flags.F1 AND flags.F2", F1_AND_F2_MASK);

        final int[] F1_AND_F2_OR_F3_MASK = new int[]{
                0, 0, 0, 255,
                0, 0, 255, 255,
                0, 255, 0, 255,
                255, 255, 255, 0
        };
        testBitmaskHandling(product, "(flags.F1 AND flags.F2) OR flags.F3", F1_AND_F2_OR_F3_MASK);
    }

    @Test
    public void testBitmaskHandlingUShort() {
        Product product = new Product("Y", "X", 4, 4);

        Band band = new Band("flags", ProductData.TYPE_UINT16, 4, 4);
        product.addBand(band);

        final byte F1 = 0x0001;
        final byte F2 = 0x0002;
        final byte F3 = 0x0004;

        FlagCoding flagCoding = new FlagCoding("flags");
        flagCoding.addFlag("F1", F1, null);
        flagCoding.addFlag("F2", F2, null);
        flagCoding.addFlag("F3", F3, null);

        product.getFlagCodingGroup().add(flagCoding);
        band.setSampleCoding(flagCoding);

        ProductData data = band.createCompatibleRasterData();
        final short[] elems = new short[]{
                0, F1, F2, F3,
                F1, 0, F1 + F2, F1 + F3,
                F2, F1 + F2, 0, F2 + F3,
                F3, F1 + F3, F2 + F3, 0
        };
        data.setElems(elems);
        band.setRasterData(data);
        product.setModified(false);

        final int[] F1_MASK = new int[]{
                0, 255, 0, 0,
                255, 0, 255, 255,
                0, 255, 0, 0,
                0, 255, 0, 0
        };
        testBitmaskHandling(product, "flags.F1", F1_MASK);

        final int[] F1_AND_F2_MASK = new int[]{
                0, 0, 0, 0,
                0, 0, 255, 0,
                0, 255, 0, 0,
                0, 0, 0, 0
        };
        testBitmaskHandling(product, "flags.F1 AND flags.F2", F1_AND_F2_MASK);

        final int[] F1_AND_F2_OR_F3_MASK = new int[]{
                0, 0, 0, 255,
                0, 0, 255, 255,
                0, 255, 0, 255,
                255, 255, 255, 0
        };
        testBitmaskHandling(product, "(flags.F1 AND flags.F2) OR flags.F3", F1_AND_F2_OR_F3_MASK);
    }

    private void testBitmaskHandling(Product product, String expr, int[] expected) {
        testBitmaskHandlingFullRaster(product, expr, expected);
        testBitmaskHandlingLineWise(product, expr, expected);
    }

    private static void testBitmaskHandlingFullRaster(Product product, String expr, int[] expected) {

        int[] res = new int[4 * 4];
        RenderedImage maskImage = product.getMaskImage(expr, null).getImage(0);
        maskImage.getData(new Rectangle(0, 0, 4, 4)).getSamples(0, 0, 4, 4, 0, res);
        
        assertArrayEquals(expected, res);
    }

    private void testBitmaskHandlingLineWise(Product product, String expr, int[] expected) {

        int[] res = new int[4 * 4];
        readBitmaskLineWise(product, expr, res);
        
        assertArrayEquals(expected, res);
    }

    private static void readBitmaskLineWise(Product product, String expr, int[] res) {
        int[] line = new int[4];
        RenderedImage maskImage = product.getMaskImage(expr, null).getImage(0);
        for (int y = 0; y < 4; y++) {
            maskImage.getData(new Rectangle(0, y, 4, 1)).getSamples(0, y, 4, 1, 0, line);
            System.arraycopy(line, 0, res, y * 4, 4);
        }
    }

    @Test
    public void testGetAndSetRefNo() {
        assertEquals(0, product.getRefNo());

        try {
            product.setRefNo(0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {
            // expectet if value out of range
        } catch (IllegalStateException ignored) {
            fail("IllegalStateException not expected");
        }

        try {
            product.setRefNo(14);
        } catch (IllegalArgumentException ignored) {
            fail("IllegalArgumentException not expected");
        } catch (IllegalStateException ignored) {
            fail("IllegalStateException not expected");
        }
        assertEquals(14, product.getRefNo());

        try {
            product.setRefNo(23);
            fail("IllegalStateException expected");
        } catch (IllegalArgumentException ignored) {
            fail("IllegalArgumentException not expected");
        } catch (IllegalStateException ignored) {
            // expected if the reference number was alredy set
        }

        // no exception expected when the reference number to be set is the same as the one already set
        try {
            product.setRefNo(14);
        } catch (IllegalArgumentException ignored) {
            fail("IllegalArgumentException not expected");
        } catch (IllegalStateException ignored) {
            fail("IllegalStateException not expected");
        }
    }

    @Test
    public void testGetAndSetFileLocationProperty() {
        final Product product = new Product("A", "B", 10, 10);
        final TracingProductNodeListener listener = new TracingProductNodeListener();
        product.addProductNodeListener(listener);

        assertEquals(null, product.getFileLocation());

        product.setFileLocation(new File("test1.nc"));
        assertEquals(new File("test1.nc"), product.getFileLocation());
        assertEquals("fileLocation", listener.events.get(0).getPropertyName());
        assertEquals(null, listener.events.get(0).getOldValue());
        assertEquals(new File("test1.nc"), listener.events.get(0).getNewValue());

        product.setFileLocation(new File("test2.nc"));
        assertEquals(new File("test2.nc"), product.getFileLocation());
        assertEquals(2, listener.events.size());
        assertEquals("fileLocation", listener.events.get(1).getPropertyName());
        assertEquals(new File("test1.nc"), listener.events.get(1).getOldValue());
        assertEquals(new File("test2.nc"), listener.events.get(1).getNewValue());

        product.setFileLocation(null);
        assertEquals(null, product.getFileLocation());
        assertEquals(3, listener.events.size());
        assertEquals("fileLocation", listener.events.get(2).getPropertyName());
        assertEquals(new File("test2.nc"), listener.events.get(2).getOldValue());
        assertEquals(null, listener.events.get(2).getNewValue());
    }

    @Test
    public void testGetAndSetBandAutoGroupingProperty() {
        final Product product = new Product("A", "B", 10, 10);
        final MyProductNodeListener listener = new MyProductNodeListener();
        product.addProductNodeListener(listener);
        listener.pname = "";
        final String uv = "u:v";
        product.setAutoGrouping(uv);
        final Product.AutoGrouping autoGrouping = product.getAutoGrouping();
        assertEquals(2, autoGrouping.size());
        assertEquals("u", autoGrouping.get(0)[0]);
        assertEquals("v", autoGrouping.get(1)[0]);
        assertEquals("autoGrouping", listener.pname);

        listener.pname = "";
        product.setAutoGrouping(uv);
        assertEquals("", listener.pname);

        listener.pname = "";
        product.setAutoGrouping("u:v");
        assertEquals("", listener.pname);

        listener.pname = "";
        product.setAutoGrouping("c");
        assertEquals("autoGrouping", listener.pname);

        listener.pname = "";
        product.setAutoGrouping("");
        assertEquals("autoGrouping", listener.pname);

        listener.pname = "";
        product.setAutoGrouping((Product.AutoGrouping) null);
        assertEquals("", listener.pname);
    }

    @Test
    public void testGetAndSetBandAutoGroupingOrder() {
        final Product product = new Product("A", "B", 10, 10);
        product.setAutoGrouping("L_1:L_1_err:L_2:L_2_err:L_10:L_10_err:L_11:L_11_err:L_21:L_21_err");
        final Product.AutoGrouping autoGrouping = product.getAutoGrouping();

        assertNotNull(autoGrouping);
        assertEquals(10, autoGrouping.size());

        assertArrayEquals(new String[]{"L_1"}, autoGrouping.get(0));
        assertArrayEquals(new String[]{"L_1_err"}, autoGrouping.get(1));
        assertArrayEquals(new String[]{"L_2"}, autoGrouping.get(2));
        assertArrayEquals(new String[]{"L_2_err"}, autoGrouping.get(3));
        assertArrayEquals(new String[]{"L_10"}, autoGrouping.get(4));
        assertArrayEquals(new String[]{"L_10_err"}, autoGrouping.get(5));
        assertArrayEquals(new String[]{"L_11"}, autoGrouping.get(6));
        assertArrayEquals(new String[]{"L_11_err"}, autoGrouping.get(7));
        assertArrayEquals(new String[]{"L_21"}, autoGrouping.get(8));
        assertArrayEquals(new String[]{"L_21_err"}, autoGrouping.get(9));

        assertEquals(0, autoGrouping.indexOf("L_1_CAM1"));
        assertEquals(0, autoGrouping.indexOf("L_1_CAM5"));
        assertEquals(1, autoGrouping.indexOf("L_1_err_CAM1"));
        assertEquals(1, autoGrouping.indexOf("L_1_err_CAM5"));
        assertEquals(2, autoGrouping.indexOf("L_2_CAM1"));
        assertEquals(2, autoGrouping.indexOf("L_2_CAM5"));
        assertEquals(3, autoGrouping.indexOf("L_2_err_CAM1"));
        assertEquals(3, autoGrouping.indexOf("L_2_err_CAM5"));
        assertEquals(4, autoGrouping.indexOf("L_10_CAM1"));
        assertEquals(4, autoGrouping.indexOf("L_10_CAM5"));
        assertEquals(5, autoGrouping.indexOf("L_10_err_CAM1"));
        assertEquals(5, autoGrouping.indexOf("L_10_err_CAM5"));
        assertEquals(6, autoGrouping.indexOf("L_11_CAM1"));
        assertEquals(6, autoGrouping.indexOf("L_11_CAM5"));
        assertEquals(7, autoGrouping.indexOf("L_11_err_CAM1"));
        assertEquals(7, autoGrouping.indexOf("L_11_err_CAM5"));
        assertEquals(8, autoGrouping.indexOf("L_21_CAM1"));
        assertEquals(8, autoGrouping.indexOf("L_21_CAM5"));
        assertEquals(9, autoGrouping.indexOf("L_21_err_CAM1"));
        assertEquals(9, autoGrouping.indexOf("L_21_err_CAM5"));
    }

    @Test
    public void testGetAndSetBandAutoGroupingSubGroups() {
        final Product product = new Product("A", "B", 10, 10);
        product.setAutoGrouping("L_1:L_1/err:L_2:L_2/err:L_10:L_10/err:L_11:L_11/err:L_21:L_21/err");
        final Product.AutoGrouping autoGrouping = product.getAutoGrouping();

        assertNotNull(autoGrouping);
        assertEquals(10, autoGrouping.size());

        assertArrayEquals(new String[]{"L_1"}, autoGrouping.get(0));
        assertArrayEquals(new String[]{"L_1", "err"}, autoGrouping.get(1));
        assertArrayEquals(new String[]{"L_2"}, autoGrouping.get(2));
        assertArrayEquals(new String[]{"L_2", "err"}, autoGrouping.get(3));
        assertArrayEquals(new String[]{"L_10"}, autoGrouping.get(4));
        assertArrayEquals(new String[]{"L_10", "err"}, autoGrouping.get(5));
        assertArrayEquals(new String[]{"L_11"}, autoGrouping.get(6));
        assertArrayEquals(new String[]{"L_11", "err"}, autoGrouping.get(7));
        assertArrayEquals(new String[]{"L_21"}, autoGrouping.get(8));
        assertArrayEquals(new String[]{"L_21", "err"}, autoGrouping.get(9));

        assertEquals(0, autoGrouping.indexOf("L_1_CAM1"));
        assertEquals(0, autoGrouping.indexOf("L_1_CAM5"));
        assertEquals(1, autoGrouping.indexOf("L_1_err_CAM1"));
        assertEquals(1, autoGrouping.indexOf("L_1_err_CAM5"));
        assertEquals(2, autoGrouping.indexOf("L_2_CAM1"));
        assertEquals(2, autoGrouping.indexOf("L_2_CAM5"));
        assertEquals(3, autoGrouping.indexOf("L_2_err_CAM1"));
        assertEquals(3, autoGrouping.indexOf("L_2_err_CAM5"));
        assertEquals(4, autoGrouping.indexOf("L_10_CAM1"));
        assertEquals(4, autoGrouping.indexOf("L_10_CAM5"));
        assertEquals(5, autoGrouping.indexOf("L_10_err_CAM1"));
        assertEquals(5, autoGrouping.indexOf("L_10_err_CAM5"));
        assertEquals(6, autoGrouping.indexOf("L_11_CAM1"));
        assertEquals(6, autoGrouping.indexOf("L_11_CAM5"));
        assertEquals(7, autoGrouping.indexOf("L_11_err_CAM1"));
        assertEquals(7, autoGrouping.indexOf("L_11_err_CAM5"));
        assertEquals(8, autoGrouping.indexOf("L_21_CAM1"));
        assertEquals(8, autoGrouping.indexOf("L_21_CAM5"));
        assertEquals(9, autoGrouping.indexOf("L_21_err_CAM1"));
        assertEquals(9, autoGrouping.indexOf("L_21_err_CAM5"));
    }

    @Test
    public void testGetAndSetBandAutoGroupingSubGroupsWithWildcards() {
        final Product product = new Product("A", "B", 10, 10);
        product.setAutoGrouping("L_1:L_1/*err*CAM*:L_2:L_2/*err*CAM*:L_10:L_10/*err*CAM*:L_11:L_11/*err*CAM*:L_21:L_21/*err*CAM*");
        final Product.AutoGrouping autoGrouping = product.getAutoGrouping();

        assertNotNull(autoGrouping);
        assertEquals(10, autoGrouping.size());

        assertArrayEquals(new String[]{"L_1"}, autoGrouping.get(0));
        assertArrayEquals(new String[]{"L_1", "*err*CAM*"}, autoGrouping.get(1));
        assertArrayEquals(new String[]{"L_2"}, autoGrouping.get(2));
        assertArrayEquals(new String[]{"L_2", "*err*CAM*"}, autoGrouping.get(3));
        assertArrayEquals(new String[]{"L_10"}, autoGrouping.get(4));
        assertArrayEquals(new String[]{"L_10", "*err*CAM*"}, autoGrouping.get(5));
        assertArrayEquals(new String[]{"L_11"}, autoGrouping.get(6));
        assertArrayEquals(new String[]{"L_11", "*err*CAM*"}, autoGrouping.get(7));
        assertArrayEquals(new String[]{"L_21"}, autoGrouping.get(8));
        assertArrayEquals(new String[]{"L_21", "*err*CAM*"}, autoGrouping.get(9));

        assertEquals(0, autoGrouping.indexOf("L_1_CAM1"));
        assertEquals(0, autoGrouping.indexOf("L_1_CAM5"));
        assertEquals(1, autoGrouping.indexOf("L_1_err_CAM1"));
        assertEquals(1, autoGrouping.indexOf("L_1_err_CAM5"));
        assertEquals(2, autoGrouping.indexOf("L_2_CAM1"));
        assertEquals(2, autoGrouping.indexOf("L_2_CAM5"));
        assertEquals(3, autoGrouping.indexOf("L_2_err_CAM1"));
        assertEquals(3, autoGrouping.indexOf("L_2_err_CAM5"));
        assertEquals(4, autoGrouping.indexOf("L_10_CAM1"));
        assertEquals(4, autoGrouping.indexOf("L_10_CAM5"));
        assertEquals(5, autoGrouping.indexOf("L_10_err_CAM1"));
        assertEquals(5, autoGrouping.indexOf("L_10_err_CAM5"));
        assertEquals(6, autoGrouping.indexOf("L_11_CAM1"));
        assertEquals(6, autoGrouping.indexOf("L_11_CAM5"));
        assertEquals(7, autoGrouping.indexOf("L_11_err_CAM1"));
        assertEquals(7, autoGrouping.indexOf("L_11_err_CAM5"));
        assertEquals(8, autoGrouping.indexOf("L_21_CAM1"));
        assertEquals(8, autoGrouping.indexOf("L_21_CAM5"));
        assertEquals(9, autoGrouping.indexOf("L_21_err_CAM1"));
        assertEquals(9, autoGrouping.indexOf("L_21_err_CAM5"));
    }

    @Test
    public void testGetAndSetBandAutoGroupingNestedSubGroupsWithWildcards() {
        final Product product = new Product("A", "B", 10, 10);
        product.setAutoGrouping("L_1:L_1/*err*CAM*:L_1/*err*CAM*/5:L_2:L_2/*err*CAM*:L_2/*err*CAM*/5");
        final Product.AutoGrouping autoGrouping = product.getAutoGrouping();

        assertNotNull(autoGrouping);
        assertEquals(6, autoGrouping.size());

        assertArrayEquals(new String[]{"L_1"}, autoGrouping.get(0));
        assertArrayEquals(new String[]{"L_1", "*err*CAM*"}, autoGrouping.get(1));
        assertArrayEquals(new String[]{"L_1", "*err*CAM*", "5"}, autoGrouping.get(2));
        assertArrayEquals(new String[]{"L_2"}, autoGrouping.get(3));
        assertArrayEquals(new String[]{"L_2", "*err*CAM*"}, autoGrouping.get(4));
        assertArrayEquals(new String[]{"L_2", "*err*CAM*", "5"}, autoGrouping.get(5));

        assertEquals(0, autoGrouping.indexOf("L_1_CAM1"));
        assertEquals(0, autoGrouping.indexOf("L_1_CAM5"));
        assertEquals(1, autoGrouping.indexOf("L_1_err_CAM1"));
        assertEquals(2, autoGrouping.indexOf("L_1_err_CAM5"));
        assertEquals(3, autoGrouping.indexOf("L_2_CAM1"));
        assertEquals(3, autoGrouping.indexOf("L_2_CAM5"));
        assertEquals(4, autoGrouping.indexOf("L_2_err_CAM1"));
        assertEquals(5, autoGrouping.indexOf("L_2_err_CAM5"));
    }

    @Test
    public void testSettingOfAutoGroupingWithEmptyStrings() {
        final Product product = new Product("A", "B", 10, 10);
        product.setAutoGrouping("L_1:L_1/*err*CAM*::L_2::L_2/*err*CAM*::L_2/*err*CAM*/");
        final Product.AutoGrouping autoGrouping = product.getAutoGrouping();

        assertNotNull(autoGrouping);
        assertEquals(5, autoGrouping.size());

        assertArrayEquals(new String[]{"L_1"}, autoGrouping.get(0));
        assertArrayEquals(new String[]{"L_1", "*err*CAM*"}, autoGrouping.get(1));
        assertArrayEquals(new String[]{"L_2"}, autoGrouping.get(2));
        assertArrayEquals(new String[]{"L_2", "*err*CAM*"}, autoGrouping.get(3));
        assertArrayEquals(new String[]{"L_2", "*err*CAM*"}, autoGrouping.get(4));

        assertEquals(0, autoGrouping.indexOf("L_1_CAM1"));
        assertEquals(0, autoGrouping.indexOf("L_1_CAM5"));
        assertEquals(1, autoGrouping.indexOf("L_1_err_CAM1"));
        assertEquals(1, autoGrouping.indexOf("L_1_err_CAM5"));
        assertEquals(2, autoGrouping.indexOf("L_2_CAM1"));
        assertEquals(2, autoGrouping.indexOf("L_2_CAM5"));
        assertEquals(3, autoGrouping.indexOf("L_2_err_CAM1"));
        assertEquals(3, autoGrouping.indexOf("L_2_err_CAM5"));
    }

    @Test
    public void testModifiedProperty() {

        assertEquals("product should be initially un-modified", false, product.isModified());
        product.setModified(true);
        assertEquals(true, product.isModified());
        product.setModified(false);
        assertEquals(false, product.isModified());
    }

    @Test
    public void testModifiedFlagAfterBandHasBeenAddedAndRemoved() {

        Band band = new Band("band1", ProductData.TYPE_FLOAT32, _sceneWidth, _sceneHeight);

        assertEquals(null, product.getBand("band1"));

        //
        product.addBand(band);
        assertEquals(band, product.getBand("band1"));
        assertEquals("added band, modified flag should be set", true, product.isModified());
        product.setModified(false);

        product.removeBand(band);
        assertEquals(null, product.getBand("band1"));
        assertEquals("removed band, modified flag should be set", true, product.isModified());
    }

    @Test
    public void testModifiedFlagAfterBandHasBeenModified() {

        Band band = new Band("band1", ProductData.TYPE_FLOAT32, _sceneWidth, _sceneHeight);
        product.addBand(band);
        product.setModified(false);

        band.setData(ProductData.createInstance(new float[_sceneWidth * _sceneHeight]));
        assertEquals("data initialized, modified flag should not be set", false, product.isModified());

        band.setData(ProductData.createInstance(new float[_sceneWidth * _sceneHeight]));
        assertEquals("data modified, modified flag should be set", true, product.isModified());

        band.setModified(false);
        product.setModified(false);

        band.setData(null);
        assertEquals("data set to null, modified flag should be set", true, product.isModified());
    }

    @Test
    public void testModifiedFlagDelegation() {

        Band band1 = new Band("band1", ProductData.TYPE_FLOAT32, _sceneWidth, _sceneHeight);
        Band band2 = new Band("band2", ProductData.TYPE_FLOAT32, _sceneWidth, _sceneHeight);

        product.addBand(band1);
        product.addBand(band2);
        product.setModified(false);

        band1.setModified(true);
        assertEquals(true, band1.isModified());
        assertEquals(false, band2.isModified());
        assertEquals(true, product.isModified());

        band2.setModified(true);
        assertEquals(true, band1.isModified());
        assertEquals(true, band2.isModified());
        assertEquals(true, product.isModified());

        product.setModified(false);
        assertEquals(false, band1.isModified());
        assertEquals(false, band2.isModified());
        assertEquals(false, product.isModified());
    }

    @Test
    public void testSetGeocoding() {
        MapProjection projection = createMapProjectionForTestSetGeocoding();
        MapInfo mapInfo = new MapInfo(projection, 0, 0, 23, 24, 12, 13, Datum.WGS_84);
        MapGeoCoding mapGeoCoding = new MapGeoCoding(mapInfo);
        int sceneRasterWidth = 243;
        int sceneRasterHeight = 524;
        Product product = new Product("name", "type", sceneRasterWidth, sceneRasterHeight);

        mapInfo.setSceneWidth(sceneRasterWidth + 1);
        mapInfo.setSceneHeight(sceneRasterHeight);
        try {
            product.setSceneGeoCoding(mapGeoCoding);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {
            // IllegalArgumentException expected
        }

        mapInfo.setSceneWidth(sceneRasterWidth);
        mapInfo.setSceneHeight(sceneRasterHeight + 1);

        try {
            product.setSceneGeoCoding(mapGeoCoding);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {
            // IllegalArgumentException expected
        }

        mapInfo.setSceneWidth(sceneRasterWidth);
        mapInfo.setSceneHeight(sceneRasterHeight);

        try {
            product.setSceneGeoCoding(mapGeoCoding);
            // IllegalArgumentException not expected
        } catch (IllegalArgumentException ignored) {
            fail("IllegalArgumentException not expected");
        }
    }

    @Test
    public void testDefaultGroups() throws Exception {
        final Product p = new Product("n", "t", 10, 10);
        final ProductNodeGroup<ProductNodeGroup> groups = p.getGroups();
        assertNotNull(groups);

        assertNotNull(p.getGroup("bands"));
        assertSame(p.getGroup("bands"), groups.get("bands"));

        assertNotNull(p.getGroup("tie_point_grids"));
        assertSame(p.getGroup("tie_point_grids"), groups.get("tie_point_grids"));

        assertNotNull(p.getGroup("index_codings"));
        assertSame(p.getGroup("index_codings"), groups.get("index_codings"));

        assertNotNull(p.getGroup("flag_codings"));
        assertSame(p.getGroup("flag_codings"), groups.get("flag_codings"));

        assertNotNull(p.getGroup("masks"));
        assertSame(p.getGroup("masks"), groups.get("masks"));

        assertNotNull(p.getGroup("vector_data"));
        assertSame(p.getGroup("vector_data"), groups.get("vector_data"));
    }

    @Test
    public void testAddAndRemoveGroup() throws Exception {
        final Product p = new Product("n", "t", 10, 10);
        final ProductNodeGroup<ProductNodeGroup> groups = p.getGroups();
        assertNotNull(groups);

        int nodeCount0 = groups.getNodeCount();
        final TracingProductNodeListener listener = new TracingProductNodeListener();
        final int eventCount0 = listener.events.size();
        p.addProductNodeListener(listener);
        final ProductNodeGroup<MetadataElement> spectra = new ProductNodeGroup<>(p, "spectra", true);
        groups.add(spectra);
        assertEquals(groups.getNodeCount(), nodeCount0 + 1);
        assertEquals(listener.events.size(), eventCount0 + 1);

        assertSame(spectra, p.getGroup("spectra"));
        spectra.add(new MetadataElement("radiance"));
        assertEquals(listener.events.size(), eventCount0 + 2);
        spectra.add(new MetadataElement("reflectance"));
        assertEquals(listener.events.size(), eventCount0 + 3);
    }


    @Test
    public void testUniqueGeoCodings() {
        Product p = new Product("N", "T", 4, 4);

        assertFalse(p.isUsingSingleGeoCoding());

        final GeoCoding gc1 = new ProductUtilsTest.SGeoCoding();
        final GeoCoding gc2 = new ProductUtilsTest.DGeoCoding();
        p.setSceneGeoCoding(gc1);

        assertTrue(p.isUsingSingleGeoCoding());

        p.addBand("A", ProductData.TYPE_INT8);
        p.addBand("B", ProductData.TYPE_INT8);

        assertTrue(p.isUsingSingleGeoCoding());

        p.getBand("A").setGeoCoding(gc1);
        p.getBand("B").setGeoCoding(gc2);

        assertFalse(p.isUsingSingleGeoCoding());

        p.getBand("B").setGeoCoding(gc1);

        assertTrue(p.isUsingSingleGeoCoding());
    }

    @Test
    public void testContainsPixel() {
        Product p = new Product("x", "y", 1121, 2241);

        assertTrue(p.containsPixel(0.0f, 0.0f));
        assertTrue(p.containsPixel(0.0f, 2241.0f));
        assertTrue(p.containsPixel(1121.0f, 0.0f));
        assertTrue(p.containsPixel(1121.0f, 2241.0f));
        assertTrue(p.containsPixel(500.0f, 1000.0f));

        assertFalse(p.containsPixel(-0.1f, 0.0f));
        assertFalse(p.containsPixel(0.0f, 2241.1f));
        assertFalse(p.containsPixel(1121.0f, -0.1f));
        assertFalse(p.containsPixel(1121.1f, 2241.0f));
        assertFalse(p.containsPixel(-1, -1));

        p.dispose();
    }

    @Test
    public void testExpressionIsChangedIfANodeNameIsChanged() {
        final Product product = new Product("p", "t", 10, 10);
        final VirtualBand virtualBand = new VirtualBand("vb", ProductData.TYPE_FLOAT32, 10, 10,
                                                        "band1 + band2 - band3");
        final File fileLocation = new File("dummy.dim");
        product.setFileLocation(fileLocation);
        product.addBand(virtualBand);
        product.addBand("band1", ProductData.TYPE_FLOAT32);
        product.addBand("band2", ProductData.TYPE_FLOAT32);
        product.addBand("band3", ProductData.TYPE_FLOAT32);

        product.getBand("band1").setName("b1");

        assertEquals("Name 'band1' is not changed",
                     "b1 + band2 - band3", virtualBand.getExpression());

        assertSame(fileLocation, product.getFileLocation());
    }

    @Test
    public void testThatAddBandThrowExceptionIfNameIsNotUnique() {
        final Product product = new Product("p", "t", 1, 1);
        product.addBand("band1", ProductData.TYPE_FLOAT32);
        product.addTiePointGrid(new TiePointGrid("grid", 2, 2, 0, 0, 1, 1, new float[]{0.0f, 0.0f, 0.0f, 0.0f}));

        try {
            product.addBand("band1", ProductData.TYPE_FLOAT32);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("name"));
        }

        try {
            product.addBand("grid", ProductData.TYPE_FLOAT32);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("name"));
        }
    }

    @Test
    public void testThatAddTiePointGridThrowExceptionIfNameIsNotUnique() {
        final Product product = new Product("p", "t", 1, 1);
        product.addBand("band1", ProductData.TYPE_FLOAT32);
        product.addTiePointGrid(new TiePointGrid("grid", 2, 2, 0, 0, 1, 1, new float[]{0.0f, 0.0f, 0.0f, 0.0f}));

        try {
            product.addTiePointGrid(new TiePointGrid("grid", 2, 2, 0, 0, 1, 1, new float[]{0.0f, 0.0f, 0.0f, 0.0f}));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("name"));
        }

        try {
            product.addTiePointGrid(new TiePointGrid("band1", 2, 2, 0, 0, 1, 1, new float[]{0.0f, 0.0f, 0.0f, 0.0f}));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("name"));
        }
    }

    @Test
    public void testPreferredTileSizeProperty() {
        Product product;

        product = new Product("A", "B", 1000, 2000);
        assertEquals(null, product.getPreferredTileSize());

        product.setPreferredTileSize(new Dimension(128, 256));
        assertEquals(new Dimension(128, 256), product.getPreferredTileSize());

        product.setPreferredTileSize(new Dimension(300, 400));
        assertEquals(new Dimension(300, 400), product.getPreferredTileSize());

        product.setPreferredTileSize(null);
        assertEquals(null, product.getPreferredTileSize());
    }

    private static MapProjection createMapProjectionForTestSetGeocoding() {
        MapTransform mapTransform = new IdentityTransformDescriptor().createTransform(null);
        return new MapProjection("p1", mapTransform, "unit");
    }

    private static class MyProductNodeListener implements ProductNodeListener {
        String pname;

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            pname = event.getPropertyName();
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
        }
    }

    private static class TracingProductNodeListener extends ProductNodeListenerAdapter {
        List<ProductNodeEvent> events = new ArrayList<ProductNodeEvent>();

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            events.add(event);
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            events.add(event);
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            events.add(event);
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            events.add(event);
        }
    }
}

class DummyProductReader extends AbstractProductReader {

    DummyProductReader(DummyProductReaderPlugIn plugIn) {
        super(plugIn);
    }

    @Override
    public ProductReaderPlugIn getReaderPlugIn() {
        return null;
    }

    @Override
    public Product readProductNodesImpl() throws IOException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void close() throws IOException {
    }
}

class DummyProductReaderPlugIn implements ProductReaderPlugIn {

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        return DecodeQualification.UNABLE;
    }

    @Override
    public String[] getFormatNames() {
        return new String[0];
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[0];
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[0];
    }

    @Override
    public String getDescription(Locale locale) {
        return null;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new DummyProductReader(this);
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

}

class DummyProductNodeListener implements ProductNodeListener {

    public DummyProductNodeListener() {
    }

    @Override
    public void nodeChanged(ProductNodeEvent event) {
    }

    @Override
    public void nodeDataChanged(ProductNodeEvent event) {
    }

    @Override
    public void nodeAdded(ProductNodeEvent event) {
    }

    @Override
    public void nodeRemoved(ProductNodeEvent event) {
    }
}
