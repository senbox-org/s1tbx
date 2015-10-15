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
package org.esa.snap.core.dataop.barithm;

import junit.framework.TestCase;

public class RasterDataEvalEnvTest extends TestCase {

    public void testDefaultConstructor() {
        final RasterDataEvalEnv env = new RasterDataEvalEnv(0, 0, 1, 1);
        assertEquals(0, env.getPixelX());
        assertEquals(0, env.getPixelY());
        assertEquals(0, env.getElemIndex());
        assertEquals(0, env.getOffsetX());
        assertEquals(0, env.getOffsetY());
        assertEquals(1, env.getRegionWidth());
        assertEquals(1, env.getRegionHeight());
    }

    public void testConstructor() {
        final RasterDataEvalEnv env = new RasterDataEvalEnv(20, 14, 238, 548);
        assertEquals(20, env.getPixelX());
        assertEquals(14, env.getPixelY());
        assertEquals(0, env.getElemIndex());
        assertEquals(20, env.getOffsetX());
        assertEquals(14, env.getOffsetY());
        assertEquals(238, env.getRegionWidth());
        assertEquals(548, env.getRegionHeight());
    }

    public void testXY() {
        final RasterDataEvalEnv env = new RasterDataEvalEnv(50, 20, 200, 100);

        assertEquals(0, env.getElemIndex());
        assertEquals(50, env.getPixelX());
        assertEquals(20, env.getPixelY());

        env.setElemIndex(1);
        assertEquals(1, env.getElemIndex());
        assertEquals(50 + 1, env.getPixelX());
        assertEquals(20, env.getPixelY());

        env.setElemIndex(200 + 2);
        assertEquals(200 + 2, env.getElemIndex());
        assertEquals(50 + 2, env.getPixelX());
        assertEquals(20 + 1, env.getPixelY());

        env.setElemIndex(70 * 200 + 110);
        assertEquals(70 * 200 + 110, env.getElemIndex());
        assertEquals(50 + 110, env.getPixelX());
        assertEquals(20 + 70, env.getPixelY());
    }
}
