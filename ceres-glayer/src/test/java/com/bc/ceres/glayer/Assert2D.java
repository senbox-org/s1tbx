/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.glayer;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import static org.junit.Assert.*;

public class Assert2D {
    public static double epsilon = 1.0e-10;

    public static void assertEquals(Point2D expected, Point2D actual) {
        if (expected == actual) {
            return;
        }
        if (expected == null || actual == null) {
            fail("expected:<" + expected + "> but was:<" + actual + ">");
            return;
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
            return;
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
