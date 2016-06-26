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
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.runtime.Config;
import org.junit.Test;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.CropDescriptor;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class PixelGeoCodingTest {

    private static final int S = 4;
    private static final int GW = 3;
    private static final int GH = 5;
    private static final int PW = (GW - 1) * S + 1;
    private static final int PH = (GH - 1) * S + 1;
    private static final float LAT_1 = 53.0f;
    private static final float LAT_2 = 50.0f;
    private static final float LON_1 = 10.0f;
    private static final float LON_2 = 15.0f;

    @Test
    public void testIllegalConstructorCalls() throws IOException {
        Product product;
        Band b1, b2;

        product = createProduct(ProductData.TYPE_FLOAT32);
        b1 = product.getBand("latBand");
        b2 = product.getBand("lonBand");

        // Should be ok
        assertNotNull(b1);
        assertNotNull(b2);
        testIllegalArgumentExceptionNotThrownByConstructor(b1, b2, null, 7);
        // b1 is null
        testIllegalArgumentExceptionThrownByConstructor(null, b2, null, 7);
        // b2 is null
        testIllegalArgumentExceptionThrownByConstructor(b1, null, null, 7);
        // b2 not attached to product
        product.removeBand(b2);
        testIllegalArgumentExceptionThrownByConstructor(b1, b2, null, 7);
        // b1 not attached to product
        product.addBand(b2);
        product.removeBand(b1);
        testIllegalArgumentExceptionThrownByConstructor(b1, b2, null, 7);

        // illegal search radius'
        product.addBand(b1);
        testIllegalArgumentExceptionThrownByConstructor(b1, b2, null, 0);
        testIllegalArgumentExceptionThrownByConstructor(b1, b2, null, -6);

        // illegal raster size (==1)
        product = new Product("test", "test", 1, 1);
        b1 = product.addBand("x", ProductData.TYPE_FLOAT32);
        b2 = product.addBand("y", ProductData.TYPE_FLOAT32);
        b1.setRasterData(ProductData.createInstance(new float[1]));
        b2.setRasterData(ProductData.createInstance(new float[1]));
        testIllegalArgumentExceptionThrownByConstructor(b1, b2, null, 5);

        // legal raster size  (>1)
        product = new Product("test", "test", 2, 2);
        b1 = product.addBand("x", ProductData.TYPE_FLOAT32);
        b2 = product.addBand("y", ProductData.TYPE_FLOAT32);
        b1.setRasterData(ProductData.createInstance(new float[4]));
        b2.setRasterData(ProductData.createInstance(new float[4]));
        testIllegalArgumentExceptionNotThrownByConstructor(b1, b2, null, 5);
    }

    private static void testIllegalArgumentExceptionNotThrownByConstructor(Band b1, Band b2, String validMask,
                                                                           int searchRadius) {
        try {
            GeoCodingFactory.createPixelGeoCoding(b1, b2, validMask, searchRadius, ProgressMonitor.NULL);
        } catch (IOException | IllegalArgumentException e) {
            fail();
        }
    }

    private static void testIllegalArgumentExceptionThrownByConstructor(Band b1, Band b2, String validMask,
                                                                        int searchRadius) {
        try {
            new PixelGeoCoding(b1, b2, validMask, searchRadius, ProgressMonitor.NULL);
            fail();
        } catch (IOException e) {
            fail();
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testEquals() throws Exception {
        Product product = createProduct(ProductData.TYPE_FLOAT32);
        GeoCoding geoCoding1 = new PixelGeoCoding(product.getBand("latBand"),
                                                  product.getBand("lonBand"), null, 5);
        GeoCoding geoCoding2 = new PixelGeoCoding(product.getBand("latBand"),
                                                  product.getBand("lonBand"), null, 5);
        GeoCoding geoCoding3 = new PixelGeoCoding(product.getBand("latBand"),
                                                  product.getBand("lonBand"), null, 7);
        product.setSceneGeoCoding(geoCoding1);
        assertEquals(geoCoding1, geoCoding2);
        assertFalse(geoCoding1.equals(geoCoding3));
    }

    @Test
    public void testGetPixelPos() throws IOException {
        Product product = createProduct(ProductData.TYPE_FLOAT32);
        TiePointGeoCoding tiePointGeoCoding = (TiePointGeoCoding) product.getSceneGeoCoding();
        GeoCoding pixelGeoCoding = GeoCodingFactory.createPixelGeoCoding(product.getBand("latBand"),
                                                                         product.getBand("lonBand"),
                                                                         null,
                                                                         2,
                                                                         ProgressMonitor.NULL);
        product.setSceneGeoCoding(pixelGeoCoding);
        TiePointGrid latGrid = tiePointGeoCoding.getLatGrid();
        TiePointGrid lonGrid = tiePointGeoCoding.getLonGrid();
        GeoPos gp = new GeoPos(latGrid.getTiePoints()[0], lonGrid.getTiePoints()[0]);
        PixelPos pixelPos = pixelGeoCoding.getPixelPos(gp, null);
        assertEquals(new PixelPos(0.5, 0.5), pixelPos);
        System.out.println("----");
        gp = new GeoPos((latGrid.getTiePoints()[0] + latGrid.getTiePoints()[1]) / 2,
                        (lonGrid.getTiePoints()[0] + lonGrid.getTiePoints()[1]) / 2);
        pixelPos = pixelGeoCoding.getPixelPos(gp, null);
        assertEquals(new PixelPos(2.5, 0.5), pixelPos);
    }

    @Test
    public void testGetGeoPos_Float() throws IOException {
        doTestGetGeoPos(ProductData.TYPE_FLOAT32);
    }

    @Test
    public void testGetGeoPos_Double() throws IOException {
        doTestGetGeoPos(ProductData.TYPE_FLOAT64);
    }

    @Test
    public void testGetGeoPos__AlternatGC_Float() throws IOException {
        try {
            Config.instance().preferences().putBoolean("snap.useAlternatePixelGeoCoding", true);
            doTestGetGeoPos(ProductData.TYPE_FLOAT32);
        } finally {
            Config.instance().preferences().remove("snap.useAlternatePixelGeoCoding");
        }
    }

    @Test
    public void testGetGeoPos__AlternatGC_Double() throws IOException {
        try {
            Config.instance().preferences().putBoolean("snap.useAlternatePixelGeoCoding", true);
            doTestGetGeoPos(ProductData.TYPE_FLOAT64);
        } finally {
            Config.instance().preferences().remove("snap.useAlternatePixelGeoCoding");
        }
    }

    @Test
    public void testGetGeoPos_useNoTiling() throws IOException {
        try {
            Config.instance().preferences().putBoolean("snap.pixelGeoCoding.useTiling", false);
            doTestGetGeoPos(ProductData.TYPE_FLOAT32);
        } finally {
            Config.instance().preferences().remove("snap.pixelGeoCoding.useTiling");
        }
    }

    private void doTestGetGeoPos(int latLonBandDataType) throws IOException {
        Product product = createProduct(latLonBandDataType);
        TiePointGeoCoding tiePointGeoCoding = (TiePointGeoCoding) product.getSceneGeoCoding();
        GeoCoding pixelGeoCoding = GeoCodingFactory.createPixelGeoCoding(product.getBand("latBand"),
                                                                         product.getBand("lonBand"), null, 5,
                                                                         ProgressMonitor.NULL);
        product.setSceneGeoCoding(pixelGeoCoding);

        String gp;

        gp = new GeoPos(tiePointGeoCoding.getLatGrid().getTiePoints()[0],
                        tiePointGeoCoding.getLonGrid().getTiePoints()[0]).toString();
        assertEquals(gp, tiePointGeoCoding.getGeoPos(new PixelPos(0.5, 0.5), null).toString());
        assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(0.5, 0.5), null).toString());

        gp = new GeoPos(tiePointGeoCoding.getLatGrid().getTiePoints()[GW - 1],
                        tiePointGeoCoding.getLonGrid().getTiePoints()[GW - 1]).toString();
        assertEquals(gp, tiePointGeoCoding.getGeoPos(new PixelPos(PW - 0.5, 0.5), null).toString());
        assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(PW - 0.5, 0.5), null).toString());

        gp = new GeoPos(tiePointGeoCoding.getLatGrid().getTiePoints()[GW * (GH - 1)],
                        tiePointGeoCoding.getLonGrid().getTiePoints()[GW * (GH - 1)]).toString();
        assertEquals(gp, tiePointGeoCoding.getGeoPos(new PixelPos(0.5, PH - 0.5), null).toString());
        assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(0.5, PH - 0.5), null).toString());

        gp = new GeoPos(tiePointGeoCoding.getLatGrid().getTiePoints()[GW * GH - 1],
                        tiePointGeoCoding.getLonGrid().getTiePoints()[GW * GH - 1]).toString();
        assertEquals(gp, tiePointGeoCoding.getGeoPos(new PixelPos(PW - 0.5, PH - 0.5), null).toString());
        assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(PW - 0.5, PH - 0.5), null).toString());
    }

    @Test
    public void testGetGeoPos_withFractionAccuracy() throws IOException {
        Product product = createProduct(ProductData.TYPE_FLOAT32);
        TiePointGeoCoding tiePointGeoCoding = (TiePointGeoCoding) product.getSceneGeoCoding();
        GeoCoding pixelGeoCoding = GeoCodingFactory.createPixelGeoCoding(product.getBand("latBand"),
                                                                         product.getBand("lonBand"), null, 5,
                                                                         ProgressMonitor.NULL);
        product.setSceneGeoCoding(pixelGeoCoding);

        String gp = tiePointGeoCoding.getGeoPos(new PixelPos(0.5, 0.5), null).toString();
        assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(0.25, 0.25), null).toString());

        try {
            Config.instance().preferences().putBoolean("snap.pixelGeoCoding.fractionAccuracy", true);
            Config.instance().preferences().putBoolean("snap.pixelGeoCoding.useTiling", true);
            product = createProduct(ProductData.TYPE_FLOAT32);
            tiePointGeoCoding = (TiePointGeoCoding) product.getSceneGeoCoding();
            pixelGeoCoding = GeoCodingFactory.createPixelGeoCoding(product.getBand("latBand"),
                                                                   product.getBand("lonBand"), null, 5,
                                                                   ProgressMonitor.NULL);

            gp = tiePointGeoCoding.getGeoPos(new PixelPos(0.5, 0.5), null).toString();
            assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(0.5, 0.5), null).toString());

            gp = tiePointGeoCoding.getGeoPos(new PixelPos(0.25, 0.25), null).toString();
            assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(0.25, 0.25), null).toString());

            gp = tiePointGeoCoding.getGeoPos(new PixelPos(0.75, 0.75), null).toString();
            assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(0.75, 0.75), null).toString());

            gp = tiePointGeoCoding.getGeoPos(new PixelPos(0.25, 0.75), null).toString();
            assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(0.25, 0.75), null).toString());

            gp = tiePointGeoCoding.getGeoPos(new PixelPos(0.75, 0.25), null).toString();
            assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(0.75, 0.25), null).toString());

            gp = tiePointGeoCoding.getGeoPos(new PixelPos(1.5, 1.5), null).toString();
            assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(1.5, 1.5), null).toString());

            gp = tiePointGeoCoding.getGeoPos(new PixelPos(1.25, 1.25), null).toString();
            assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(1.25, 1.25), null).toString());

            gp = tiePointGeoCoding.getGeoPos(new PixelPos(1.75, 1.75), null).toString();
            assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(1.75, 1.75), null).toString());

            gp = tiePointGeoCoding.getGeoPos(new PixelPos(1.25, 1.75), null).toString();
            assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(1.25, 1.75), null).toString());

            gp = tiePointGeoCoding.getGeoPos(new PixelPos(1.75, 1.25), null).toString();
            assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(1.75, 1.25), null).toString());

            gp = tiePointGeoCoding.getGeoPos(new PixelPos(2.5, 2.5), null).toString();
            assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(2.5, 2.5), null).toString());

            gp = tiePointGeoCoding.getGeoPos(new PixelPos(2.25, 2.25), null).toString();
            assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(2.25, 2.25), null).toString());

            gp = tiePointGeoCoding.getGeoPos(new PixelPos(2.75, 2.75), null).toString();
            assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(2.75, 2.75), null).toString());

            gp = tiePointGeoCoding.getGeoPos(new PixelPos(2.25, 2.75), null).toString();
            assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(2.25, 2.75), null).toString());

            gp = tiePointGeoCoding.getGeoPos(new PixelPos(2.75, 2.25), null).toString();
            assertEquals(gp, pixelGeoCoding.getGeoPos(new PixelPos(2.75, 2.25), null).toString());
        } finally {
            Config.instance().preferences().remove("snap.pixelGeoCoding.fractionAccuracy");
            Config.instance().preferences().remove("snap.pixelGeoCoding.useTiling");
        }
    }

    @Test
    public void testTransferGeoCoding() throws IOException {
        doTestTransferGeoCoding();
    }

    @Test
    public void testTransferGeoCoding_useNoTiling() throws IOException {
        try {
            Config.instance().preferences().putBoolean("snap.pixelGeoCoding.useTiling", false);
            doTestTransferGeoCoding();
        } finally {
            Config.instance().preferences().remove("snap.pixelGeoCoding.useTiling");
        }
    }

    private void doTestTransferGeoCoding() throws IOException {
        Product sourceProduct = createProduct(ProductData.TYPE_FLOAT32);
        GeoCoding newGeoCoding = new PixelGeoCoding(sourceProduct.getBand("latBand"),
                                                    sourceProduct.getBand("lonBand"), null, 5,
                                                    ProgressMonitor.NULL);
        sourceProduct.setSceneGeoCoding(newGeoCoding);

        Product targetProduct = createProduct(ProductData.TYPE_FLOAT32);
        targetProduct.setSceneGeoCoding(null);   // remove geo-coding of target product

        sourceProduct.transferGeoCodingTo(targetProduct, null);

        PixelGeoCoding targetGC = (PixelGeoCoding) targetProduct.getSceneGeoCoding();
        assertNotNull(targetGC.getPixelPosEstimator());
    }

    @Test
    public void testTransferGeoCoding_WithSpatialSubset() throws IOException {
        doTestTransferGeoCoding_WithSpatialSubset();
    }

    @Test
    public void testTransferGeoCoding_WithSpatialSubset_useNoTiling() throws IOException {
        try {
            Config.instance().preferences().putBoolean("snap.pixelGeoCoding.useTiling", false);
            doTestTransferGeoCoding_WithSpatialSubset();
        } finally {
            Config.instance().preferences().remove("snap.pixelGeoCoding.useTiling");
        }
    }

    private void doTestTransferGeoCoding_WithSpatialSubset() throws IOException {
        Product sourceProduct = createProduct(ProductData.TYPE_FLOAT32);
        GeoCoding pixelGeoCoding = GeoCodingFactory.createPixelGeoCoding(sourceProduct.getBand("latBand"),
                                                                         sourceProduct.getBand("lonBand"),
                                                                         "flagomat.valid", 5,
                                                                         ProgressMonitor.NULL);
        sourceProduct.setSceneGeoCoding(pixelGeoCoding);

        final ProductSubsetDef def = new ProductSubsetDef();
        final int subsetWidth = sourceProduct.getSceneRasterWidth() - 3;
        final int subsetHeight = sourceProduct.getSceneRasterHeight() - 3;
        def.setRegion(2, 2, subsetWidth, subsetHeight);
        def.setSubSampling(1, 2);
        Product targetProduct = sourceProduct.createSubset(def, "target", "");

        if (Config.instance().preferences().getBoolean(GeoCodingFactory.USE_ALTERNATE_PIXEL_GEO_CODING_PROPERTY, false)) {
            targetProduct.setSceneGeoCoding(null);
            targetProduct.removeTiePointGrid(targetProduct.getTiePointGrid("latGrid"));
            targetProduct.removeTiePointGrid(targetProduct.getTiePointGrid("lonGrid"));
            targetProduct.removeBand(targetProduct.getBand("latBand"));
            targetProduct.removeBand(targetProduct.getBand("lonBand"));
            targetProduct.removeBand(targetProduct.getBand("flagomat"));
            targetProduct.getFlagCodingGroup().removeAll();
            sourceProduct.transferGeoCodingTo(targetProduct, def);
        }

        assertTrue(targetProduct.containsBand("latBand"));
        assertTrue(targetProduct.containsBand("lonBand"));
        assertTrue(targetProduct.containsBand("flagomat"));

        if (Config.instance().preferences().getBoolean(GeoCodingFactory.USE_ALTERNATE_PIXEL_GEO_CODING_PROPERTY, false)) {
            assertTrue(targetProduct.containsTiePointGrid("latGrid"));
            assertTrue(targetProduct.containsTiePointGrid("lonGrid"));
        }

        assertTrue(targetProduct.getFlagCodingGroup().contains("flags"));

        BasicPixelGeoCoding targetGC = (BasicPixelGeoCoding) targetProduct.getSceneGeoCoding();
        if (Config.instance().preferences().getBoolean(GeoCodingFactory.USE_ALTERNATE_PIXEL_GEO_CODING_PROPERTY, false)) {
            assertNotNull(targetGC.getPixelPosEstimator());
        }

        final GeoPos sourceGeoPos = sourceProduct.getSceneGeoCoding().getGeoPos(new PixelPos(2.5, 2.5), null);
        final GeoPos targetGeoPos = targetProduct.getSceneGeoCoding().getGeoPos(new PixelPos(0.0, 0.0), null);
        assertEquals(sourceGeoPos.getLat(), targetGeoPos.getLat(), 1.0e-1);
        assertEquals(sourceGeoPos.getLon(), targetGeoPos.getLon(), 1.0e-1);

        assertEquals(6, targetProduct.getSceneRasterWidth());
        assertEquals(7, targetProduct.getSceneRasterHeight());
        Raster data = targetProduct.getBand("latBand").getSourceImage().getData();
        assertNotNull(data);
        assertEquals(6, data.getWidth());
        assertEquals(7, data.getHeight());
    }

    private Product createProduct(int latLonBandDataType) {
        Product product = new Product("test", "test", PW, PH);

        TiePointGrid latGrid = new TiePointGrid("latGrid", GW, GH, 0.5, 0.5, S, S, createLatGridData());
        TiePointGrid lonGrid = new TiePointGrid("lonGrid", GW, GH, 0.5, 0.5, S, S, createLonGridData());

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);

        Band latBand = product.addBand("latBand", latLonBandDataType);
        Band lonBand = product.addBand("lonBand", latLonBandDataType);

        latBand.setRasterData(createBandData(latGrid, latLonBandDataType));
        lonBand.setRasterData(createBandData(lonGrid, latLonBandDataType));
        final FlagCoding flagCoding = new FlagCoding("flags");
        flagCoding.addFlag("valid", 0x01, "valid pixel");

        product.getFlagCodingGroup().add(flagCoding);

        Band flagomatBand = product.addBand("flagomat", ProductData.TYPE_UINT8);
        byte[] flagomatData = new byte[PW * PH];
        Arrays.fill(flagomatData, (byte) 0x01);
        flagomatBand.setRasterData(ProductData.createInstance(ProductData.TYPE_UINT8, flagomatData));
        flagomatBand.setSampleCoding(flagCoding);

        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));

        return product;
    }

    private float[] createLatGridData() {
        return createGridData(LAT_1, LAT_2);
    }

    private float[] createLonGridData() {
        return createGridData(LON_1, LON_2);
    }

    private static ProductData createBandData(TiePointGrid grid, int latLonBandDataType) {
        ProductData bandData = ProductData.createInstance(latLonBandDataType, PW * PH);
        for (int y = 0; y < PH; y++) {
            for (int x = 0; x < PW; x++) {
                bandData.setElemFloatAt(y * PW + x, grid.getPixelFloat(x, y));
            }
        }
        return bandData;
    }

    private static float[] createGridData(float lon0, float lon1) {
        float[] floats = new float[GW * GH];

        for (int j = 0; j < GH; j++) {
            for (int i = 0; i < GW; i++) {
                float x = i / (GW - 1.0f);
                float y = j / (GH - 1.0f);
                floats[j * GW + i] = lon0 + (lon1 - lon0) * x * x + 0.1f * (lon1 - lon0) * y * y;
            }
        }

        return floats;
    }

    @Test
    public void testThatImageMinXYAreImportant() throws Exception {
        int minX = 4;
        int minY = 3;

        final RenderedOp src = ConstantDescriptor.create(16F, 16F, new Short[]{(short) 33}, null);
        final RenderedOp dst = CropDescriptor.create(src, (float) minX, (float) minY, 8F, 8F, null);

        assertEquals(minX, dst.getMinX());
        assertEquals(minY, dst.getMinY());

        // Test that we have to use the min X and Y
        try {
            dst.getData(new Rectangle(0, 0, 1, 1));
            fail("IllegalArgumentException thrown by JAI expected");
        } catch (IllegalArgumentException e) {
            // ok
        }

        final Raster data = dst.getData(new Rectangle(minX, minY, 1, 1));
        final short[] outData = new short[1];

        // Test that we have to use the min X and Y
        try {
            data.getDataElements(0, 0, outData);
            fail("ArrayIndexOutOfBoundsException thrown by AWT expected");
        } catch (ArrayIndexOutOfBoundsException e) {
            // ok
        }

        data.getDataElements(minX, minY, outData);
        assertEquals(33, outData[0]);

        // In many cases, we use the tile rectangles in order to retrieve pixel data
        // So make sure it also considers the min X and Y
        assertEquals(new Rectangle(minX, minY, 8, 8), dst.getTileRect(0, 0));
    }

    @Test
    public void testGetPositiveLonMin() throws Exception {
        double lon0 = 160.0;
        double lon1 = 150.0;
        double lon2 = -169.0;
        double lon3 = 165.0;
        double result = PixelGeoCoding.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(150.0, result, 0.0);

        lon0 = -175.0;
        lon1 = 170.0;
        lon2 = -169.0;
        lon3 = -170.0;
        result = PixelGeoCoding.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(170.0, result, 0.0);

        lon0 = 170.0;
        lon1 = 160.0;
        lon2 = -175.0;
        lon3 = -165.0;
        result = PixelGeoCoding.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(160.0, result, 0.0);

        lon0 = -150.0;
        lon1 = +160.0;
        lon2 = -140.0;
        lon3 = -170.0;
        result = PixelGeoCoding.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(160.0, result, 0.0);

        lon0 = 170.0;
        lon1 = -175.0;
        lon2 = -165.0;
        lon3 = -150.0;
        result = PixelGeoCoding.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(170.0, result, 0.0);

        lon0 = 140.0;
        lon1 = 150.0;
        lon2 = 160.0;
        lon3 = -170.0;
        result = PixelGeoCoding.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(140.0, result, 0.0);

        lon0 = -175.0;
        lon1 = -165.0;
        lon2 = 170.0;
        lon3 = 160.0;
        result = PixelGeoCoding.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(160.0, result, 0.0);

        lon0 = 170.0;
        lon1 = -140.0;
        lon2 = 160.0;
        lon3 = -150.0;
        result = PixelGeoCoding.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(160.0, result, 0.0);

        lon0 = -160.0;
        lon1 = -150.0;
        lon2 = 170.0;
        lon3 = -170.0;
        result = PixelGeoCoding.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(170.0, result, 0.0);

        lon0 = 170.0;
        lon1 = -170.0;
        lon2 = 150.0;
        lon3 = 160.0;
        result = PixelGeoCoding.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(150.0, result, 0.0);

        lon0 = -170.0;
        lon1 = 150.0;
        lon2 = 160.0;
        lon3 = 140.0;
        result = PixelGeoCoding.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(140.0, result, 0.0);

        lon0 = -150.0;
        lon1 = -170.0;
        lon2 = -160.0;
        lon3 = 170.0;
        result = PixelGeoCoding.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(170.0, result, 0.0);
    }

    @Test
    public void testGetNegativeLonMax() throws Exception {
        double lon0 = 160.0;
        double lon1 = 150.0;
        double lon2 = -169.0;
        double lon3 = 165.0;
        double result = PixelGeoCoding.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-169.0, result, 0.0);

        lon0 = -175.0;
        lon1 = 170.0;
        lon2 = -169.0;
        lon3 = -170.0;
        result = PixelGeoCoding.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-169.0, result, 0.0);

        lon0 = 170.0;
        lon1 = 160.0;
        lon2 = -175.0;
        lon3 = -165.0;
        result = PixelGeoCoding.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-165.0, result, 0.0);

        lon0 = -150.0;
        lon1 = +160.0;
        lon2 = -140.0;
        lon3 = -170.0;
        result = PixelGeoCoding.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-140.0, result, 0.0);

        lon0 = 170.0;
        lon1 = -175.0;
        lon2 = -165.0;
        lon3 = -150.0;
        result = PixelGeoCoding.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-150.0, result, 0.0);

        lon0 = 140.0;
        lon1 = 150.0;
        lon2 = 160.0;
        lon3 = -170.0;
        result = PixelGeoCoding.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-170.0, result, 0.0);

        lon0 = -175.0;
        lon1 = -165.0;
        lon2 = 170.0;
        lon3 = 160.0;
        result = PixelGeoCoding.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-165.0, result, 0.0);

        lon0 = 170.0;
        lon1 = -140.0;
        lon2 = 160.0;
        lon3 = -150.0;
        result = PixelGeoCoding.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-140.0, result, 0.0);

        lon0 = -160.0;
        lon1 = -150.0;
        lon2 = 170.0;
        lon3 = -170.0;
        result = PixelGeoCoding.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-150.0, result, 0.0);

        lon0 = 170.0;
        lon1 = -170.0;
        lon2 = 150.0;
        lon3 = 160.0;
        result = PixelGeoCoding.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-170.0, result, 0.0);

        lon0 = -170.0;
        lon1 = 150.0;
        lon2 = 160.0;
        lon3 = 140.0;
        result = PixelGeoCoding.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-170.0, result, 0.0);

        lon0 = -150.0;
        lon1 = -170.0;
        lon2 = -160.0;
        lon3 = 170.0;
        result = PixelGeoCoding.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-150.0, result, 0.0);
    }

    @Test
    public void testIsCrossingMeridianInsideQuad() throws Exception {
        double lon0 = 160.0;
        double lon1 = 150.0;
        double lon2 = -169.0;
        double lon3 = 165.0;
        assertTrue(PixelGeoCoding.isCrossingMeridianInsideQuad(true, lon0, lon1, lon2, lon3));

        lon0 = -160.0;
        lon1 = -150.0;
        lon2 = 170.0;
        lon3 = -170.0;
        assertTrue(PixelGeoCoding.isCrossingMeridianInsideQuad(true, lon0, lon1, lon2, lon3));

        lon0 = 170.0;
        lon1 = 170.0;
        lon2 = 179.0;
        lon3 = 179.0;
        assertFalse(PixelGeoCoding.isCrossingMeridianInsideQuad(true, lon0, lon1, lon2, lon3));

        lon0 = 170.0;
        lon1 = 170.0;
        lon2 = 179.0;
        lon3 = 179.0;
        assertFalse(PixelGeoCoding.isCrossingMeridianInsideQuad(false, lon0, lon1, lon2, lon3));

        lon0 = -10.0;
        lon1 = -10.0;
        lon2 = 10.0;
        lon3 = 10.0;
        assertFalse(PixelGeoCoding.isCrossingMeridianInsideQuad(false, lon0, lon1, lon2, lon3));
    }
}
