/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.util;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.LineTimeCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.esa.snap.core.util.DummyProductBuilder.GC.*;
import static org.esa.snap.core.util.DummyProductBuilder.GCOcc.*;
import static org.esa.snap.core.util.DummyProductBuilder.I2M.*;
import static org.esa.snap.core.util.DummyProductBuilder.SizeOcc.*;
import static org.junit.Assert.*;

public class ProductUtilsTest {

    private static final float EPS = 1.0e-6f;

    @Test
    public void testGetAngleSum() {
        double angleSum;

        angleSum = ProductUtils.getAngleSum(createPositiveRotationGeoPolygon(0));
        assertEquals(Math.PI * 2, angleSum, 1.0e-6);

        angleSum = ProductUtils.getAngleSum(createNegativeRotationGeoPolygon(0));
        assertEquals(Math.PI * 2 * -1, angleSum, 1.0e-6);
    }

    @Test
    public void testGetRotationDirection() {
        int rotationDirection;

        rotationDirection = ProductUtils.getRotationDirection(createPositiveRotationGeoPolygon(0));
        assertEquals(1, rotationDirection);

        rotationDirection = ProductUtils.getRotationDirection(createNegativeRotationGeoPolygon(0));
        assertEquals(-1, rotationDirection);
    }

    @Test
    public void testNormalizeGeoBoundary() {
        GeoPos[] boundary;
        GeoPos[] expected;
        int expectedNormalizing;

        // Area does not intersect 180 degree meridian
        // --> no modification expected
        // --> longitude min/max of actual area are fully within [-180,+180] after normalizing
        boundary = createPositiveRotationGeoPolygon(0);
        expected = createPositiveRotationGeoPolygon(0);
        expectedNormalizing = 0;
        assertNormalizing(boundary, expectedNormalizing, expected);

        // Area does not intersect 180 degree meridian
        // --> no modification expected
        // --> longitude min/max of actual area are fully within [-180,+180] after normalizing
        boundary = createNegativeRotationGeoPolygon(0);
        expected = createNegativeRotationGeoPolygon(0);
        expectedNormalizing = 0;
        assertNormalizing(boundary, expectedNormalizing, expected);

        // Area does intersect 180 degree meridian at the upper side
        // --> modification expected
        // --> longitude min/max of actual area are fully within [-180,+360] after normalizing
        boundary = createPositiveRotationGeoPolygon(175);
        ProductUtils.denormalizeGeoPolygon(boundary);
        expected = createPositiveRotationGeoPolygon(175);
        expectedNormalizing = 1;
        assertNormalizing(boundary, expectedNormalizing, expected);

        // Area does intersect 180 degree meridian at the upper side
        // --> modification expected
        // --> longitude min/max of actual area are fully within [-180,+360] after normalizing
        boundary = createNegativeRotationGeoPolygon(175);
        ProductUtils.denormalizeGeoPolygon(boundary);
        expected = createNegativeRotationGeoPolygon(175);
        expectedNormalizing = 1;
        assertNormalizing(boundary, expectedNormalizing, expected);

        // Area does intersect 180 degree meridian at the lower side
        // --> modification expected
        // --> longitude min/max of actual area are fully within [-360,+180] after normalizing
        boundary = createPositiveRotationGeoPolygon(-135);
        ProductUtils.denormalizeGeoPolygon(boundary);
        expected = createPositiveRotationGeoPolygon(-135);
        shiftGeoPolygon(expected, 360);
        expectedNormalizing = -1;
        assertNormalizing(boundary, expectedNormalizing, expected);

        // Area does intersect 180 degree meridian at the lower side
        // --> modification expected
        // --> longitude min/max of actual area are fully within [-360,+180] after normalizing
        boundary = createNegativeRotationGeoPolygon(-135);
        ProductUtils.denormalizeGeoPolygon(boundary);
        expected = createNegativeRotationGeoPolygon(-135);
        shiftGeoPolygon(expected, 360);
        expectedNormalizing = -1;
        assertNormalizing(boundary, expectedNormalizing, expected);
    }

    @Test
    public void testDenormalizeGeoPos() {
        final GeoPos geoPos = new GeoPos();

        geoPos.lon = -678.2f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(-678.2f + 720.0f, geoPos.lon, 1.0e-8);

        geoPos.lon = -540.108f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(-540.108f + 720.0f, geoPos.lon, 1.0e-8);

        geoPos.lon = -539.67f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(-539.67f + 360.0f, geoPos.lon, 1.0e-8);

        geoPos.lon = -256.98f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(-256.98f + 360.0f, geoPos.lon, 1.0e-8);

        geoPos.lon = -180.3f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(-180.3f + 360.0f, geoPos.lon, 1.0e-8);

        geoPos.lon = -179.4f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(-179.4f, geoPos.lon, 1.0e-8);

        geoPos.lon = -34;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(-34, geoPos.lon, 1.0e-8);

        geoPos.lon = 0.34f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(0.34f, geoPos.lon, 1.0e-8);

        geoPos.lon = 114.9f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(114.9f, geoPos.lon, 1.0e-8);

        geoPos.lon = 184.4f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(184.4f - 360.0f, geoPos.lon, 1.0e-8);

        geoPos.lon = 245.7f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(245.7f - 360.0f, geoPos.lon, 1.0e-8);

        geoPos.lon = 536.9f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(536.9f - 360.0f, geoPos.lon, 1.0e-8);

        geoPos.lon = 541.5f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(541.5f - 720.0f, geoPos.lon, 1.0e-8);

        geoPos.lon = 722.5f;
        ProductUtils.denormalizeGeoPos(geoPos);
        assertEquals(722.5f - 720.0f, geoPos.lon, 1.0e-8);
    }

    @Test
    public void testNormalizing_crossingMeridianTwice() {
        GeoPos[] expected = createPositiveRotationDualMeridianGeoPolygon();
        GeoPos[] converted = createPositiveRotationDualMeridianGeoPolygon();

        ProductUtils.normalizeGeoPolygon(converted);
        ProductUtils.denormalizeGeoPolygon(converted);

        for (int i = 0; i < expected.length; i++) {
            assertEquals("at index " + i, expected[i], converted[i]);
        }
    }

    @Test
    public void testCopyTiePointGrids() {
        final Product sourceProduct = new Product("p1n", "p1t", 20, 20);

        final float[] tpg1tp = new float[]{
                1, 2, 3, 4, 5,
                6, 7, 8, 9, 10,
                11, 12, 13, 14, 15,
                16, 17, 18, 19, 20
        };
        final TiePointGrid tiePointGrid1 = new TiePointGrid("tpg1n", 5, 4, 2, 3, 4, 5, tpg1tp);
        tiePointGrid1.setDescription("tpg1d");
        tiePointGrid1.setUnit("tpg1u");
        sourceProduct.addTiePointGrid(tiePointGrid1);

        final float[] tpg2tp = new float[]{
                1, 2, 3, 4,
                5, 6, 7, 8,
                9, 10, 11, 12,
                13, 14, 15, 16,
                17, 18, 19, 20
        };
        final TiePointGrid tiePointGrid2 = new TiePointGrid("tpg2n", 4, 5, 1.2f, 1.4f, 5, 4, tpg2tp);
        tiePointGrid2.setDescription("tpg2d");
        tiePointGrid2.setUnit("tpg2u");
        sourceProduct.addTiePointGrid(tiePointGrid2);

        final Product targetProduct = new Product("p2n", "p2t", 200, 200);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);

        assertEquals(2, sourceProduct.getNumTiePointGrids());
        assertEquals("tpg1n", sourceProduct.getTiePointGridAt(0).getName());
        assertEquals(5, sourceProduct.getTiePointGridAt(0).getGridWidth());
        assertEquals(4, sourceProduct.getTiePointGridAt(0).getGridHeight());
        assertEquals(2.0f, sourceProduct.getTiePointGridAt(0).getOffsetX(), 1.0e-5);
        assertEquals(3.0f, sourceProduct.getTiePointGridAt(0).getOffsetY(), 1.0e-5);
        assertEquals(4.0f, sourceProduct.getTiePointGridAt(0).getSubSamplingX(), 1.0e-5);
        assertEquals(5.0f, sourceProduct.getTiePointGridAt(0).getSubSamplingY(), 1.0e-5);
        assertEquals(tpg1tp, sourceProduct.getTiePointGridAt(0).getDataElems());
        assertEquals("tpg2n", sourceProduct.getTiePointGridAt(1).getName());
        assertEquals(4, sourceProduct.getTiePointGridAt(1).getGridWidth());
        assertEquals(5, sourceProduct.getTiePointGridAt(1).getGridHeight());
        assertEquals(1.2f, sourceProduct.getTiePointGridAt(1).getOffsetX(), 1.0e-5);
        assertEquals(1.4f, sourceProduct.getTiePointGridAt(1).getOffsetY(), 1.0e-5);
        assertEquals(5.0f, sourceProduct.getTiePointGridAt(1).getSubSamplingX(), 1.0e-5);
        assertEquals(4.0f, sourceProduct.getTiePointGridAt(1).getSubSamplingY(), 1.0e-5);
        assertEquals(tpg2tp, sourceProduct.getTiePointGridAt(1).getDataElems());

        assertEquals(2, targetProduct.getNumTiePointGrids());
        assertEquals("tpg1n", targetProduct.getTiePointGridAt(0).getName());
        assertEquals(5, targetProduct.getTiePointGridAt(0).getGridWidth());
        assertEquals(4, targetProduct.getTiePointGridAt(0).getGridHeight());
        assertEquals(2.0f, targetProduct.getTiePointGridAt(0).getOffsetX(), 1.0e-5);
        assertEquals(3.0f, targetProduct.getTiePointGridAt(0).getOffsetY(), 1.0e-5);
        assertEquals(4.0f, targetProduct.getTiePointGridAt(0).getSubSamplingX(), 1.0e-5);
        assertEquals(5.0f, targetProduct.getTiePointGridAt(0).getSubSamplingY(), 1.0e-5);
        assertTrue(Arrays.equals(tpg1tp, (float[]) targetProduct.getTiePointGridAt(0).getDataElems()));
        assertEquals("tpg2n", targetProduct.getTiePointGridAt(1).getName());
        assertEquals(4, targetProduct.getTiePointGridAt(1).getGridWidth());
        assertEquals(5, targetProduct.getTiePointGridAt(1).getGridHeight());
        assertEquals(1.2f, targetProduct.getTiePointGridAt(1).getOffsetX(), 1.0e-5);
        assertEquals(1.4f, targetProduct.getTiePointGridAt(1).getOffsetY(), 1.0e-5);
        assertEquals(5.0f, targetProduct.getTiePointGridAt(1).getSubSamplingX(), 1.0e-5);
        assertEquals(4.0f, targetProduct.getTiePointGridAt(1).getSubSamplingY(), 1.0e-5);
        assertTrue(Arrays.equals(tpg2tp, (float[]) targetProduct.getTiePointGridAt(1).getDataElems()));
    }

    @Test
    public void testCopyFlagBands() {
        final int size = 10;
        final Product source = new Product("source", "test", size, size);
        final Band flagBand = source.addBand("flag", ProductData.TYPE_INT8);
        flagBand.setSourceImage(ConstantDescriptor.create((float) size, (float) size, new Byte[]{42}, null));
        final FlagCoding originalFlagCoding = new FlagCoding("flagCoding");
        originalFlagCoding.addFlag("erni", 1, "erni flag");
        originalFlagCoding.addFlag("bert", 2, "bert flag");
        originalFlagCoding.addFlag("bibo", 4, "bert flag");
        flagBand.setSampleCoding(originalFlagCoding);
        source.getFlagCodingGroup().add(originalFlagCoding);
        final String maskName = "erni_mask";
        final Mask mask = Mask.BandMathsType.create(maskName, "erni detected", size, size, "flag.erni",
                                                    Color.WHITE, 0.6f);
        source.getMaskGroup().add(mask);

        Product target = new Product("target", "T", size, size);
        ProductUtils.copyFlagBands(source, target, false);

        assertEquals(1, target.getFlagCodingGroup().getNodeCount());
        Band targetFlagBand = target.getBand("flag");
        assertNotNull(targetFlagBand);
        assertTrue(targetFlagBand.isFlagBand());
        assertFalse(targetFlagBand.isSourceImageSet());
        assertTrue(target.getMaskGroup().contains(maskName));

        target = new Product("target", "T", size, size);
        ProductUtils.copyFlagBands(source, target, true);

        assertEquals(1, target.getFlagCodingGroup().getNodeCount());
        targetFlagBand = target.getBand("flag");
        assertNotNull(targetFlagBand);
        assertTrue(targetFlagBand.isFlagBand());
        assertTrue(targetFlagBand.isSourceImageSet());
        assertTrue(target.getMaskGroup().contains(maskName));
    }

    @Test
    public void testCopyBandsForGeomTransform() {
        final int sourceWidth = 100;
        final int sourceHeight = 200;
        final Product source = new Product("source", "test", sourceWidth, sourceHeight);
        final TiePointGrid t1 = new TiePointGrid("t1", 10, 20, 0, 0, 10, 10, new float[10 * 20]);
        final TiePointGrid t2 = new TiePointGrid("t2", 10, 20, 0, 0, 10, 10, new float[10 * 20]);
        final Band b1 = new Band("b1", ProductData.TYPE_INT8, sourceWidth, sourceHeight);
        final Band b2 = new Band("b2", ProductData.TYPE_UINT16, sourceWidth, sourceHeight);
        final Band b3 = new Band("b3", ProductData.TYPE_FLOAT32, sourceWidth, sourceHeight);
        source.addTiePointGrid(t1);
        source.addTiePointGrid(t2);
        source.addBand(b1);
        source.addBand(b2);
        source.addBand(b3);

        final Map<Band, RasterDataNode> bandMapping = new HashMap<Band, RasterDataNode>();

        // Should NOT copy any bands because source is NOT geo-coded
        final Product target1 = new Product("target1", "test", 300, 400);
        ProductUtils.copyBandsForGeomTransform(source, target1, 0, bandMapping);
        assertEquals(0, target1.getNumBands());
        assertEquals(0, bandMapping.size());

        // Geo-code source
        source.setSceneGeoCoding(new DGeoCoding());

        // Should copy bands because source is now geo-coded
        final Product target2 = new Product("dest", "test", 300, 400);
        ProductUtils.copyBandsForGeomTransform(source, target2, 0, bandMapping);
        assertEquals(3, target2.getNumBands());
        assertNotNull(target2.getBand("b1"));
        assertNotNull(target2.getBand("b2"));
        assertNotNull(target2.getBand("b3"));
        assertEquals(3, bandMapping.size());
        assertSame(b1, bandMapping.get(target2.getBand("b1")));
        assertSame(b2, bandMapping.get(target2.getBand("b2")));
        assertSame(b3, bandMapping.get(target2.getBand("b3")));

        bandMapping.clear();

        // Should copy tie-point grids as well because flag has been set
        final Product target3 = new Product("dest", "test", 300, 400);
        ProductUtils.copyBandsForGeomTransform(source, target3, true, 0, bandMapping);
        assertEquals(5, target3.getNumBands());
        assertNotNull(target3.getBand("t1"));
        assertNotNull(target3.getBand("t2"));
        assertNotNull(target3.getBand("b1"));
        assertNotNull(target3.getBand("b2"));
        assertNotNull(target3.getBand("b3"));
        assertEquals(5, bandMapping.size());
        assertSame(t1, bandMapping.get(target3.getBand("t1")));
        assertSame(t2, bandMapping.get(target3.getBand("t2")));
        assertSame(b1, bandMapping.get(target3.getBand("b1")));
        assertSame(b2, bandMapping.get(target3.getBand("b2")));
        assertSame(b3, bandMapping.get(target3.getBand("b3")));
    }


    @Test
    public void testComputeSourcePixelCoordinates() {
        final PixelPos[] pixelCoords = ProductUtils.computeSourcePixelCoordinates(new ProductUtilsTest.SGeoCoding(),
                                                                                  2, 2,
                                                                                  new ProductUtilsTest.DGeoCoding(),
                                                                                  new Rectangle(0, 0, 3, 2));

        assertEquals(3 * 2, pixelCoords.length);

        testCoord(pixelCoords, 0, 0.5f, 0.5f);
        testCoord(pixelCoords, 1, 1.5f, 0.5f);
        assertNull(pixelCoords[2]);
        testCoord(pixelCoords, 3, 0.5f, 1.5f);
        testCoord(pixelCoords, 4, 1.5f, 1.5f);
        assertNull(pixelCoords[5]);
    }

    @Test
    public void testComputeMinMaxY() {
        // call with null
        try {
            ProductUtils.computeMinMaxY(null);
            fail();
        } catch (IllegalArgumentException ignored) {
        }

        // call with pixel positions array width null elements
        final PixelPos[] pixelPositions = new PixelPos[5];

        assertNull(ProductUtils.computeMinMaxY(pixelPositions));

        // call with pixel positions array width one element
        pixelPositions[0] = new PixelPos(2.23f, 3.87f);
        double[] minMaxEqual = ProductUtils.computeMinMaxY(pixelPositions);

        assertEquals(2, minMaxEqual.length);
        assertEquals(minMaxEqual[0], minMaxEqual[1], 1.0e-5f);

        // call with full pixel positions array
        pixelPositions[1] = new PixelPos(3, 3.34f);
        pixelPositions[2] = new PixelPos(4, 4.54f);
        pixelPositions[3] = null;
        pixelPositions[4] = new PixelPos(6, 6.36f);

        double[] minMax = ProductUtils.computeMinMaxY(pixelPositions);

        assertEquals(2, minMax.length);
        assertEquals(3.34f, minMax[0], 1.0e-5f);
        assertEquals(6.36f, minMax[1], 1.0e-5f);
    }

    @Test
    public void testCreateRectBoundary_usePixelCenter_false() {
        final boolean usePixelCenter = false;
        final PixelPos[] rectBoundary = ProductUtils.createRectBoundary(new Rectangle(2, 3, 15, 20), 7,
                                                                        usePixelCenter);
        assertEquals(12, rectBoundary.length);
        assertEquals(new PixelPos(2, 3), rectBoundary[0]);
        assertEquals(new PixelPos(9, 3), rectBoundary[1]);
        assertEquals(new PixelPos(16, 3), rectBoundary[2]);
        assertEquals(new PixelPos(17, 3), rectBoundary[3]);
        assertEquals(new PixelPos(17, 10), rectBoundary[4]);
        assertEquals(new PixelPos(17, 17), rectBoundary[5]);
        assertEquals(new PixelPos(17, 23), rectBoundary[6]);
        assertEquals(new PixelPos(16, 23), rectBoundary[7]);
        assertEquals(new PixelPos(9, 23), rectBoundary[8]);
        assertEquals(new PixelPos(2, 23), rectBoundary[9]);
        assertEquals(new PixelPos(2, 17), rectBoundary[10]);
        assertEquals(new PixelPos(2, 10), rectBoundary[11]);
    }

    @Test
    public void testCreateRectBoundary_usePixelCenter_true() {
        final boolean usePixelCenter = true;
        final PixelPos[] rectBoundary = ProductUtils.createRectBoundary(new Rectangle(2, 3, 15, 20), 7, usePixelCenter);
        assertEquals(10, rectBoundary.length);
        assertEquals(new PixelPos(2.5f, 3.5f), rectBoundary[0]);
        assertEquals(new PixelPos(9.5f, 3.5f), rectBoundary[1]);
        assertEquals(new PixelPos(16.5f, 3.5f), rectBoundary[2]);
        assertEquals(new PixelPos(16.5f, 10.5f), rectBoundary[3]);
        assertEquals(new PixelPos(16.5f, 17.5f), rectBoundary[4]);
        assertEquals(new PixelPos(16.5f, 22.5f), rectBoundary[5]);
        assertEquals(new PixelPos(9.5f, 22.5f), rectBoundary[6]);
        assertEquals(new PixelPos(2.5f, 22.5f), rectBoundary[7]);
        assertEquals(new PixelPos(2.5f, 17.5f), rectBoundary[8]);
        assertEquals(new PixelPos(2.5f, 10.5f), rectBoundary[9]);
    }

    @Test
    public void testCreateRectBoundary_without_usePixelCenter_Parameter() {
        final PixelPos[] rectBoundary = ProductUtils.createRectBoundary(new Rectangle(2, 3, 15, 20), 7);
        assertEquals(10, rectBoundary.length);
        assertEquals(new PixelPos(2.5f, 3.5f), rectBoundary[0]);
        assertEquals(new PixelPos(9.5f, 3.5f), rectBoundary[1]);
        assertEquals(new PixelPos(16.5f, 3.5f), rectBoundary[2]);
        assertEquals(new PixelPos(16.5f, 10.5f), rectBoundary[3]);
        assertEquals(new PixelPos(16.5f, 17.5f), rectBoundary[4]);
        assertEquals(new PixelPos(16.5f, 22.5f), rectBoundary[5]);
        assertEquals(new PixelPos(9.5f, 22.5f), rectBoundary[6]);
        assertEquals(new PixelPos(2.5f, 22.5f), rectBoundary[7]);
        assertEquals(new PixelPos(2.5f, 17.5f), rectBoundary[8]);
        assertEquals(new PixelPos(2.5f, 10.5f), rectBoundary[9]);
    }

    @Test
    public void testCopyFlagCoding() {
        final FlagCoding originalFlagCoding = new FlagCoding("sesame street character flags");
        originalFlagCoding.addFlag("erni", 1, "erni flag");
        originalFlagCoding.addFlag("bert", 2, "bert flag");
        originalFlagCoding.addFlag("bibo", 4, "bert flag");

        final Product product = new Product("S", "S", 0, 0);
        ProductUtils.copyFlagCoding(originalFlagCoding, product);

        final ProductNodeGroup<FlagCoding> flagCodingGroup = product.getFlagCodingGroup();
        assertNotNull(flagCodingGroup);
        assertEquals(1, flagCodingGroup.getNodeCount());

        final FlagCoding actualFlagCoding = flagCodingGroup.get("sesame street character flags");
        assertNotNull(actualFlagCoding);
        assertNotSame(originalFlagCoding, actualFlagCoding);

        assertMetadataAttributeEqualityInt(originalFlagCoding.getFlag("erni"), actualFlagCoding.getFlag("erni"));
        assertMetadataAttributeEqualityInt(originalFlagCoding.getFlag("bert"), actualFlagCoding.getFlag("bert"));
        assertMetadataAttributeEqualityInt(originalFlagCoding.getFlag("bibo"), actualFlagCoding.getFlag("bibo"));

        // try to copy the same coding a second time
        ProductUtils.copyFlagCoding(originalFlagCoding, product);
        assertEquals(1, flagCodingGroup.getNodeCount());
    }

    @Test
    public void testCopyIndexCoding() {
        final IndexCoding originalIndexCoding = new IndexCoding("sesame street characters");
        originalIndexCoding.addIndex("erni", 0, "erni character");
        originalIndexCoding.addIndex("bert", 1, "bert character");

        final Product product = new Product("S", "S", 0, 0);
        ProductUtils.copyIndexCoding(originalIndexCoding, product);

        final ProductNodeGroup<IndexCoding> indexCodingGroup = product.getIndexCodingGroup();
        assertNotNull(indexCodingGroup);
        assertEquals(1, indexCodingGroup.getNodeCount());


        final IndexCoding actualIndexCoding = indexCodingGroup.get("sesame street characters");
        assertNotNull(actualIndexCoding);
        assertNotSame(originalIndexCoding, actualIndexCoding);

        assertMetadataAttributeEqualityInt(originalIndexCoding.getIndex("erni"), actualIndexCoding.getIndex("erni"));
        assertMetadataAttributeEqualityInt(originalIndexCoding.getIndex("bert"), actualIndexCoding.getIndex("bert"));

        // try to copy the same coding a second time
        ProductUtils.copyIndexCoding(originalIndexCoding, product);
        assertEquals(1, indexCodingGroup.getNodeCount());

    }

    @Test
    public void testCopyGeoCoding_fromRasterToRaster() throws Exception {
        final Product sourceProduct = new DummyProductBuilder().gc(TIE_POINTS).gcOcc(UNIQUE).create();
        final Product targetProduct = new Product("N", "T", sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        final Band sourceBand = sourceProduct.getBand("band_a");
        final Band targetBand = targetProduct.addBand("targetBand", ProductData.TYPE_INT8);
        ProductUtils.copyGeoCoding(sourceBand, targetBand);
        assertNotNull(targetBand.getGeoCoding());
    }

    @Test
    public void testCopyGeoCoding_fromRasterToProduct() throws Exception {
        final Product sourceProduct = new DummyProductBuilder().gc(TIE_POINTS).gcOcc(UNIQUE).create();
        final Product targetProduct = new Product("N", "T", sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        final Band sourceBand = sourceProduct.getBand("band_a");
        ProductUtils.copyGeoCoding(sourceBand, targetProduct);
        assertNotNull(targetProduct.getSceneGeoCoding());
    }

    @Test
    public void testCopyGeoCoding_fromProductToRaster() throws Exception {
        final Product sourceProduct = new DummyProductBuilder().gc(TIE_POINTS).gcOcc(UNIQUE).create();
        final Product targetProduct = new Product("N", "T", sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        final Band targetBand = targetProduct.addBand("targetBand", ProductData.TYPE_INT8);
        ProductUtils.copyGeoCoding(sourceProduct, targetBand);
        assertNotNull(targetBand.getGeoCoding());
    }

    @Test
    public void testCopyImageGeometry_TargetBandTooSmall() throws Exception {
        final Product sourceProduct = new DummyProductBuilder().gc(TIE_POINTS).gcOcc(UNIQUE).create();
        final Product targetProduct = new Product("N", "T");

        final Band sourceBand = sourceProduct.getBand("band_a");
        final Band tooSmallBand = new Band("tooSmallBand", ProductData.TYPE_INT8,
                                           sourceBand.getRasterWidth() - 10, sourceBand.getRasterHeight() - 10);
        targetProduct.addBand(tooSmallBand);
        ProductUtils.copyImageGeometry(sourceBand, tooSmallBand, true);
        assertNull(tooSmallBand.getGeoCoding()); // GC could not be transferred; raster must be equal in size
    }

    @Test
    public void testCopyImageGeometry_TargetBandWithGC() throws Exception {
        final Product sourceProduct = new DummyProductBuilder().gc(TIE_POINTS).gcOcc(UNIQUE).create();
        final Product targetProduct = new Product("N", "T");
        final Band sourceBand = sourceProduct.getBand("band_a");
        final Band hasAlreadyGcBand = new Band("hasAlreadyGcBand", ProductData.TYPE_INT8,
                                               sourceBand.getRasterWidth(), sourceBand.getRasterHeight());
        final CrsGeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                                        hasAlreadyGcBand.getRasterWidth(), hasAlreadyGcBand.getRasterHeight(),
                                                        10, 45, 0.3, 0.3, 0.0, 0.0);
        hasAlreadyGcBand.setGeoCoding(geoCoding);
        targetProduct.addBand(hasAlreadyGcBand);
        ProductUtils.copyImageGeometry(sourceBand, hasAlreadyGcBand, true);
        assertTrue(hasAlreadyGcBand.getGeoCoding() instanceof TiePointGeoCoding); // has replaced the GC
    }

    @Test
    public void testCopyImageGeometry_CopyByReference() throws Exception {
        final Product sourceProduct = new DummyProductBuilder().gc(TIE_POINTS).gcOcc(UNIQUE).create();
        final Band sourceBand = sourceProduct.getBand("band_a");
        final Product targetProduct = new Product("N", "T", sourceBand.getRasterWidth(), sourceBand.getRasterHeight());
        final CrsGeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                                        targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight(),
                                                        10, 45, 0.3, 0.3, 0.0, 0.0);
        targetProduct.setSceneGeoCoding(geoCoding);
        final VectorDataNode newVectorNode = new VectorDataNode("newVectorNode", Placemark.createGeometryFeatureType());
        final Mask mask = targetProduct.addMask("NewMask", newVectorNode, "LoremIpsum", Color.BLUE, 0.3);
        ProductUtils.copyImageGeometry(sourceBand, mask, false);
        assertSame(sourceBand.getGeoCoding(), mask.getGeoCoding()); // copied by reference
        assertEquals(sourceBand.getImageToModelTransform(), mask.getImageToModelTransform());
    }

    @Test
    public void testCopyImageGeometry_Copy() throws Exception {
        final Product sourceProduct2 = new DummyProductBuilder().sizeOcc(MULTI).i2m(SET_PROPORTIONAL).gc(PER_PIXEL).gcOcc(VARIOUS).create();
        final Product targetProduct = new Product("N", "T");
        final Band sourceBand2 = sourceProduct2.getBand("band_a_2");
        assertNotEquals(new AffineTransform(), sourceBand2.getImageToModelTransform());
        final Band band = new Band("band", ProductData.TYPE_INT16, sourceBand2.getRasterWidth(), sourceBand2.getRasterHeight());
        targetProduct.addBand(band);
        ProductUtils.copyImageGeometry(sourceBand2, band, false);
        assertSame(sourceBand2.getGeoCoding(), band.getGeoCoding()); // copied by reference
        assertEquals(sourceBand2.getImageToModelTransform(), band.getImageToModelTransform());
    }

    @Test
    public void testGetScanLineTime_1_pixel() throws Exception {
        Product product = new Product("name", "type", 1, 1);
        ProductData.UTC startTime = ProductData.UTC.parse("01-01-2010", "dd-MM-yyyy");
        ProductData.UTC endTime = ProductData.UTC.parse("02-01-2010", "dd-MM-yyyy");
        product.setStartTime(startTime);
        product.setEndTime(endTime);
        double startTimeMJD = startTime.getMJD();
        assertEquals(startTimeMJD, ProductUtils.getScanLineTime(product, 0).getMJD(), 1E-6);
        assertNotSame(startTime, ProductUtils.getScanLineTime(product, 0));
    }

    @Test
    public void testGetScanLineTime() throws Exception {
        Product product = new Product("name", "type", 10, 10);
        ProductData.UTC startTime = ProductData.UTC.parse("01-01-2010", "dd-MM-yyyy");
        ProductData.UTC endTime = ProductData.UTC.parse("02-01-2010", "dd-MM-yyyy");
        product.setStartTime(startTime);
        product.setEndTime(endTime);
        double startTimeMJD = startTime.getMJD();
        double endTimeMJD = endTime.getMJD();
        assertEquals(startTimeMJD, ProductUtils.getScanLineTime(product, 0).getMJD(), 1E-6);
        assertEquals(endTimeMJD, ProductUtils.getScanLineTime(product, 9).getMJD(), 1E-6);
    }

    @Test
    public void testGetPixelScanTime() throws Exception {
        Product product = new Product("name", "type", 10, 10);
        ProductData.UTC startTime = ProductData.UTC.parse("01-01-2010", "dd-MM-yyyy");
        ProductData.UTC endTime = ProductData.UTC.parse("02-01-2010", "dd-MM-yyyy");
        Band dummyBand = product.addBand("dummy", ProductData.TYPE_INT8);
        double startTimeMJD = startTime.getMJD();
        double endTimeMJD = endTime.getMJD();
        dummyBand.setTimeCoding(new LineTimeCoding(dummyBand.getRasterHeight(), startTimeMJD, endTimeMJD));

        assertEquals(startTimeMJD, ProductUtils.getPixelScanTime(dummyBand, 0, 0).getMJD(), 1E-6);
        assertEquals(endTimeMJD, ProductUtils.getPixelScanTime(dummyBand, 0, 9).getMJD(), 1E-6);
    }

    @Test
    public void testCopyMetadata() {
        try {
            ProductUtils.copyMetadata((Product) null, null);
            fail();
        } catch (NullPointerException ignored) {
        }
        try {
            ProductUtils.copyMetadata((MetadataElement) null, null);
            fail();
        } catch (NullPointerException ignored) {
        }
        try {
            ProductUtils.copyMetadata(new MetadataElement("source"), null);
            fail();
        } catch (NullPointerException ignored) {
        }
        try {
            ProductUtils.copyMetadata(null, new MetadataElement("target"));
            fail();
        } catch (NullPointerException ignored) {
        }

        final MetadataElement source = new MetadataElement("source");
        final FlagCoding sourceChild = new FlagCoding("child");
        sourceChild.addFlag("a", 1, "condition a is true");
        sourceChild.addFlag("b", 2, "condition b is true");
        source.addElement(sourceChild);

        final MetadataElement target = new MetadataElement("target");
        ProductUtils.copyMetadata(source, target);

        final MetadataElement targetChild = target.getElement("child");
        assertNotSame(sourceChild, targetChild);
        assertNotSame(sourceChild.getAttribute("a"), targetChild.getAttribute("a"));
        assertNotSame(sourceChild.getAttribute("b"), targetChild.getAttribute("b"));
        assertNotSame(sourceChild.getAttribute("a").getData(), targetChild.getAttribute("a").getData());
        assertNotSame(sourceChild.getAttribute("b").getData(), targetChild.getAttribute("b").getData());

        assertNotNull(targetChild.getAttribute("a"));
        assertNotNull(targetChild.getAttribute("b"));

        assertMetadataAttributeEqualityInt(sourceChild.getFlag("a"), targetChild.getAttribute("a"));
        assertMetadataAttributeEqualityInt(sourceChild.getFlag("b"), targetChild.getAttribute("b"));
    }

    @Test
    public void testCreateGeoBoundary() {
        final GeoPos[] geoPoses = ProductUtils.createGeoBoundary(createTestProduct(), null, 20, false);
        for (int i = 0; i < geoPoses.length; i++) {
            GeoPos geoPos = geoPoses[i];
            assertTrue(String.format("geoPos at <%d> is invalid", i), geoPos.isValid());
        }
    }

    @Test
    public void testAreRastersCompatible() {
        final Band band1 = new Band("band1", ProductData.TYPE_INT8, 16, 16);
        final Band band2 = new Band("band2", ProductData.TYPE_INT8, 8, 8);
        final TiePointGrid grid = new TiePointGrid("grid", 2, 2, 0, 0, 15, 15, new float[]{0f, 0f, 0f, 0f});

        assertEquals(true, ProductUtils.areRastersEqualInSize());
        assertEquals(true, ProductUtils.areRastersEqualInSize(band1));
        assertEquals(true, ProductUtils.areRastersEqualInSize(band2));
        assertEquals(true, ProductUtils.areRastersEqualInSize(grid));
        assertEquals(false, ProductUtils.areRastersEqualInSize(band1, band2));
        assertEquals(true, ProductUtils.areRastersEqualInSize(band1, grid));
        assertEquals(false, ProductUtils.areRastersEqualInSize(band2, grid));
    }

    @Test
    public void testAreRastersCompatible_ReferencedByName() {
        final Product p = new Product("myProduct", "type", 16, 16);
        final Band band1 = new Band("band1", ProductData.TYPE_INT8, 16, 16);
        final Band band2 = new Band("band2", ProductData.TYPE_INT8, 8, 8);
        final Band band3 = new Band("band3", ProductData.TYPE_INT8, 8, 8);
        final TiePointGrid grid = new TiePointGrid("grid", 2, 2, 0, 0, 15, 15, new float[]{0f, 0f, 0f, 0f});
        p.addBand(band1);
        p.addBand(band2);
        p.addBand(band3);
        p.addTiePointGrid(grid);

        assertEquals(true, ProductUtils.areRastersEqualInSize(p));
        assertEquals(true, ProductUtils.areRastersEqualInSize(p, "band1"));
        assertEquals(true, ProductUtils.areRastersEqualInSize(p, "band2"));
        assertEquals(true, ProductUtils.areRastersEqualInSize(p, "band3"));
        assertEquals(true, ProductUtils.areRastersEqualInSize(p, "grid"));
        assertEquals(true, ProductUtils.areRastersEqualInSize(p, "band1", "grid"));
        assertEquals(false, ProductUtils.areRastersEqualInSize(p, "band2", "grid"));
        assertEquals(false, ProductUtils.areRastersEqualInSize(p, "band3", "grid"));
        assertEquals(false, ProductUtils.areRastersEqualInSize(p, "band2", "band3", "grid"));
        assertEquals(false, ProductUtils.areRastersEqualInSize(p, "band1", "band2", "grid"));
        assertEquals(false, ProductUtils.areRastersEqualInSize(p, "band1", "band3", "grid"));
        assertEquals(false, ProductUtils.areRastersEqualInSize(p, "band1", "band2", "band3"));
        try {
            ProductUtils.areRastersEqualInSize(p, "band1", "dvfgzfj");
            fail("Exception expected");
        } catch (IllegalArgumentException iae) {
            assertEquals("dvfgzfj is not part of myProduct", iae.getMessage());
        }
    }

    @Test
    public void testGetSampleAsLong() throws Exception {
        Band uint8 = createTestBand(ProductData.TYPE_UINT8, new byte[]{0, Byte.MAX_VALUE, (byte) (Math.pow(2, 7)), Byte.MIN_VALUE});

        assertEquals(0, ProductUtils.getGeophysicalSampleAsLong(uint8, 0, 0, 0));
        assertEquals(127, ProductUtils.getGeophysicalSampleAsLong(uint8, 1, 0, 0));
        assertEquals(128L, ProductUtils.getGeophysicalSampleAsLong(uint8, 2, 0, 0));
        assertEquals(128, ProductUtils.getGeophysicalSampleAsLong(uint8, 3, 0, 0));

        Band uint16 = createTestBand(ProductData.TYPE_UINT16, new short[]{0, Short.MAX_VALUE, (short) (Math.pow(2, 15)), Short.MIN_VALUE});

        assertEquals(0, ProductUtils.getGeophysicalSampleAsLong(uint16, 0, 0, 0));
        assertEquals(32767, ProductUtils.getGeophysicalSampleAsLong(uint16, 1, 0, 0));
        assertEquals(32768L, ProductUtils.getGeophysicalSampleAsLong(uint16, 2, 0, 0));
        assertEquals(32768, ProductUtils.getGeophysicalSampleAsLong(uint16, 3, 0, 0));

        Band uint32 = createTestBand(ProductData.TYPE_UINT32, new int[]{0, Integer.MAX_VALUE, (int) ((long)(Math.pow(2, 31)) & 0xFFFFFFFFL), Integer.MIN_VALUE});

        assertEquals(0, ProductUtils.getGeophysicalSampleAsLong(uint32, 0, 0, 0));
        assertEquals(2147483647, ProductUtils.getGeophysicalSampleAsLong(uint32, 1, 0, 0));
        assertEquals(2147483648L, ProductUtils.getGeophysicalSampleAsLong(uint32, 2, 0, 0));
        assertEquals(2147483648L, ProductUtils.getGeophysicalSampleAsLong(uint32, 3, 0, 0));

    }

    @Test
    public void testCopyVirtualBand_preserveSize() throws Exception {
        Product target = new Product("N", "T", 100, 150);
        VirtualBand vb = new VirtualBand("vb", ProductData.TYPE_FLOAT32, 40, 30, "1");
        VirtualBand newVB = ProductUtils.copyVirtualBand(target, vb, "newVB");
        assertEquals(40, newVB.getRasterWidth());
        assertEquals(30, newVB.getRasterHeight());
    }

    @Test
    public void testCopyVirtualBand_adaptSize() throws Exception {
        Product target = new Product("N", "T", 100, 150);
        VirtualBand vb = new VirtualBand("vb", ProductData.TYPE_FLOAT32, 40, 30, "1");
        VirtualBand newVB = ProductUtils.copyVirtualBand(target, vb, "newVB", true);
        assertEquals(100, newVB.getRasterWidth());
        assertEquals(150, newVB.getRasterHeight());
    }

    private Band createTestBand(int dataType, Object data) {
        if (data.getClass().isArray()) {
            int length = Array.getLength(data);
            Band band = new Band(ProductData.getTypeString(dataType), dataType, length, 1);
            band.setData(ProductData.createInstance(dataType, data));
            return band;
        } else {
            throw new RuntimeException("Parameter 'data' must be of type array");
        }
    }

    public static class SGeoCoding implements GeoCoding {

        @Override
        public boolean isCrossingMeridianAt180() {
            return false;
        }

        @Override
        public Datum getDatum() {
            return Datum.WGS_84;
        }

        @Override
        public boolean canGetPixelPos() {
            return true;
        }

        @Override
        public boolean canGetGeoPos() {
            return false;
        }

        @Override
        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            if (pixelPos == null) {
                pixelPos = new PixelPos();
            }
            pixelPos.x = geoPos.lon;
            pixelPos.y = geoPos.lat;
            return pixelPos;
        }

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            return geoPos;
        }

        @Override
        public void dispose() {
        }

        @Override
        public CoordinateReferenceSystem getMapCRS() {
            return null;
        }

        @Override
        public CoordinateReferenceSystem getImageCRS() {
            return null;
        }

        @Override
        public CoordinateReferenceSystem getGeoCRS() {
            return null;
        }

        @Override
        public MathTransform getImageToMapTransform() {
            return null;
        }
    }

    public static class DGeoCoding implements GeoCoding {

        @Override
        public boolean isCrossingMeridianAt180() {
            return true;
        }

        @Override
        public Datum getDatum() {
            return Datum.WGS_84;
        }

        @Override
        public boolean canGetPixelPos() {
            return false;
        }

        @Override
        public boolean canGetGeoPos() {
            return true;
        }

        @Override
        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            return pixelPos;
        }

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            if (geoPos == null) {
                geoPos = new GeoPos();
            }
            geoPos.lon = pixelPos.x;
            geoPos.lat = pixelPos.y;
            return geoPos;
        }

        @Override
        public void dispose() {
        }

        @Override
        public CoordinateReferenceSystem getImageCRS() {
            return null;
        }

        @Override
        public CoordinateReferenceSystem getMapCRS() {
            return null;
        }

        @Override
        public CoordinateReferenceSystem getGeoCRS() {
            return null;
        }

        @Override
        public MathTransform getImageToMapTransform() {
            return null;
        }

    }

    private static void assertMetadataAttributeEqualityInt(MetadataAttribute expected, MetadataAttribute actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getData().getElemInt(), actual.getData().getElemInt());
        assertEquals(expected.getDescription(), actual.getDescription());
    }

    private static void testCoord(final PixelPos[] pixelCoords, final int i, final float x, final float y) {
        assertNotNull(pixelCoords[i]);
        assertEquals(x, pixelCoords[i].x, EPS);
        assertEquals(y, pixelCoords[i].y, EPS);
    }

    private void assertNormalizing(final GeoPos[] boundary, final int expectedNormalizing,
                                   final GeoPos[] expected) {
        final int normalized = ProductUtils.normalizeGeoPolygon(boundary);
        assertEquals(expectedNormalizing, normalized);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("at index " + i, expected[i], boundary[i]);
        }
    }

    private static GeoPos[] createPositiveRotationGeoPolygon(int lonOffset) {
        return new GeoPos[]{
                new GeoPos(-60, -40 + lonOffset),
                new GeoPos(-80, +20 + lonOffset),
                new GeoPos(-40, +60 + lonOffset),
                new GeoPos(+20, +120 + lonOffset),
                new GeoPos(+60, +40 + lonOffset),
                new GeoPos(+80, -20 + lonOffset),
                new GeoPos(+40, -60 + lonOffset),
                new GeoPos(-20, -120 + lonOffset)
        };
    }

    private static GeoPos[] createNegativeRotationGeoPolygon(int lonOffset) {
        return new GeoPos[]{
                new GeoPos(-60, -40 + lonOffset),
                new GeoPos(-20, -120 + lonOffset),
                new GeoPos(+40, -60 + lonOffset),
                new GeoPos(+80, -20 + lonOffset),
                new GeoPos(+60, +40 + lonOffset),
                new GeoPos(+20, +120 + lonOffset),
                new GeoPos(-40, +60 + lonOffset),
                new GeoPos(-80, +20 + lonOffset)
        };
    }

    private static GeoPos[] createPositiveRotationDualMeridianGeoPolygon() {
        return new GeoPos[]{
                new GeoPos(20, -160),
                new GeoPos(30, -180),
                new GeoPos(30, 178),
                new GeoPos(40, 10),
                new GeoPos(50, -70),
                new GeoPos(70, -170),
                new GeoPos(80, 150),
                new GeoPos(60, 150),
                new GeoPos(57, 170),
                new GeoPos(50, -140),
                new GeoPos(30, 30),
                new GeoPos(25, 170),
                new GeoPos(10, -165),
        };
    }

    private void shiftGeoPolygon(final GeoPos[] geoPositions, final int lonOffset) {
        for (final GeoPos geoPosition : geoPositions) {
            geoPosition.lon += lonOffset;
        }
    }

    private static Product createTestProduct() {
        final float[] longitudes = new float[]{
                9.512839f, 9.690325f, 9.867694f, 10.044944f, 10.2220745f, 10.399086f, 9.475174f, 9.65231f, 9.829329f,
                10.006232f, 10.183016f, 10.359681f, 9.437564f, 9.614353f, 9.791027f, 9.967584f, 10.144024f, 10.320345f,
                9.40001f, 9.576455f, 9.752785f, 9.929f, 10.105098f, 10.281079f, 9.362511f, 9.538614f, 9.714604f,
                9.890479f, 10.066238f, 10.241882f, 9.325066f, 9.50083f, 9.676482f, 9.85202f, 10.027444f, 10.202752f,
                9.287674f, 9.463103f, 9.638419f, 9.813623f, 9.988713f, 10.163689f, 9.250335f, 9.42543f, 9.600414f,
                9.775286f, 9.950046f, 10.124692f, 9.213048f, 9.387812f, 9.562466f, 9.737009f, 9.911441f, 10.08576f
        };

        final float[] latitudes = new float[]{
                34.254475f, 34.22662f, 34.198513f, 34.170143f, 34.14152f, 34.11264f, 34.088284f, 34.060417f,
                34.032295f, 34.00392f, 33.975292f, 33.946407f, 33.922085f, 33.894207f, 33.866077f, 33.837692f,
                33.809055f, 33.780163f, 33.755875f, 33.72799f, 33.69985f, 33.67145f, 33.642807f,
                33.61391f, 33.589664f, 33.56176f, 33.53361f, 33.505207f, 33.476555f, 33.447655f, 33.423443f,
                33.395527f, 33.367367f, 33.338955f, 33.310295f, 33.281387f, 33.257214f, 33.229286f, 33.201115f,
                33.172695f, 33.144028f, 33.115112f, 33.090977f, 33.06304f, 33.034855f, 33.006424f, 32.977753f,
                32.948833f, 32.924736f, 32.896786f, 32.868587f, 32.84015f, 32.811466f, 32.78254f
        };

        final Product product = new Product("testName", "TEST_TYPE", 50, 100);

        final TiePointGrid lonGrid = new TiePointGrid("longitudes", 6, 9, -3.5f, -7.5f, 16, 16, longitudes);
        product.addTiePointGrid(lonGrid);
        final TiePointGrid latGrid = new TiePointGrid("latitudes", 6, 9, -3.5f, -7.5f, 16, 16, latitudes);
        product.addTiePointGrid(latGrid);
        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid);
        product.setSceneGeoCoding(tpGeoCoding);
        return product;
    }

}

