package org.esa.snap.core.datamodel;

import org.esa.snap.core.dataop.maptransf.Datum;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 * @since 6.0
 */
public class GcpGeoCodingTest {

    private GcpGeoCoding gcpGeoCoding;

    @Before
    public void setUp() throws Exception {
        int width = 10;
        int height = 10;

        Placemark[] gcps = {
                Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "p1", "p1", "", new PixelPos(0.5f, 0.5f), new GeoPos(10, -10),
                                               null),
                Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "p2", "p2", "", new PixelPos(width - 0.5f, 0.5f), new GeoPos(10, 10),
                                               null),
                Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "p3", "p3", "", new PixelPos(width - 0.5f, height - 0.5f), new GeoPos(-10, 10),
                                               null),
                Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "p4", "p4", "", new PixelPos(0.5f, height - 0.5f), new GeoPos(-10, -10),
                                               null),
        };
        gcpGeoCoding = new GcpGeoCoding(GcpGeoCoding.Method.POLYNOMIAL1, gcps, width, height, Datum.WGS_84);
    }

    @Test
    public void transferGeoCoding() throws Exception {
        Scene source = SceneFactory.createScene(new Band("source", ProductData.TYPE_INT8, 10, 10));
        Scene target = SceneFactory.createScene(new Band("target", ProductData.TYPE_INT8, 10, 10));

        boolean transferred = gcpGeoCoding.transferGeoCoding(source, target, null);

        assertTrue(transferred);
        assertTrue(target.getGeoCoding() instanceof GcpGeoCoding);
    }

}