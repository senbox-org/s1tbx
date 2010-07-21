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

package org.esa.beam.processor.binning;

import junit.framework.TestCase;

import org.esa.beam.framework.datamodel.GeoPos;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 18.11.2005
 * Time: 12:31:04
 * To change this template use File | Settings | File Templates.
 */
public class L3ProjectionRasterTest extends TestCase {
    private L3ProjectionRaster projectionRaster;

    public void testGetWidth() {
        assertEquals("Width", 2220, projectionRaster.getWidth());
    }

    public void testGetHeight() {
        assertEquals("Height", 1110, projectionRaster.getHeight());
    }

    @Override
    public void setUp() {
        projectionRaster = new L3ProjectionRaster();
        projectionRaster.init(111, new GeoPos(20, 10), new GeoPos(20, 30), new GeoPos(10, 30), new GeoPos(10, 10));
    }
}
