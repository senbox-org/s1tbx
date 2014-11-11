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

package com.bc.ceres.swing.figure.support;

import org.junit.Before;
import org.junit.Test;

import java.awt.geom.Point2D;

import static org.junit.Assert.assertEquals;


public class PointHandleTest {

    private DefaultPointFigure figure;
    private PointHandle handle;

    @Before
    public void setUp() throws Exception {
        figure = new DefaultPointFigure(new Point2D.Double(), 1e-10, new DefaultFigureStyle(), new DefaultFigureStyle());
        handle = new PointHandle(figure, new DefaultFigureStyle());
        assertEquals(new Point2D.Double(0.0, 0.0), figure.getLocation());
        assertEquals(new Point2D.Double(0.0, 0.0), handle.getLocation());
    }

    @Test
    public void testMove() throws Exception {
        handle.move(1.5, -2.2);
        assertEquals(new Point2D.Double(1.5, -2.2), handle.getLocation());
        assertEquals(new Point2D.Double(1.5, -2.2), figure.getLocation());
    }

    @Test
    public void testUpdateLocation() throws Exception {
        figure.setLocation(8.1, 0.4);
        assertEquals(new Point2D.Double(8.1, 0.4), handle.getLocation());
        assertEquals(new Point2D.Double(8.1, 0.4), figure.getLocation());
    }
}