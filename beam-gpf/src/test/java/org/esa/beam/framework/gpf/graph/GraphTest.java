package org.esa.beam.framework.gpf.graph;

import junit.framework.TestCase;

public class GraphTest extends TestCase {

    public void testEmptyChain() {
        Graph graph = new Graph("chain1");

        assertEquals("chain1", graph.getId());
        assertNotNull(graph.getNodes());
    }

    public void testOneNodeChain() {
        Graph graph = new Graph("chain1");
        Node node = new Node("node1", "org.esa.beam.framework.gpf.TestOps$Op1Spi");
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
        Node node = new Node("node1", "org.esa.beam.framework.gpf.TestOps$Op1Spi");
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
        Node node1 = new Node("node1", "org.esa.beam.framework.gpf.TestOps$Op1Spi");
        Node node2 = new Node("node1", "org.esa.beam.framework.gpf.TestOps$Op2Spi");
        try {
            graph.addNode(node1);
            graph.addNode(node2);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

}
