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

package org.esa.beam.framework.gpf.graph;

import org.junit.Test;

import static junit.framework.Assert.*;

public class NodeTest {

    @Test
    public void testAddSource() throws Exception {
        final Node node = new Node("myId", "opName");
        node.addSource(new NodeSource("nodeName", "source1"));
        node.addSource(new NodeSource("anotherName", "source2"));
        node.addSource(new NodeSource("thirdName", "source3"));

        assertEquals(3, node.getSources().length);
        assertEquals("nodeName", node.getSources()[0].getName());
        assertEquals("anotherName", node.getSources()[1].getName());
        assertEquals("thirdName", node.getSources()[2].getName());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicatedSourceNodeNameNotAllowed() throws Exception {
        final Node node = new Node("myId", "opName");
        node.addSource(new NodeSource("duplicated", "sourceNode1"));
        node.addSource(new NodeSource("duplicated", "sourceNode2"));
    }
}
