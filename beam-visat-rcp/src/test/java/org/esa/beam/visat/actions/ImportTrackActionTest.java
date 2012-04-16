package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Norman Fomferra
 */
public class ImportTrackActionTest {
    @Test
    public void testReadTrack() throws Exception {

        CrsGeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, new Rectangle(360, 180), new AffineTransform());
        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("TrackData.csv"));

        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = ImportTrackAction.readTrack(reader, geoCoding);
        assertNotNull(featureCollection);
        assertEquals(23, featureCollection.size());

        // test ordering
        SimpleFeature[] simpleFeatures = featureCollection.toArray(new SimpleFeature[0]);
        assertEquals(23, simpleFeatures.length);
        assertEquals("ID00000000", simpleFeatures[0].getID());
        assertEquals("ID00000011", simpleFeatures[11].getID());
        assertEquals("ID00000022", simpleFeatures[22].getID());
    }
}
