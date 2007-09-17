package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;

import java.awt.geom.Point2D;

import org.esa.beam.framework.datamodel.Rotator;

/**
 * Tests for class {@link Rotator}..
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class RotatorTest extends TestCase {

    public void testRotateX() {
        final Point2D p1 = new Point2D.Double(0.0, 0.0);
        final Point2D p2 = new Point2D.Double(0.0, 5.0);

        final Rotator rotator = new Rotator(p1, -90.0);
        rotator.transform(p1);
        rotator.transform(p2);

        assertEquals(0.0, p1.getX(), 1.0E-10);
        assertEquals(0.0, p1.getY(), 1.0E-10);
        assertEquals(0.0, p2.getY(), 1.0E-10);
        assertEquals(5.0, p2.getX(), 1.0E-10);
    }

    public void testRotateY() {
        final Point2D p1 = new Point2D.Double(0.0, 45.0);
        final Point2D p2 = new Point2D.Double(0.0, 50.0);

        final Rotator rotator = new Rotator(p1);
        rotator.transform(p1);
        rotator.transform(p2);

        assertEquals(0.0, p1.getX(), 1.0E-10);
        assertEquals(0.0, p1.getY(), 1.0E-10);
        assertEquals(0.0, p2.getX(), 1.0E-10);
        assertEquals(5.0, p2.getY(), 1.0E-10);
    }

    public void testRotateZ() {
        final Point2D p1 = new Point2D.Double(45.0, 0.0);
        final Point2D p2 = new Point2D.Double(50.0, 0.0);

        final Rotator rotator = new Rotator(p1);
        rotator.transform(p1);
        rotator.transform(p2);

        assertEquals(0.0, p1.getX(), 1.0E-10);
        assertEquals(0.0, p1.getY(), 1.0E-10);
        assertEquals(5.0, p2.getX(), 1.0E-10);
        assertEquals(0.0, p2.getY(), 1.0E-10);
    }

    public void testRotateNorthPole() {
        final double[] lons = new double[]{0.0, 90.0, 180.0, -90.0, -180.0};
        final double[] lats = new double[]{80.0, 80.0, 80.0, 80.0, 80.0};

        new Rotator(0.0, 90.0).transform(lons, lats);

        assertEquals(0.0, lons[0], 1.0E-10);
        assertEquals(-10.0, lats[0], 1.0E-10);

        assertEquals(10.0, lons[1], 1.0E-10);
        assertEquals(0.0, lats[1], 1.0E-10);

        assertEquals(0.0, lons[2], 1.0E-10);
        assertEquals(10.0, lats[2], 1.0E-10);

        assertEquals(-10.0, lons[3], 1.0E-10);
        assertEquals(0.0, lats[3], 1.0E-10);

        assertEquals(0.0, lons[4], 1.0E-10);
        assertEquals(10.0, lats[4], 1.0E-10);
    }

    public void testRotateSouthPole() {
        final double[] lons = new double[]{0.0, 90.0, 180.0, -90.0, -180.0};
        final double[] lats = new double[]{-80.0, -80.0, -80.0, -80.0, -80.0};

        new Rotator(0.0, -90.0).transform(lons, lats);

        assertEquals(0.0, lons[0], 1.0E-10);
        assertEquals(10.0, lats[0], 1.0E-10);

        assertEquals(10.0, lons[1], 1.0E-10);
        assertEquals(0.0, lats[1], 1.0E-10);

        assertEquals(0.0, lons[2], 1.0E-10);
        assertEquals(-10.0, lats[2], 1.0E-10);

        assertEquals(-10.0, lons[3], 1.0E-10);
        assertEquals(0.0, lats[3], 1.0E-10);

        assertEquals(-10.0, lats[4], 1.0E-10);
        assertEquals(0.0, lons[4], 1.0E-10);
    }
}
