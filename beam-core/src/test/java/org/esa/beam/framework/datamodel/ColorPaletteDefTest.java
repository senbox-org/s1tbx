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
package org.esa.beam.framework.datamodel;

import static org.junit.Assert.*;

import org.junit.*;

import java.awt.Color;

public class ColorPaletteDefTest {

    @Test
    public void testConstructors() {
        ColorPaletteDef cpd = new ColorPaletteDef(-1.0, 1.0);
        assertEquals(256, cpd.getNumColors());
        assertEquals(3, cpd.getNumPoints());
        assertEquals(-1.0, cpd.getPointAt(0).getSample(), 1e-10);
        assertEquals(+0.0, cpd.getPointAt(1).getSample(), 1e-10);
        assertEquals(+1.0, cpd.getPointAt(2).getSample(), 1e-10);
        assertEquals(Color.BLACK, cpd.getPointAt(0).getColor());
        assertEquals(Color.GRAY, cpd.getPointAt(1).getColor());
        assertEquals(Color.WHITE, cpd.getPointAt(2).getColor());

        cpd = new ColorPaletteDef(-1.0, 0.5, 1.0);
        assertEquals(256, cpd.getNumColors());
        assertEquals(3, cpd.getNumPoints());
        assertEquals(-1.0, cpd.getPointAt(0).getSample(), 1e-10);
        assertEquals(+0.5, cpd.getPointAt(1).getSample(), 1e-10);
        assertEquals(+1.0, cpd.getPointAt(2).getSample(), 1e-10);
        assertEquals(Color.BLACK, cpd.getPointAt(0).getColor());
        assertEquals(Color.GRAY, cpd.getPointAt(1).getColor());
        assertEquals(Color.WHITE, cpd.getPointAt(2).getColor());

        cpd = new ColorPaletteDef(new ColorPaletteDef.Point[]{
                    new ColorPaletteDef.Point(100, Color.ORANGE),
                    new ColorPaletteDef.Point(200, Color.MAGENTA),
                    new ColorPaletteDef.Point(500, Color.BLUE),
                    new ColorPaletteDef.Point(600, Color.WHITE)
        });
        assertEquals(4, cpd.getNumPoints());
        assertEquals(256, cpd.getNumColors());


        cpd = new ColorPaletteDef(new ColorPaletteDef.Point[]{
                    new ColorPaletteDef.Point(100, Color.ORANGE),
                    new ColorPaletteDef.Point(200, Color.MAGENTA),
                    new ColorPaletteDef.Point(500, Color.BLUE),
                    new ColorPaletteDef.Point(600, Color.WHITE)
        }, 512);
        assertEquals(4, cpd.getNumPoints());
        assertEquals(512, cpd.getNumColors());
    }

    @Test
    public void testPaletteCreation_numPointsEqualsNumColors() {

        final ColorPaletteDef cpd = new ColorPaletteDef(new ColorPaletteDef.Point[]{
                    new ColorPaletteDef.Point(100, Color.ORANGE),
                    new ColorPaletteDef.Point(200, Color.MAGENTA),
                    new ColorPaletteDef.Point(300, Color.RED),
                    new ColorPaletteDef.Point(400, Color.GREEN),
        }, 4);

        final Color[] palette = cpd.createColorPalette(Scaling.IDENTITY);
        assertNotNull(palette);
        assertEquals(4, palette.length);
        assertEquals(Color.ORANGE, palette[0]);
        assertEquals(Color.MAGENTA, palette[1]);
        assertEquals(Color.RED, palette[2]);
        assertEquals(Color.GREEN, palette[3]);
    }

    @Test
    public void testPaletteCreation_morePointsThanNumColors() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new ColorPaletteDef.Point[]{
                    new ColorPaletteDef.Point(100, Color.WHITE),
                    new ColorPaletteDef.Point(200, Color.BLUE),
                    new ColorPaletteDef.Point(300, Color.RED),
                    new ColorPaletteDef.Point(400, Color.GREEN),
        }, 7);

        final Color[] palette = cpd.createColorPalette(Scaling.IDENTITY);
        assertNotNull(palette);
        assertEquals(7, palette.length);
        assertEquals(new Color(255, 255, 255), palette[0]);
        assertEquals(new Color(128, 128, 255), palette[1]);
        assertEquals(new Color(0, 0, 255), palette[2]);
        assertEquals(new Color(128, 0, 128), palette[3]);
        assertEquals(new Color(255, 0, 0), palette[4]);
        assertEquals(new Color(128, 128, 0), palette[5]);
        assertEquals(new Color(0, 255, 0), palette[6]);
    }
}