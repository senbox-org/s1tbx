package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.*;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class ComponentGeoCodingTest_transferGeoCoding {

    private final int SCENE_WIDTH = 40;
    private final int SCENE_HEIGHT = 50;
    private final int TP_WIDTH = 8;
    private final int TP_HEIGHT = 10;
    private Product srcProduct;

    @Before
    public void setUp() {
        srcProduct = new Product("A", "B", SCENE_WIDTH, SCENE_HEIGHT);
        srcProduct.addTiePointGrid(new TiePointGrid("tpLon", TP_WIDTH, TP_HEIGHT, 0.5, 0.5, 5, 5));
        srcProduct.addTiePointGrid(new TiePointGrid("tpLat", TP_WIDTH, TP_HEIGHT, 0.5, 0.5, 5, 5));
        srcProduct.addBand("Lon", ProductData.TYPE_FLOAT64);
        srcProduct.addBand("Lat", ProductData.TYPE_FLOAT64);
        srcProduct.addBand("dummy", ProductData.TYPE_FLOAT64);
    }

    @Test
    public void testTransferGeoCoding_TP_BILINEAR__NoSubsetDefinition() throws IOException {
        //preparation
        final boolean bilinear = true;
        boolean antimeridian = false;
        final ComponentGeoCoding geoCoding = initializeWithTiePoints(bilinear, antimeridian);

        final ProductSubsetDef subsetDef = null;

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), ComponentFactory.FWD_TIE_POINT_BILINEAR);
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), ComponentFactory.INV_TIE_POINT);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), subsetGC.getForwardCoding().getFactoryKey());
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), subsetGC.getInverseCoding().getFactoryKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertEquals(subsetGC.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(subsetGC);
    }

    @Test
    public void testTransferGeoCoding_TP_BILINEAR() throws IOException {
        //preparation
        final boolean bilinear = true;
        boolean antimeridian = false;
        final ComponentGeoCoding geoCoding = initializeWithTiePoints(bilinear, antimeridian);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.addNodeName("dummy");

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), ComponentFactory.FWD_TIE_POINT_BILINEAR);
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), ComponentFactory.INV_TIE_POINT);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), subsetGC.getForwardCoding().getFactoryKey());
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), subsetGC.getInverseCoding().getFactoryKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertEquals(subsetGC.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(subsetGC);
    }

    @Test
    public void testTransferGeoCoding_TP_BILINEAR_SUBSAMPLING_3() throws IOException {
        //preparation
        final boolean bilinear = true;
        boolean antimeridian = false;
        final ComponentGeoCoding geoCoding = initializeWithTiePoints(bilinear, antimeridian);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        final int subSampling = 3;
        subsetDef.setSubSampling(subSampling, subSampling);
        subsetDef.addNodeName("dummy");

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), ComponentFactory.FWD_TIE_POINT_BILINEAR);
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), ComponentFactory.INV_TIE_POINT);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), subsetGC.getForwardCoding().getFactoryKey());
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), subsetGC.getInverseCoding().getFactoryKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertEquals(subsetGC.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(subsetGC, subSampling);
    }

    @Test
    public void testTransferGeoCoding_TP_BILINEAR_with_ANTIMERIDIAN() throws IOException {
        //preparation
        final boolean bilinear = true;
        boolean antimeridian = true;
        final ComponentGeoCoding geoCoding = initializeWithTiePoints(bilinear, antimeridian);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.addNodeName("dummy");

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), ComponentFactory.FWD_TIE_POINT_BILINEAR);
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), ComponentFactory.INV_TIE_POINT);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), true);
        doForwardBackwardTestWithAntimeridian(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), subsetGC.getForwardCoding().getFactoryKey());
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), subsetGC.getInverseCoding().getFactoryKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertEquals(subsetGC.isCrossingMeridianAt180(), true);
        doForwardBackwardTestWithAntimeridian(subsetGC);
    }

    @Test
    public void testTransferGeoCoding_TP_SPLINE() throws IOException {
        //preparation
        final boolean bilinear = false; // = SPLINE instead
        boolean antimeridian = false;
        final ComponentGeoCoding geoCoding = initializeWithTiePoints(bilinear, antimeridian);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.addNodeName("dummy");

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), ComponentFactory.FWD_TIE_POINT_SPLINE);
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), ComponentFactory.INV_TIE_POINT);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), subsetGC.getForwardCoding().getFactoryKey());
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), subsetGC.getInverseCoding().getFactoryKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(subsetGC);
    }

    @Test
    public void testTransferGeoCoding_TP_SPLINE_SUBSAMPLING() throws IOException {
        //preparation
        final boolean bilinear = false; // = SPLINE instead
        boolean antimeridian = false;
        final ComponentGeoCoding geoCoding = initializeWithTiePoints(bilinear, antimeridian);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        final int subSampling = 4;
        subsetDef.setSubSampling(subSampling, subSampling);
        subsetDef.addNodeName("dummy");

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), ComponentFactory.FWD_TIE_POINT_SPLINE);
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), ComponentFactory.INV_TIE_POINT);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), subsetGC.getForwardCoding().getFactoryKey());
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), subsetGC.getInverseCoding().getFactoryKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(subsetGC, subSampling);
    }

    @Test
    public void testTransferGeoCoding_TP_SPLINE_with_ANTIMERIDIAN() throws IOException {
        //preparation
        final boolean bilinear = false; // = SPLINE instead
        boolean antimeridian = true;
        final ComponentGeoCoding geoCoding = initializeWithTiePoints(bilinear, antimeridian);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.addNodeName("dummy");

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), ComponentFactory.FWD_TIE_POINT_SPLINE);
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), ComponentFactory.INV_TIE_POINT);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), true);
        doForwardBackwardTestWithAntimeridian(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), subsetGC.getForwardCoding().getFactoryKey());
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), subsetGC.getInverseCoding().getFactoryKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertEquals(subsetGC.isCrossingMeridianAt180(), true);
        doForwardBackwardTestWithAntimeridian(subsetGC);
    }

    @Test
    public void testTransferGeoCoding_PIXELBASED() throws IOException {
        //preparation
        final boolean interpolating = true;
        final boolean quadTree = true;
        final boolean antimeridian = false;
        final ComponentGeoCoding geoCoding = initializeWithBands(interpolating, quadTree, antimeridian);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.addNodeName("dummy");

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), ComponentFactory.FWD_PIXEL_INTERPOLATING);
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), ComponentFactory.INV_PIXEL_QUAD_TREE_INTERPOLATING);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), subsetGC.getForwardCoding().getFactoryKey());
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), subsetGC.getInverseCoding().getFactoryKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertEquals(subsetGC.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(subsetGC);
    }

    @Test
    public void testTransferGeoCoding_PIXELBASED_Subsampling_2() throws IOException {
        //preparation
        final boolean interpolating = true;
        final boolean quadTree = true;
        final boolean antimeridian = false;
        final ComponentGeoCoding geoCoding = initializeWithBands(interpolating, quadTree, antimeridian);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        final int subSampling = 2;
        subsetDef.setSubSampling(subSampling, subSampling);
        subsetDef.addNodeName("dummy");

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), ComponentFactory.FWD_PIXEL_INTERPOLATING);
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), ComponentFactory.INV_PIXEL_QUAD_TREE_INTERPOLATING);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getFactoryKey(), subsetGC.getForwardCoding().getFactoryKey());
        assertEquals(geoCoding.getInverseCoding().getFactoryKey(), subsetGC.getInverseCoding().getFactoryKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertEquals(subsetGC.isCrossingMeridianAt180(), false);

        GeoPos geoPos;
        PixelPos pixelPos;
        PixelPos ppInverse;

        pixelPos = new PixelPos(0.5, 0.5);

        geoPos = subsetGC.getGeoPos(pixelPos, null);
        assertEquals(-19.4, geoPos.lon, 1e-12);
        assertEquals(48.6, geoPos.lat, 1e-12);
        ppInverse = subsetGC.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-8);
        assertEquals(pixelPos.y, ppInverse.y, 1e-8);

        pixelPos = new PixelPos(3.5, 3.5);
        geoPos = subsetGC.getGeoPos(pixelPos, null);
        assertEquals(-15.8, geoPos.lon, 1e-12);
        assertEquals(40.2, geoPos.lat, 1e-12);
        ppInverse = subsetGC.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-8);
        assertEquals(pixelPos.y, ppInverse.y, 1e-8);

        pixelPos = new PixelPos(2.5, 2.5);
        geoPos = subsetGC.getGeoPos(pixelPos, null);
        assertEquals(-17.0, geoPos.lon, 1e-12);
        assertEquals(43.0, geoPos.lat, 1e-12);
        ppInverse = subsetGC.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-8);
        assertEquals(pixelPos.y, ppInverse.y, 1e-8);
    }

    public void doForwardBackwardTest(ComponentGeoCoding gc) {
        doForwardBackwardTest(gc, 1);
    }

    public void doForwardBackwardTest(ComponentGeoCoding gc, int subSampling) {
        GeoPos geoPos;
        PixelPos pixelPos;
        PixelPos ppInverse;

        pixelPos = newPixelPos(0.5, 0.5, subSampling);

        geoPos = gc.getGeoPos(pixelPos, null);
        assertEquals(-20, geoPos.lon, 1e-12);
        assertEquals(50, geoPos.lat, 1e-12);
        ppInverse = gc.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-8);
        assertEquals(pixelPos.y, ppInverse.y, 1e-8);

        pixelPos = newPixelPos(5.5, 5.5, subSampling);
        geoPos = gc.getGeoPos(pixelPos, null);
        assertEquals(-17, geoPos.lon, 1e-12);
        assertEquals(43, geoPos.lat, 1e-12);
        ppInverse = gc.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-8);
        assertEquals(pixelPos.y, ppInverse.y, 1e-8);

        pixelPos = newPixelPos(3.5, 3.5, subSampling);
        geoPos = gc.getGeoPos(pixelPos, null);
        assertEquals(-18.2, geoPos.lon, 1e-12);
        assertEquals(45.8, geoPos.lat, 1e-12);
        ppInverse = gc.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-8);
        assertEquals(pixelPos.y, ppInverse.y, 1e-8);
    }

    public void doForwardBackwardTestWithAntimeridian(ComponentGeoCoding gc) {
        doForwardBackwardTestWithAntimeridian(gc, 1);
    }

    public void doForwardBackwardTestWithAntimeridian(ComponentGeoCoding gc, int subSampling) {
        GeoPos geoPos;
        PixelPos pixelPos;
        PixelPos ppInverse;

        pixelPos = newPixelPos(0.5, 0.5, subSampling);
        geoPos = gc.getGeoPos(pixelPos, null);
        assertEquals(160.0, geoPos.lon, 1e-6);
        assertEquals(50.0, geoPos.lat, 1e-15);
        ppInverse = gc.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-6);
        assertEquals(pixelPos.y, ppInverse.y, 1e-6);

        pixelPos = newPixelPos(5.5, 5.5, subSampling);
        geoPos = gc.getGeoPos(pixelPos, null);
        assertEquals(163.0, geoPos.lon, 1e-6);
        assertEquals(43.0, geoPos.lat, 1e-15);
        ppInverse = gc.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-6);
        assertEquals(pixelPos.y, ppInverse.y, 1e-6);

        pixelPos = newPixelPos(3.5, 3.5, subSampling);
        geoPos = gc.getGeoPos(pixelPos, null);
        assertEquals(161.8, geoPos.lon, 1e-3);
        assertEquals(45.8, geoPos.lat, 1e-15);
        ppInverse = gc.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-3);
        assertEquals(pixelPos.y, ppInverse.y, 1e-4);
    }

    private PixelPos newPixelPos(double x, double y, int subSampling) {
        final double rezi = 1.0 / subSampling;
        final double x1 = (x - 0.5) * rezi + 0.5;
        final double y1 = (y - 0.5) * rezi + 0.5;
        return new PixelPos(x1, y1);
    }

    public ComponentGeoCoding initializeWithTiePoints(boolean bilinear, boolean antimeridian) {
        float[][] tiePointFloats = createTiePointFloats(antimeridian);
        float[] lons = tiePointFloats[0];
        float[] lats = tiePointFloats[1];
        srcProduct.getTiePointGrid("tpLon").setData(ProductData.createInstance(lons));
        srcProduct.getTiePointGrid("tpLat").setData(ProductData.createInstance(lats));
        final GeoRaster geoRaster = new GeoRaster(toD(lons), toD(lats), "tpLon", "tpLat",
                                                  TP_WIDTH, TP_HEIGHT, SCENE_WIDTH, SCENE_HEIGHT,
                                                  300.0, 0.5, 0.5, 5, 5);
        final ForwardCoding forwardCoding;
        if (bilinear) {
            forwardCoding = ComponentFactory.getForward(ComponentFactory.FWD_TIE_POINT_BILINEAR);
        } else {
            forwardCoding = ComponentFactory.getForward(ComponentFactory.FWD_TIE_POINT_SPLINE);
        }
        final InverseCoding inverseCoding = ComponentFactory.getInverse(ComponentFactory.INV_TIE_POINT);
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, GeoChecks.ANTIMERIDIAN);
        geoCoding.initialize();
        srcProduct.setSceneGeoCoding(geoCoding);
        return geoCoding;
    }

    public ComponentGeoCoding initializeWithBands(boolean interpolating, boolean quadTree, boolean antimeridian) {
        double[][] sceneDoubles = createSceneDoubles(antimeridian);
        double[] lons = sceneDoubles[0];
        double[] lats = sceneDoubles[1];
        srcProduct.getBand("Lon").setData(ProductData.createInstance(lons));
        srcProduct.getBand("Lat").setData(ProductData.createInstance(lats));
        final GeoRaster geoRaster = new GeoRaster(lons, lats, "Lon", "Lat",
                                                  SCENE_WIDTH, SCENE_HEIGHT,
                                                  300.0);
        final ForwardCoding forwardCoding;
        final InverseCoding inverseCoding;
        if (interpolating) {
            forwardCoding = ComponentFactory.getForward(ComponentFactory.FWD_PIXEL_INTERPOLATING);
            if (quadTree) {
                inverseCoding = ComponentFactory.getInverse(ComponentFactory.INV_PIXEL_QUAD_TREE_INTERPOLATING);
            } else {
                inverseCoding = ComponentFactory.getInverse(ComponentFactory.INV_PIXEL_GEO_INDEX_INTERPOLATING);
            }
        } else {
            forwardCoding = ComponentFactory.getForward(ComponentFactory.FWD_PIXEL);
            if (quadTree) {
                inverseCoding = ComponentFactory.getInverse(ComponentFactory.INV_PIXEL_QUAD_TREE);
            } else {
                inverseCoding = ComponentFactory.getInverse(ComponentFactory.INV_PIXEL_GEO_INDEX);
            }
        }
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, GeoChecks.ANTIMERIDIAN);
        geoCoding.initialize();
        srcProduct.setSceneGeoCoding(geoCoding);
        return geoCoding;
    }

    private double[] toD(float[] lons) {
        final double[] doubles = new double[lons.length];
        for (int i = 0; i < lons.length; i++) {
            doubles[i] = lons[i];
        }
        return doubles;
    }

    public float[][] createTiePointFloats(boolean withAntimeridian) {

        final float[] lon = new float[TP_WIDTH * TP_HEIGHT];
        final float[] lat = new float[TP_WIDTH * TP_HEIGHT];

        final float lonStart = withAntimeridian ? 160 : -20;
        final float lonStepY = -2;
        final float lonStepX = 5;

        final float latStart = 50;
        final float latStepY = -5;
        final float latStepX = -2;

        for (int y = 0; y < TP_HEIGHT; y++) {
            final float lonOffY = y * lonStepY;
            final float latOffY = y * latStepY;
            for (int x = 0; x < TP_WIDTH; x++) {
                final int idx = y * TP_WIDTH + x;
                lon[idx] = lonStart + lonOffY + x * lonStepX;
                lat[idx] = latStart + latOffY + x * latStepX;
            }
        }

        for (int i = 0; i < lon.length; i++) {
            float v = lon[i];
            lon[i] = v > 180 ? v - 360 : v;
        }

        return new float[][]{lon, lat};
    }


    public double[][] createSceneDoubles(boolean withAntimeridian) {

        final double[] lon = new double[SCENE_WIDTH * SCENE_HEIGHT];
        final double[] lat = new double[SCENE_WIDTH * SCENE_HEIGHT];

        final double lonStart = withAntimeridian ? 160 : -20;
        final double lonStepY = -0.4;
        final double lonStepX = 1;

        final double latStart = 50;
        final double latStepY = -1;
        final double latStepX = -0.4;

        for (int y = 0; y < SCENE_HEIGHT; y++) {
            final double lonOffY = y * lonStepY;
            final double latOffY = y * latStepY;
            for (int x = 0; x < SCENE_WIDTH; x++) {
                final int idx = y * SCENE_WIDTH + x;
                lon[idx] = lonStart + lonOffY + x * lonStepX;
                lat[idx] = latStart + latOffY + x * latStepX;
            }
        }

        for (int i = 0; i < lon.length; i++) {
            double v = lon[i];
            lon[i] = v > 180 ? v - 360 : v;
        }

        return new double[][]{lon, lat};
    }


}
