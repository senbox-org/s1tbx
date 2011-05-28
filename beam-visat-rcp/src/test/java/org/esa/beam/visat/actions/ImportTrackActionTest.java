package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

/**
 * todo - add api doc
 *
 * @author Norman Fomferra
 */
public class ImportTrackActionTest {
    @Test
    public void testReadTrack() throws Exception {

        CrsGeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, new Rectangle(360, 180), new AffineTransform());
        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("TrackData.csv"));

        FeatureCollection<SimpleFeatureType,SimpleFeature> featureCollection = ImportTrackAction.readTrack(reader, geoCoding);
        assertNotNull(featureCollection);
        assertEquals(23, featureCollection.size());
    }
}
