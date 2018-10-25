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

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultEllipsoidalCS;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CrsGeoCodingTest {

    private CrsGeoCoding srcGeoCoding;
    private Scene destScene;
    private Scene srcScene;

    @Before
    public void setup() throws Exception {
        final Rectangle imageBounds = new Rectangle(10, 20);
        srcGeoCoding = createCrsGeoCoding(imageBounds);

        final Band srcNode = new Band("srcDummy", ProductData.TYPE_INT8, 10, 20);
        srcNode.setGeoCoding(srcGeoCoding);
        srcScene = SceneFactory.createScene(srcNode);

        final Band destNode = new Band("destDummy", ProductData.TYPE_INT8, 10, 20);
        destScene = SceneFactory.createScene(destNode);
    }

    @Test
    public void testTransferGeoCodingWithoutSubset() {
        final boolean returnValue = srcScene.transferGeoCodingTo(destScene, null);
        assertTrue(returnValue);

        final GeoCoding destGeoCoding = destScene.getGeoCoding();
        assertNotNull(destGeoCoding);
        assertNotSame(srcGeoCoding, destGeoCoding);

        assertEquals(srcGeoCoding.getDatum(), destGeoCoding.getDatum());
        assertEquals(srcGeoCoding.getMapCRS(), destGeoCoding.getMapCRS());
        assertEquals(srcGeoCoding.getGeoCRS(), destGeoCoding.getGeoCRS());

        assertEquals(srcGeoCoding.getGeoPos(new PixelPos(3.5f, 0.5f), null),
                     destGeoCoding.getGeoPos(new PixelPos(3.5f, 0.5f), null));
    }

    @Test
    public void testTransferGeoCodingWithSubset_Region() {
        final ProductSubsetDef subsetDef = new ProductSubsetDef("subset");
        subsetDef.setRegion(2, 2, 4, 4);
        final boolean transfered = srcScene.transferGeoCodingTo(destScene, subsetDef);
        assertTrue(transfered);

        final GeoCoding destGeoCoding = destScene.getGeoCoding();
        assertNotNull(destGeoCoding);
        assertNotSame(srcGeoCoding, destGeoCoding);

        assertEquals(srcGeoCoding.getDatum(), destGeoCoding.getDatum());
        assertEquals(srcGeoCoding.getMapCRS(), destGeoCoding.getMapCRS());

        // position (3,3) in source equals (1,1) in dest
        comparePixelPos(destGeoCoding, new PixelPos(3, 3), new PixelPos(1, 1));
    }

    @Test
    public void testTransferGeoCodingWithSubset_Subsampling() {
        final ProductSubsetDef subsetDef = new ProductSubsetDef("subset");
        subsetDef.setSubSampling(2, 4);
        final boolean transfered = srcScene.transferGeoCodingTo(destScene, subsetDef);
        assertTrue(transfered);

        final GeoCoding destGeoCoding = destScene.getGeoCoding();
        assertNotNull(destGeoCoding);
        assertNotSame(srcGeoCoding, destGeoCoding);

        assertEquals(srcGeoCoding.getDatum(), destGeoCoding.getDatum());
        assertEquals(srcGeoCoding.getMapCRS(), destGeoCoding.getMapCRS());
        assertEquals(srcGeoCoding.getGeoCRS(), destGeoCoding.getGeoCRS());

        comparePixelPos(destGeoCoding, new PixelPos(0, 0), new PixelPos(0, 0));
        comparePixelPos(destGeoCoding, new PixelPos(8, 0), new PixelPos(4, 0));
        comparePixelPos(destGeoCoding, new PixelPos(8, 16), new PixelPos(4, 4));
        comparePixelPos(destGeoCoding, new PixelPos(0, 16), new PixelPos(0, 4));
    }

    @Test
    public void testTransferGeoCodingWithSubset_SubsamplingAndRegion() {
        final ProductSubsetDef subsetDef = new ProductSubsetDef("subset");
        subsetDef.setRegion(2, 2, 8, 8);
        subsetDef.setSubSampling(2, 2);
        final boolean transfered = srcScene.transferGeoCodingTo(destScene, subsetDef);
        assertTrue(transfered);

        final GeoCoding destGeoCoding = destScene.getGeoCoding();
        assertNotNull(destGeoCoding);
        assertNotSame(srcGeoCoding, destGeoCoding);

        assertEquals(srcGeoCoding.getDatum(), destGeoCoding.getDatum());
        assertEquals(srcGeoCoding.getMapCRS(), destGeoCoding.getMapCRS());
        assertEquals(srcGeoCoding.getGeoCRS(), destGeoCoding.getGeoCRS());

        comparePixelPos(destGeoCoding, new PixelPos(2, 2), new PixelPos(0, 0));
        comparePixelPos(destGeoCoding, new PixelPos(10, 2), new PixelPos(4, 0));
        comparePixelPos(destGeoCoding, new PixelPos(10, 10), new PixelPos(4, 4));
        comparePixelPos(destGeoCoding, new PixelPos(2, 10), new PixelPos(0, 4));
    }

    @Test
    public void testCrossing180() throws Exception {
        final Rectangle imageBounds = new Rectangle(10, 20);
        srcGeoCoding = createCrsGeoCodingCross180(imageBounds);

        assertTrue(srcGeoCoding.isCrossingMeridianAt180());

        final Band srcNode = new Band("srcDummy", ProductData.TYPE_INT8, 10, 20);
        srcNode.setGeoCoding(srcGeoCoding);
        srcScene = SceneFactory.createScene(srcNode);

        final ProductSubsetDef subsetDef = new ProductSubsetDef("subset");
        subsetDef.setRegion(2, 2, 8, 8);
        subsetDef.setSubSampling(2, 2);
        boolean transfered = srcScene.transferGeoCodingTo(destScene, subsetDef);
        assertTrue(transfered);

        assertTrue(destScene.getGeoCoding().isCrossingMeridianAt180());

        subsetDef.setRegion(2, 2, 2, 2);
        transfered = srcScene.transferGeoCodingTo(destScene, subsetDef);
        assertTrue(transfered);

        assertFalse(destScene.getGeoCoding().isCrossingMeridianAt180());
    }

    @Test
    public void testWholeWorld() throws Exception {
        final Rectangle imageBounds = new Rectangle(10, 20);
        srcGeoCoding = createCrsGeoCodingCoveringWholeWorld(imageBounds);

        assertFalse(srcGeoCoding.isCrossingMeridianAt180());
    }

    @Test
    public void testCustomSpheroidDatum() throws TransformException, FactoryException, InterruptedException {
        String wkt = "PROJCS[\"MODIS_Sinusoidal\", \n" +
                "  GEOGCS[\"Unknown datum based upon the custom spheroid\", \n" +
                "    DATUM[\"Not specified (based on custom spheroid)\", \n" +
                "      SPHEROID[\"Custom spheroid\", 6371007.181, 0.0]], \n" +
                "    PRIMEM[\"Greenwich\", 0.0], \n" +
                "    UNIT[\"degree\", 0.017453292519943295], \n" +
                "    AXIS[\"Longitude\", EAST], \n" +
                "    AXIS[\"Latitude\", NORTH]], \n" +
                "  PROJECTION[\"Sinusoidal\"], \n" +
                "  PARAMETER[\"central_meridian\", 0.0], \n" +
                "  PARAMETER[\"false_easting\", 0.0], \n" +
                "  PARAMETER[\"false_northing\", 0.0], \n" +
                "  UNIT[\"m\", 1.0], \n" +
                "  AXIS[\"x\", EAST], \n" +
                "  AXIS[\"y\", NORTH]]";
        CoordinateReferenceSystem crs = CRS.parseWKT(wkt);
        CrsGeoCoding geoCoding = new CrsGeoCoding(crs, new Rectangle(10, 10, 10, 10), new AffineTransform());
        DefaultGeographicCRS defaultCrs = (DefaultGeographicCRS) geoCoding.getGeoCRS();
        Ellipsoid wgs84Spheroid = DefaultGeographicCRS.WGS84.getDatum().getEllipsoid();
        Ellipsoid customSpheroid = defaultCrs.getDatum().getEllipsoid();

        assertNotSame(DefaultGeographicCRS.WGS84, defaultCrs);
        assertTrue(wgs84Spheroid.getSemiMinorAxis() != customSpheroid.getSemiMinorAxis());
        assertTrue(wgs84Spheroid.getSemiMajorAxis() != customSpheroid.getSemiMajorAxis());
        assertSame(DefaultEllipsoidalCS.GEODETIC_2D, defaultCrs.getCoordinateSystem());
    }

    @Test
    public void testWgs84() throws TransformException, FactoryException {
        String testedWkt = "GEOGCS[\"WGS 84\",\n" +
                "    DATUM[\"WGS_1984\",\n" +
                "        SPHEROID[\"WGS 84\",6378137,298.257223563,\n" +
                "            AUTHORITY[\"EPSG\",\"7030\"]],\n" +
                "        AUTHORITY[\"EPSG\",\"6326\"]],\n" +
                "    PRIMEM[\"Greenwich\",0,\n" +
                "        AUTHORITY[\"EPSG\",\"8901\"]],\n" +
                "    UNIT[\"degree\",0.01745329251994328,\n" +
                "        AUTHORITY[\"EPSG\",\"9122\"]],\n" +
                "    AUTHORITY[\"EPSG\",\"4326\"]]";

        CoordinateReferenceSystem testedCrs = CRS.parseWKT(testedWkt);
        GeoCoding geoCoding = new CrsGeoCoding(testedCrs, new Rectangle(10, 10, 10, 10), new AffineTransform());
        DefaultGeographicCRS testedDefaultCrs = (DefaultGeographicCRS) geoCoding.getGeoCRS();
        Ellipsoid testedSpheroid = testedDefaultCrs.getDatum().getEllipsoid();
        Ellipsoid wgs84Spheroid = DefaultGeographicCRS.WGS84.getDatum().getEllipsoid();

        assertTrue(wgs84Spheroid.getSemiMinorAxis() == testedSpheroid.getSemiMinorAxis());
        assertTrue(wgs84Spheroid.getSemiMajorAxis() == testedSpheroid.getSemiMajorAxis());
        assertSame(DefaultGeographicCRS.WGS84.getCoordinateSystem(), testedDefaultCrs.getCoordinateSystem());
    }

    @Test
    public void testGetPixels() throws FactoryException, TransformException {
        int numPixels = 10000;
        // SR-ORG:6965
        String wkt = "PROJCS[\"MODIS_Sinusoidal\", \n" +
                     "  GEOGCS[\"Unknown datum based upon the custom spheroid\", \n" +
                     "    DATUM[\"Not specified (based on custom spheroid)\", \n" +
                     "      SPHEROID[\"Custom spheroid\", 6371007.181, 0.0]], \n" +
                     "    PRIMEM[\"Greenwich\", 0.0], \n" +
                     "    UNIT[\"degree\", 0.017453292519943295], \n" +
                     "    AXIS[\"Longitude\", EAST], \n" +
                     "    AXIS[\"Latitude\", NORTH]], \n" +
                     "  PROJECTION[\"Sinusoidal\"], \n" +
                     "  PARAMETER[\"central_meridian\", 0.0], \n" +
                     "  PARAMETER[\"false_easting\", 1000.0], \n" +
                     "  PARAMETER[\"false_northing\", 500000.0], \n" +
                     "  PARAMETER[\"semi_minor\",6371007.0], \n" +
                     "  UNIT[\"m\", 1.0], \n" +
                     "  AXIS[\"x\", EAST], \n" +
                     "  AXIS[\"y\", NORTH]]";
        CoordinateReferenceSystem testedCrs = CRS.parseWKT(wkt);
        CrsGeoCoding geoCoding = new CrsGeoCoding(testedCrs, new Rectangle(100, 100, 100, 100), new AffineTransform());

        double[] latPixels_1 = new double[numPixels];
        double[] latPixels_2 = new double[numPixels];
        double[] latPixels_3 = new double[numPixels];
        double[] lonPixels_1 = new double[numPixels];
        double[] lonPixels_2 = new double[numPixels];
        double[] lonPixels_3 = new double[numPixels];

        GeoPos geoPos = new GeoPos();
        final PixelPos pixelPos = new PixelPos();
        int entryCount = 0;
        for (int y = 0; y < 100; y++) {
            double yp = y + 0.5;
            for (int x = 0; x < 100; x++) {
                pixelPos.setLocation(x + 0.5, yp);
                geoCoding.getGeoPos(pixelPos, geoPos);
                latPixels_3[entryCount] = (float) geoPos.getLat();
                lonPixels_3[entryCount] = (float) geoPos.getLon();
                entryCount++;
            }
        }

        geoCoding.getPixelsOneByOne(0, 0, 100, 100, latPixels_2, lonPixels_2);

        geoCoding.getPixels(0, 0, 100, 100, latPixels_1, lonPixels_1);

        for (int i = 0; i < numPixels; i++) {
            assertEquals(latPixels_1[i], latPixels_2[i], 1e-8);
            assertEquals(latPixels_1[i], latPixels_3[i], 1e-6);
            assertEquals(lonPixels_1[i], lonPixels_2[i], 1e-8);
            assertEquals(lonPixels_1[i], lonPixels_3[i], 1e-6);
        }
    }

    private void comparePixelPos(GeoCoding destGeoCoding, PixelPos pixelPos, PixelPos pixelPos1) {
        GeoPos srcPos = srcGeoCoding.getGeoPos(pixelPos, null);
        GeoPos destPos = destGeoCoding.getGeoPos(pixelPos1, null);
        assertEquals(srcPos, destPos);
    }

    private CrsGeoCoding createCrsGeoCoding(Rectangle imageBounds) throws Exception {
        AffineTransform i2m = new AffineTransform();
        final int northing = 60;
        final int easting = 5;
        i2m.translate(easting, northing);
        final int scaleX = 1;
        final int scaleY = 1;
        i2m.scale(scaleX, -scaleY);
        return new CrsGeoCoding(DefaultGeographicCRS.WGS84, imageBounds, i2m);
    }

    private CrsGeoCoding createCrsGeoCodingCross180(Rectangle imageBounds) throws Exception {
        AffineTransform i2m = new AffineTransform();
        final int northing = 60;
        final int easting = 175;
        i2m.translate(easting, northing);
        final int scaleX = 1;
        final int scaleY = 1;
        i2m.scale(scaleX, -scaleY);
        return new CrsGeoCoding(DefaultGeographicCRS.WGS84, imageBounds, i2m);
    }

    private CrsGeoCoding createCrsGeoCodingCoveringWholeWorld(Rectangle imageBounds) throws Exception {
        AffineTransform i2m = new AffineTransform();
        final int northing = 60;
        final int easting = -180;
        i2m.translate(easting, northing);
        final double scaleX = 360 / imageBounds.getWidth();
        final double scaleY = 1.0;
        i2m.scale(scaleX, -scaleY);
        return new CrsGeoCoding(DefaultGeographicCRS.WGS84, imageBounds, i2m);
    }
}
