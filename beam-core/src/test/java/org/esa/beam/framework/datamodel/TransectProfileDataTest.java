package org.esa.beam.framework.datamodel;

import org.junit.Test;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Norman Fomferra
 */
public class TransectProfileDataTest {
    @Test
    public void testCreate() throws Exception {

        Product product = new Product("p", "t", 4, 4);
        Band band = product.addBand("b", "4 * (Y-0.5) + (X-0.5) + 0.1");

        Path2D.Double path = new Path2D.Double();
        // Draw a "Z"
        path.moveTo(0, 0);
        path.lineTo(3, 0);
        path.lineTo(0, 3);
        path.lineTo(3, 3);

        TransectProfileData profileData = TransectProfileData.create(band, path);

        int numPixels = profileData.getNumPixels();
        assertEquals(10, numPixels);

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

        float[] sampleValues = profileData.getSampleValues();
        assertNotNull(sampleValues.length);
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
}
