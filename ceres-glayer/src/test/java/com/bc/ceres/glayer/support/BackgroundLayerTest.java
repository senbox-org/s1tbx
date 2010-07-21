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

package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.TracingLayerListener;

import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.Color;

public class BackgroundLayerTest  {
    @Test
    public void testConstructor() {
        BackgroundLayer layer = new BackgroundLayer(Color.BLUE);
        assertEquals(Color.BLUE, layer.getColor());
        assertNull(layer.getModelBounds());
    }

    @Test
    public void testStyleProperties() {
        BackgroundLayer layer = new BackgroundLayer(Color.WHITE);
        assertEquals(Color.WHITE, layer.getColor());

        TracingLayerListener ll = new TracingLayerListener();
        layer.addListener(ll);

        layer.setColor(Color.YELLOW);
        layer.setColor(Color.YELLOW);
        assertEquals(Color.YELLOW, layer.getColor());
        layer.setColor(Color.BLUE);
        layer.setColor(Color.BLUE);
        assertEquals(Color.BLUE, layer.getColor());

        assertEquals("color;color;", ll.trace);
    }
}