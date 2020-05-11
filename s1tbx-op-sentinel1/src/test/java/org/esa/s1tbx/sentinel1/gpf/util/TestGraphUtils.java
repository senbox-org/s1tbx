/*
 * Copyright (C) 2020 by SENSAR B.V. http://www.sensar.nl
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
package org.esa.s1tbx.sentinel1.gpf.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit test for GraphUtils.
 *
 * @author David A. Monge
 */
public class TestGraphUtils {

    /**
     * Test several cases of graph connectedness.
     */
    @Test
    public void isConnectedGraph() {
        int[][] arcs = new int[][]{{0, 1}, {1, 2}};

        assertTrue(GraphUtils.isConnectedGraph(arcs, 3));
        assertFalse(GraphUtils.isConnectedGraph(arcs, 4));
        assertFalse(GraphUtils.isConnectedGraph(arcs, 2));

        assertFalse(GraphUtils.isConnectedGraph(new int[][]{{0, 1}, {2, 3}}, 4));
    }

    /**
     * Test several cases of graph connectedness.
     */
    @Test
    public void isConnectedGraph_list() {
        List<int[]> arcs = new ArrayList<>();
        arcs.add(new int[]{0, 1});
        arcs.add(new int[]{1, 2});

        assertTrue(GraphUtils.isConnectedGraph(arcs, 3));
        assertFalse(GraphUtils.isConnectedGraph(arcs, 4));
        assertFalse(GraphUtils.isConnectedGraph(arcs, 2));
    }
}