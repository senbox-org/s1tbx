package org.esa.beam.framework.dataop.maptransf;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.GeoPos;

import java.awt.geom.Point2D;

/**
 * Created by IntelliJ IDEA.
 * User: marco
 * Date: 15.02.2006
 * Time: 12:15:01
 */

/**
 * Description of StereographicTest
 *
 * @author Marco Peters
 */
public class StereographicTest extends TestCase {

    private static final double EPSILON = 1.0e-6;
    private final double[] DEFAULT_PARAMS = new double[]{
        Ellipsoid.WGS_84.getSemiMajor(), // semi_major (meter)
        Ellipsoid.WGS_84.getSemiMinor(), // semi_minor (meter)
        90.0, // central parallel (degree)
        0.0, // central meridian (degree)
        1.0, //  scale factor
        0.0, // false_easting (meter)
        0.0 // false_northing (meter)
    };

    public void testForwardTransform() {
        final MapTransform transform = MapTransformFactory.createTransform(StereographicDescriptor.TYPE_ID,
                                                                           DEFAULT_PARAMS);
        final float lat = 90.0f, lon = 25.0f;
        GeoPos geoPos = new GeoPos(lat, lon);

        Point2D mapPoint = transform.forward(geoPos, null);

        assertEquals(0.0, mapPoint.getX(), EPSILON);
        assertEquals(0.0, mapPoint.getY(), EPSILON);
    }

    public void testForwardAndInverseTransform() {
        final MapTransform transform = MapTransformFactory.createTransform(StereographicDescriptor.TYPE_ID,
                                                                           DEFAULT_PARAMS);

        float lat = 90f, lon = 11.87305555f;
        GeoPos geoPos = new GeoPos(lat, lon);

        Point2D mapPoint = transform.forward(geoPos, null);

        GeoPos testPos = transform.inverse(mapPoint, null);
        assertEquals(90.0, testPos.getLat(), EPSILON);
        assertEquals(0, testPos.getLon(), EPSILON);

        lat = 53.786666666f;
        lon = 11.87305555f;
        geoPos = new GeoPos(lat, lon);

        mapPoint = transform.forward(geoPos, null);

        testPos = transform.inverse(mapPoint, null);
        assertEquals(lat, testPos.getLat(), EPSILON);
        assertEquals(lon, testPos.getLon(), EPSILON);
    }

}
