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
package org.esa.snap.cluster;

import junit.framework.TestCase;

import static java.lang.Math.*;

/**
 * Tests for class {@link MultinormalDistribution}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class MultinormalDistributionTest extends TestCase {

    public void test1D() {
        final Distribution dist =
                new MultinormalDistribution(new double[]{0.0}, new double[][]{{1.0}});

        final double maximum = 1.0 / sqrt(2.0 * PI);
        assertEquals(maximum, dist.probabilityDensity(new double[]{0.0}), 1.0E-14);

        final double expectedValue = maximum * exp(-0.5);
        assertEquals(expectedValue, dist.probabilityDensity(new double[]{1.0}), 1.0E-14);
    }

    public void test2D() {
        final Distribution dist =
                new MultinormalDistribution(new double[]{0.0, 0.0}, new double[][]{{1.0, 0.0}, {0.0, 2.0}});

        final double maximum = 1.0 / (2.0 * PI) / sqrt(2.0);
        assertEquals(maximum, dist.probabilityDensity(new double[]{0.0, 0.0}), 1.0E-14);

        final double expectedValue = maximum * exp(-0.75);
        assertEquals(expectedValue, dist.probabilityDensity(new double[]{1.0, 1.0}), 1.0E-14);
    }
}
