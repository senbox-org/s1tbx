package org.esa.beam.framework.datamodel;

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class CrsGeoCodingTest {

    private CrsGeoCoding srcGeoCoding;
    private Scene destScene;
    private Scene srcScene;

    @Before
    public void setup() throws Exception {
        final Rectangle2D.Double imageBounds = new Rectangle2D.Double(0, 0, 10, 20);
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
        assertEquals(srcGeoCoding.getModelCRS(), destGeoCoding.getModelCRS());
        assertEquals(srcGeoCoding.getImageToModelTransform(), destGeoCoding.getImageToModelTransform());

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
        assertEquals(srcGeoCoding.getModelCRS(), destGeoCoding.getModelCRS());

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
        assertEquals(srcGeoCoding.getModelCRS(), destGeoCoding.getModelCRS());

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
        assertEquals(srcGeoCoding.getModelCRS(), destGeoCoding.getModelCRS());

        comparePixelPos(destGeoCoding, new PixelPos( 2, 2), new PixelPos(0, 0));
        comparePixelPos(destGeoCoding, new PixelPos(10, 2), new PixelPos(4, 0));
        comparePixelPos(destGeoCoding, new PixelPos(10,10), new PixelPos(4, 4));
        comparePixelPos(destGeoCoding, new PixelPos( 2,10), new PixelPos(0, 4));
    }

    @Test
    public void testCrossing180() throws Exception {
        final Rectangle2D.Double imageBounds = new Rectangle2D.Double(0, 0, 10, 20);
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

    private void comparePixelPos(GeoCoding destGeoCoding, PixelPos pixelPos, PixelPos pixelPos1) {
        GeoPos srcPos = srcGeoCoding.getGeoPos(pixelPos, null);
        GeoPos destPos = destGeoCoding.getGeoPos(pixelPos1, null);
        assertEquals(srcPos, destPos);
    }

    private CrsGeoCoding createCrsGeoCoding(Rectangle2D.Double imageBounds) throws Exception{
        AffineTransform i2m = new AffineTransform();
        final int northing = 60;
        final int easting = 5;
        i2m.translate(easting, northing);
        final int scaleX = 1;
        final int scaleY = 1;
        i2m.scale(scaleX, -scaleY);
        return new CrsGeoCoding(DefaultGeographicCRS.WGS84, imageBounds, i2m);
    }

    private CrsGeoCoding createCrsGeoCodingCross180(Rectangle2D.Double imageBounds) throws Exception {
        AffineTransform i2m = new AffineTransform();
        final int northing = 60;
        final int easting = 175;
        i2m.translate(easting, northing);
        final int scaleX = 1;
        final int scaleY = 1;
        i2m.scale(scaleX, -scaleY);
        return new CrsGeoCoding(DefaultGeographicCRS.WGS84, imageBounds, i2m);
    }
}