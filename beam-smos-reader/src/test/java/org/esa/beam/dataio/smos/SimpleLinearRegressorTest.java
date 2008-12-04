/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.smos;

import junit.framework.TestCase;

public class SimpleLinearRegressorTest extends TestCase {

    /**
     * Tests regression with no points.
     */
    public void testRegressionWithoutPoints() {
        final SimpleLinearRegressor r = new SimpleLinearRegressor(new DefaultPointFilter());

        r.add(Double.NaN, Double.NaN);
        assertEquals(0, r.getPointCount());

        r.add(Double.POSITIVE_INFINITY, Double.NaN);
        assertEquals(0, r.getPointCount());

        r.add(Double.NaN, Double.NEGATIVE_INFINITY);
        assertEquals(0, r.getPointCount());

        assertTrue(Double.isNaN(r.getRegression().getX()));
        assertTrue(Double.isNaN(r.getRegression().getY()));
    }

    public void testRegressionWithOnePoint() {
        final SimpleLinearRegressor r = new SimpleLinearRegressor();

        r.add(0.0, 0.0);

        assertEquals(1, r.getPointCount());
        assertTrue(Double.isNaN(r.getRegression().getX()));
        assertTrue(Double.isNaN(r.getRegression().getY()));
    }

    /**
     * Tests regression with two points.
     */
    public void testRegressionWithTwoPoints() {
        final SimpleLinearRegressor r = new SimpleLinearRegressor();

        r.add(0.0, 0.0);
        r.add(1.0, 1.0);

        assertEquals(2, r.getPointCount());
        assertEquals(1.0, r.getRegression().getX(), 0.0);
        assertEquals(0.0, r.getRegression().getY(), 0.0);
    }

    /**
     * Performs a test using Example 11.1 of Mendenhall & Sincich (1995, Statistics for Engineering
     * and the Sciences) as reference.
     */
    public void testTextbookExample() {
        final SimpleLinearRegressor r = new SimpleLinearRegressor();

        r.add(1.0, 1.0);
        r.add(2.0, 1.0);
        r.add(3.0, 2.0);
        r.add(4.0, 2.0);
        r.add(5.0, 4.0);

        assertEquals(5, r.getPointCount());
        assertEquals(0.7, r.getRegression().getX(), 0.0);
        assertEquals(-0.1, r.getRegression().getY(), 0.0);
    }
}
