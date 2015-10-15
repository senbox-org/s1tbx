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

package org.esa.snap.core.gpf.graph;

import org.junit.Test;

import static org.junit.Assert.*;


public class NodeTest {

    @Test
    public void testAddSource() throws Exception {
        final Node node = new Node("myId", "opName");
        node.addSource(new NodeSource("nodeName", "source1"));

        assertEquals(1, node.getSources().length);
        assertEquals("nodeName", node.getSources()[0].getName());

        node.addSource(new NodeSource("anotherName", "source2"));
        node.addSource(new NodeSource("thirdName", "source3"));

        assertEquals(3, node.getSources().length);
        assertEquals("nodeName", node.getSources()[0].getName());
        assertEquals("anotherName", node.getSources()[1].getName());
        assertEquals("thirdName", node.getSources()[2].getName());

    }

    @Test
    public void testRemoveSource() throws Exception {
        final Node baseNode = new Node("myId", "opName");
        NodeSource node1 = new NodeSource("nodeName", "source1");
        NodeSource node2 = new NodeSource("anotherName", "source2");
        NodeSource node3 = new NodeSource("thirdName", "source3");
        baseNode.addSource(node1);
        baseNode.addSource(node2);
        baseNode.addSource(node3);

        assertEquals(3, baseNode.getSources().length);

        baseNode.removeSource(node2);

        assertEquals(2, baseNode.getSources().length);
        assertEquals("nodeName", baseNode.getSources()[0].getName());
        assertEquals("thirdName", baseNode.getSources()[1].getName());

        baseNode.removeSource(node1);
        baseNode.removeSource(node3);

        assertEquals(0, baseNode.getSources().length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicatedSourceNodeNameNotAllowed() throws Exception {
        final Node node = new Node("myId", "opName");
        node.addSource(new NodeSource("duplicated", "sourceNode1"));
        node.addSource(new NodeSource("sameId", "sourceNode1"));
    }
}
