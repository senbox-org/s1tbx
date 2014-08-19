/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dat.graphbuilder;

import com.bc.ceres.binding.dom.XppDomElement;
import org.esa.beam.framework.gpf.graph.Node;
import org.esa.beam.framework.gpf.graph.NodeSource;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * GraphNode Tester.
 *
 * @author lveci
 * @version 1.0
 * @since <pre>12/21/2007</pre>
 */
public class TestGraphNode {

    private Node node;
    private GraphNode graphNode;

    @Before
    public void setUp() throws Exception {
        node = new Node("id", "readOp");
        final XppDomElement parameters = new XppDomElement("parameters");
        node.setConfiguration(parameters);

        graphNode = new GraphNode(node);
    }

    @Test
    public void testPosition() {
        Point p1 = new Point(1, 2);
        graphNode.setPos(p1);

        Point p2 = graphNode.getPos();

        assertEquals(p1, p2);
    }

    @Test
    public void testNode() {

        assertEquals(node, graphNode.getNode());
        assertEquals(node.getId(), graphNode.getID());
        assertEquals(node.getOperatorName(), graphNode.getOperatorName());
    }

    @Test
    public void testSourceConnection() {
        final Node sourceNode = new Node("sourceID", "testSourceNodeOp");
        final XppDomElement parameters = new XppDomElement("parameters");
        sourceNode.setConfiguration(parameters);

        GraphNode sourceGraphNode = new GraphNode(sourceNode);

        // test connect
        graphNode.connectOperatorSource(sourceGraphNode.getID());

        NodeSource ns = node.getSource(0);
        assertNotNull(ns);

        assertEquals(ns.getSourceNodeId(), sourceNode.getId());

        // test disconnect
        graphNode.disconnectOperatorSources(sourceGraphNode.getID());

        NodeSource[] nsList = node.getSources();
        assertEquals(nsList.length, 0);
    }
}