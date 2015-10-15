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
package org.esa.snap.core.datamodel;

import org.junit.Test;

import java.awt.Color;

import static org.esa.snap.core.datamodel.ColorPaletteDef.*;
import static org.junit.Assert.*;

public class ColorPaletteDefTest {

    @Test
    public void testConstructors() {
        ColorPaletteDef cpd = new ColorPaletteDef(-1.0, 1.0);
        assertEquals(256, cpd.getNumColors());
        assertEquals(2, cpd.getNumPoints());
        assertEquals(-1.0, cpd.getPointAt(0).getSample(), 1e-10);
        assertEquals(+1.0, cpd.getPointAt(1).getSample(), 1e-10);
        assertEquals(Color.BLACK, cpd.getPointAt(0).getColor());
        assertEquals(Color.WHITE, cpd.getPointAt(1).getColor());

        cpd = new ColorPaletteDef(-1.0, 0.5, 1.0);
        assertEquals(256, cpd.getNumColors());
        assertEquals(3, cpd.getNumPoints());
        assertEquals(-1.0, cpd.getPointAt(0).getSample(), 1e-10);
        assertEquals(+0.5, cpd.getPointAt(1).getSample(), 1e-10);
        assertEquals(+1.0, cpd.getPointAt(2).getSample(), 1e-10);
        assertEquals(Color.BLACK, cpd.getPointAt(0).getColor());
        assertEquals(Color.GRAY, cpd.getPointAt(1).getColor());
        assertEquals(Color.WHITE, cpd.getPointAt(2).getColor());

        cpd = new ColorPaletteDef(new Point[]{
                new Point(100, Color.ORANGE),
                new Point(200, Color.MAGENTA),
                new Point(500, Color.BLUE),
                new Point(600, Color.WHITE)
        });
        assertEquals(4, cpd.getNumPoints());
        assertEquals(256, cpd.getNumColors());
        assertEquals(true, cpd.isFullyOpaque());


        cpd = new ColorPaletteDef(new Point[]{
                new Point(100, Color.ORANGE),
                new Point(200, Color.MAGENTA),
                new Point(500, Color.BLUE),
                new Point(600, Color.WHITE)
        }, 512);
        assertEquals(4, cpd.getNumPoints());
        assertEquals(512, cpd.getNumColors());
        assertEquals(true, cpd.isFullyOpaque());

        cpd = new ColorPaletteDef(new Point[]{
                new Point(100, new Color(100, 100, 100, 100)),
                new Point(600, Color.WHITE)
        }, 16);
        assertEquals(2, cpd.getNumPoints());
        assertEquals(16, cpd.getNumColors());
        assertEquals(false, cpd.isFullyOpaque());
    }

    @Test
    public void testCreateClone_andEquals() {
        //preparation
        final Point[] points = {
                new Point(1, Color.black),
                new Point(2, Color.red),
                new Point(3, Color.green),
                new Point(4, Color.blue),
                new Point(5, Color.white),
        };
        final ColorPaletteDef cpd = new ColorPaletteDef(points, 256);
        cpd.setDiscrete(true);
        cpd.setAutoDistribute(true);

        //execution
        final ColorPaletteDef clone = (ColorPaletteDef) cpd.clone();

        //verification
        assertTrue(cpd.equals(clone));
    }

}
