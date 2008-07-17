package com.bc;

import junit.framework.TestCase;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public abstract class ImagingTestCase extends TestCase {
    public static double epsilon = 1.0e-10;

    public static void assertEquals(Point2D expected, Point2D actual) {
        if (expected == actual) {
            return;
        }
        if (expected == null || actual == null) {
            fail("expected:<" + expected + "> but was:<" + actual + ">");
        }
        assertTrue("expected:<" + expected + "> but was:<" + actual + ">",
                   equals(expected.getX(), actual.getX())
                           && equals(expected.getY(), actual.getY()));
    }

    public static void assertEquals(Rectangle2D expected, Rectangle2D actual) {
        if (expected == actual) {
            return;
        }
        if (expected == null || actual == null) {
            fail("expected:<" + expected + "> but was:<" + actual + ">");
        }
        assertTrue("expected:<" + expected + "> but was:<" + actual + ">",
                   equals(expected.getX(), actual.getX())
                           && equals(expected.getY(), actual.getY())
                           && equals(expected.getWidth(), actual.getWidth())
                           && equals(expected.getHeight(), actual.getHeight()));
    }

    public static boolean equals(double v1, double v2) {
        return Math.abs(v1 - v2) <= epsilon;
    }

}
