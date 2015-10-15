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

import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import junit.framework.TestCase;

public class GraphTest extends TestCase {

    public void testEmptyChain() {
        Graph graph = new Graph("chain1");

        assertEquals("chain1", graph.getId());
        assertNotNull(graph.getNodes());
    }

    public void testOneNodeChain() {
        Graph graph = new Graph("chain1");
        Node node = new Node("node1", "org.esa.snap.core.gpf.TestOps$Op1Spi");
        try {
            graph.addNode(node);
        } catch (IllegalArgumentException e) {
            fail(e.getMessage());
        }

        assertEquals("chain1", graph.getId());
        assertNotNull(graph.getNodes());
        assertEquals(node, graph.getNode("node1"));

    }

    public void testRemoveNode() {
        Graph graph = new Graph("chain1");
        Node node = new Node("node1", "org.esa.snap.core.gpf.TestOps$Op1Spi");
        try {
            graph.addNode(node);
        } catch (IllegalArgumentException e) {
            fail(e.getMessage());
        }
        boolean result = graph.removeNode("node1");

        assertEquals("chain1", graph.getId());
        assertNotNull(graph.getNodes());
        assertTrue(result);
        assertNull(graph.getNode("node1"));
    }

    public void testAddExistingNode() {
        Graph graph = new Graph("chain1");
        Node node1 = new Node("node1", "org.esa.snap.core.gpf.TestOps$Op1Spi");
        Node node2 = new Node("node1", "org.esa.snap.core.gpf.TestOps$Op2Spi");
        try {
            graph.addNode(node1);
            graph.addNode(node2);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testApplicationData() throws Exception {
        Graph graph = new Graph("chain1");
        assertNull(graph.getApplicationData("foo"));

        XppDom xpp3Dom1 = new XppDom("");
        XppDom font = new XppDom("font");
        font.setValue("big");
        xpp3Dom1.addChild(font);
        graph.setAppData("foo", xpp3Dom1);

        assertSame(xpp3Dom1, graph.getApplicationData("foo"));

        XppDom xpp3Dom2 = new XppDom("");
        XppDom font2 = new XppDom("font");
        font2.setValue("small");
        xpp3Dom2.addChild(font2);
        graph.setAppData("foo", xpp3Dom2);

        assertSame(xpp3Dom2, graph.getApplicationData("foo"));
    }

}
