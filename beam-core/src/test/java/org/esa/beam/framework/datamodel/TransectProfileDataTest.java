package org.esa.beam.framework.datamodel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Norman Fomferra
 */
public class TransectProfileDataTest {

    private Product product;
    private Band band;
    private Path2D path;

    @Before
    public void setUp() throws Exception {
        // Draw a "Z"
        path = new Path2D.Double();
        path.moveTo(0, 0);
        path.lineTo(3, 0);
        path.lineTo(0, 3);
        path.lineTo(3, 3);

        product = new Product("p", "t", 4, 4);
        band = product.addBand("b", "4 * (Y-0.5) + (X-0.5) + 0.1");
    }

    @Test
    public void testCreateWithBoxSize() throws Exception {
        final TransectProfileData profileData = TransectProfileData.create(band, path, 3, null);

        int numPixels = profileData.getNumPixels();
        assertEquals(10, numPixels);

        assertPixelPositions(profileData, numPixels);
        assertShapeVertexIndices(profileData);

        float[] sampleValues = profileData.getSampleValues();
        assertNotNull(sampleValues);
        assertEquals(numPixels, sampleValues.length);

        assertEquals(2.6000000F, sampleValues[0], 1E-5F);
        assertEquals(2.4333334F, sampleValues[1], 1E-5F);
        assertEquals(3.4333334F, sampleValues[2], 1E-5F);
        assertEquals(4.6000000F, sampleValues[3], 1E-5F);
        assertEquals(6.1000000F, sampleValues[4], 1E-5F);
        assertEquals(9.1000000F, sampleValues[5], 1E-5F);
        assertEquals(10.600000F, sampleValues[6], 1E-5F);
        assertEquals(10.433333F, sampleValues[7], 1E-5F);
        assertEquals(11.433333F, sampleValues[8], 1E-5F);
        assertEquals(12.600000F, sampleValues[9], 1E-5F);

    }

    @Test
    public void testCreateWithDefaults() throws Exception {
        TransectProfileData profileData = TransectProfileData.create(band, path);
        assertProfileDataIsAsExpected(profileData);
    }

    @Test
    public void testCreateWithCorrelativeData() throws Exception {
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setName("Track");
        ftb.add("point", Point.class);
        ftb.add("data1", Double.class);
        ftb.add("data2", Double.class);
        ftb.setDefaultGeometry("point");
        SimpleFeatureType ft = ftb.buildFeatureType();

        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureBuilder sbb = new SimpleFeatureBuilder(ft);

        // Draw a "Z"
        VectorDataNode track = new VectorDataNode("Track", ft);
        track.getFeatureCollection().add(sbb.buildFeature("id1", new Object[]{gf.createPoint(new Coordinate(0, 0)), 1.2, 2.3}));
        track.getFeatureCollection().add(sbb.buildFeature("id2", new Object[]{gf.createPoint(new Coordinate(3, 0)), 3.4, 4.5}));
        track.getFeatureCollection().add(sbb.buildFeature("id3", new Object[]{gf.createPoint(new Coordinate(0, 3)), 5.6, 6.7}));
        track.getFeatureCollection().add(sbb.buildFeature("id4", new Object[]{gf.createPoint(new Coordinate(3, 3)), 7.8, 8.9}));

        product.getVectorDataGroup().add(track);

        TransectProfileData profileData = TransectProfileData.create(band, track, 1, null);
        assertProfileDataIsAsExpected(profileData);
    }

    @Test
    public void testProfileDataUsingRoiMask() throws Exception {
        final Mask mask = product.addMask("name", Mask.BandMathsType.INSTANCE);
        mask.getImageConfig().setValue(Mask.BandMathsType.PROPERTY_NAME_EXPRESSION, "Y <= 1.5");
        final TransectProfileData profileData = TransectProfileData.create(band, path, 1, mask);
        final float[] sampleValues = profileData.getSampleValues();

        assertEquals(Float.NaN, Float.NaN, 1E-5F);

        assertEquals(0.1F, sampleValues[0], 1E-5F);
        assertEquals(1.1F, sampleValues[1], 1E-5F);
        assertEquals(2.1F, sampleValues[2], 1E-5F);
        assertEquals(3.1F, sampleValues[3], 1E-5F);
        assertEquals(6.1F, sampleValues[4], 1E-5F);
        assertEquals(Float.NaN, sampleValues[5], 1E-5F);
        assertEquals(Float.NaN, sampleValues[6], 1E-5F);
        assertEquals(Float.NaN, sampleValues[7], 1E-5F);
        assertEquals(Float.NaN, sampleValues[8], 1E-5F);
        assertEquals(Float.NaN, sampleValues[9], 1E-5F);
    }

    private static void assertProfileDataIsAsExpected(TransectProfileData profileData) {
        int numPixels = profileData.getNumPixels();
        assertEquals(10, numPixels);

        assertPixelPositions(profileData, numPixels);
        assertShapeVertexIndices(profileData);

        float[] sampleValues = profileData.getSampleValues();
        assertNotNull(sampleValues);
        assertEquals(numPixels, sampleValues.length);
        assertEquals(0.1F, sampleValues[0], 1E-5F);
        assertEquals(1.1F, sampleValues[1], 1E-5F);
        assertEquals(2.1F, sampleValues[2], 1E-5F);
        assertEquals(3.1F, sampleValues[3], 1E-5F);
        assertEquals(6.1F, sampleValues[4], 1E-5F);
        assertEquals(9.1F, sampleValues[5], 1E-5F);
        assertEquals(12.1F, sampleValues[6], 1E-5F);
        assertEquals(13.1F, sampleValues[7], 1E-5F);
        assertEquals(14.1F, sampleValues[8], 1E-5F);
        assertEquals(15.1F, sampleValues[9], 1E-5F);
    }

    private static void assertShapeVertexIndices(TransectProfileData profileData) {
        int numShapeVertices = profileData.getNumShapeVertices();
        assertEquals(4, numShapeVertices);
        int[] shapeVertexIndexes = profileData.getShapeVertexIndexes();
        assertNotNull(shapeVertexIndexes);
        assertEquals(4, shapeVertexIndexes.length);
        assertEquals(0, shapeVertexIndexes[0]);
        assertEquals(3, shapeVertexIndexes[1]);
        assertEquals(6, shapeVertexIndexes[2]);
        assertEquals(9, shapeVertexIndexes[3]);
    }

    private static void assertPixelPositions(TransectProfileData profileData, int numPixels) {
        Point2D[] pixelPositions = profileData.getPixelPositions();
        assertNotNull(pixelPositions);
        assertEquals(numPixels, pixelPositions.length);
        assertEquals(new Point2D.Float(0, 0), pixelPositions[0]);
        assertEquals(new Point2D.Float(1, 0), pixelPositions[1]);
        assertEquals(new Point2D.Float(2, 0), pixelPositions[2]);
        assertEquals(new Point2D.Float(3, 0), pixelPositions[3]);
        assertEquals(new Point2D.Float(2, 1), pixelPositions[4]);
        assertEquals(new Point2D.Float(1, 2), pixelPositions[5]);
        assertEquals(new Point2D.Float(0, 3), pixelPositions[6]);
        assertEquals(new Point2D.Float(1, 3), pixelPositions[7]);
        assertEquals(new Point2D.Float(2, 3), pixelPositions[8]);
        assertEquals(new Point2D.Float(3, 3), pixelPositions[9]);
    }

}
