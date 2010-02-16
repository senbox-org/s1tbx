package org.esa.beam.framework.gpf.graph;

import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;

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
    
    public void testapplicationData() throws Exception {
        Graph graph = new Graph("chain1");
        assertNull(graph.getApplicationData("foo"));
        
        Xpp3Dom xpp3Dom1 = new Xpp3Dom("");
        Xpp3Dom font = new Xpp3Dom("font");
        font.setValue("big");
        xpp3Dom1.addChild(font);
        graph.setAppData("foo", xpp3Dom1);
        
        assertSame(xpp3Dom1, graph.getApplicationData("foo"));
        
        Xpp3Dom xpp3Dom2 = new Xpp3Dom("");
        Xpp3Dom font2 = new Xpp3Dom("font");
        font2.setValue("small");
        xpp3Dom2.addChild(font2);
        graph.setAppData("foo", xpp3Dom2);
        
        assertSame(xpp3Dom2, graph.getApplicationData("foo"));
    }

}
