package org.esa.beam.util.math;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArcDistanceCalculatorTest {

    @Test
    public void testArcDistance() {
        final DistanceCalculator distanceCalculator = new ArcDistanceCalculator(0.0, 0.0);
        assertEquals(0.0, distanceCalculator.distance(0.0, 0.0), 0.0);
        assertEquals(1.0, Math.toDegrees(distanceCalculator.distance(1.0, 0.0)), 1.0e-10);
        assertEquals(1.0, Math.toDegrees(distanceCalculator.distance(0.0, 1.0)), 1.0e-10);
    }
}
