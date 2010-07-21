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

package org.esa.beam.framework.dataop.resamp;


import junit.framework.TestCase;

public class ResamplingFactoryTest extends TestCase {

    public void testCreateResampling() {
        Resampling resampling;

        resampling = ResamplingFactory.createResampling(ResamplingFactory.CUBIC_CONVOLUTION_NAME);
        assertEquals(resampling.getName(), Resampling.CUBIC_CONVOLUTION.getName());

        resampling = ResamplingFactory.createResampling(ResamplingFactory.NEAREST_NEIGHBOUR_NAME);
        assertEquals(resampling.getName(), Resampling.NEAREST_NEIGHBOUR.getName());

        resampling = ResamplingFactory.createResampling(ResamplingFactory.BILINEAR_INTERPOLATION_NAME);
        assertEquals(resampling.getName(), Resampling.BILINEAR_INTERPOLATION.getName());

        resampling = ResamplingFactory.createResampling("Not known");
        assertTrue(resampling == null);

    }

}