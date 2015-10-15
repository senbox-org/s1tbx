package org.esa.snap.core.util.math;/*
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

import static org.junit.Assert.*;

public class SphericalDistanceTest {

    @Test
    public void testDistance() {
        final DistanceMeasure distanceMeasure = new SphericalDistance(0.0, 0.0);

        assertEquals(0.0, distanceMeasure.distance(0.0, 0.0), 0.0);
        assertEquals(1.0, Math.toDegrees(distanceMeasure.distance(1.0, 0.0)), 1.0e-10);
        assertEquals(1.0, Math.toDegrees(distanceMeasure.distance(0.0, 1.0)), 1.0e-10);
    }
}
