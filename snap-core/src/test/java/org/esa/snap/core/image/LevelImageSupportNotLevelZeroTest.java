/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.image;

import org.junit.Test;

import static org.junit.Assert.*;

public class LevelImageSupportNotLevelZeroTest {

    @Test
    public void testGetSourceX_sizePowerOf2() {
        final LevelImageSupport levelImageSupport = createLevelSupport(1024);

        assertEquals(0, levelImageSupport.getSourceX(0));
        assertEquals(8, levelImageSupport.getSourceX(1));
        assertEquals(512, levelImageSupport.getSourceX(64));
        assertEquals(1023, levelImageSupport.getSourceX(128));
        assertEquals(1023, levelImageSupport.getSourceX(150)); // clipped to max source X
    }

    private LevelImageSupport createLevelSupport(int size) {
        return new LevelImageSupport(size, size, new ResolutionLevel(3, 8));
    }

    @Test
    public void testGetSourceX_sizeEven() {
        final LevelImageSupport levelImageSupport = createLevelSupport(1000);

        assertEquals(0, levelImageSupport.getSourceX(0));
        assertEquals(8, levelImageSupport.getSourceX(1));
        assertEquals(512, levelImageSupport.getSourceX(64));
        assertEquals(992, levelImageSupport.getSourceX(124));
        assertEquals(999, levelImageSupport.getSourceX(125));
        assertEquals(999, levelImageSupport.getSourceX(200)); // clipped to max source X
    }

    @Test
    public void testGetSourceX_sizeOdd() {
        final LevelImageSupport levelImageSupport = createLevelSupport(955);

        assertEquals(0, levelImageSupport.getSourceX(0));
        assertEquals(8, levelImageSupport.getSourceX(1));
        assertEquals(512, levelImageSupport.getSourceX(64));
        assertEquals(952, levelImageSupport.getSourceX(119));
        assertEquals(954, levelImageSupport.getSourceX(120));
        assertEquals(954, levelImageSupport.getSourceX(200)); // clipped to max source X
    }

    @Test
    public void testGetSourceWidth_sizePowerOf2() {
        final LevelImageSupport levelImageSupport = createLevelSupport(1024);

        assertEquals(1, levelImageSupport.getSourceWidth(0));
        assertEquals(8, levelImageSupport.getSourceWidth(1));
        assertEquals(1024, levelImageSupport.getSourceWidth(128));
    }

    @Test
    public void testGetSourceWidth_sizeEven() {
        final LevelImageSupport levelImageSupport = createLevelSupport(1000);

        assertEquals(1, levelImageSupport.getSourceWidth(0));
        assertEquals(8, levelImageSupport.getSourceWidth(1));
        assertEquals(1000, levelImageSupport.getSourceWidth(125));
        assertEquals(1000, levelImageSupport.getSourceWidth(130));
    }


    @Test
    public void testGetSourceWidth_sizeOdd() {
        final LevelImageSupport levelImageSupport = createLevelSupport(955);

        assertEquals(1, levelImageSupport.getSourceWidth(0));
        assertEquals(8, levelImageSupport.getSourceWidth(1));
        assertEquals(952, levelImageSupport.getSourceWidth(119));
        assertEquals(955, levelImageSupport.getSourceWidth(120));
        assertEquals(955, levelImageSupport.getSourceWidth(130));
    }

}
