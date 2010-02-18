package org.esa.beam.framework.gpf.graph;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.gpf.TestOps;
import org.esa.beam.util.jai.VerbousTileCache;

import javax.media.jai.JAI;
import javax.media.jai.TileCache;

public class GraphProcessorTest extends TestCase {
    private OperatorSpi spi1;
    private OperatorSpi spi2;
    private OperatorSpi spi3;
    private TileCache jaiTileCache;
    private TileCache testTileCache;

    @Override
    protected void setUp() throws Exception {
        jaiTileCache = JAI.getDefaultInstance().getTileCache();
        testTileCache = new VerbousTileCache(jaiTileCache);
        JAI.getDefaultInstance().setTileCache(testTileCache);
        testTileCache.flush();

        TestOps.clearCalls();
        spi1 = new TestOps.Op1.Spi();
        spi2 = new TestOps.Op2.Spi();
        spi3 = new TestOps.Op3.Spi();
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.addOperatorSpi(spi1);
        registry.addOperatorSpi(spi2);
        registry.addOperatorSpi(spi3);
    }

    @Override
    protected void tearDown() throws Exception {
        testTileCache.flush();
        JAI.getDefaultInstance().setTileCache(jaiTileCache);
        final OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        spiRegistry.removeOperatorSpi(spi1);
        spiRegistry.removeOperatorSpi(spi2);
        spiRegistry.removeOperatorSpi(spi3);
    }

    public void testEmptyChain() {
        Graph graph = new Graph("test-graph");
        try {
            new GraphProcessor().createGraphContext(graph, ProgressMonitor.NULL);
            fail("GraphException expected due to empty graph");
        } catch (GraphException e) {
            // ok
        }
    }

    public void testSourceToNodeResovling() throws GraphException {
        GraphProcessor processor = new GraphProcessor();

        Graph graph = new Graph("chain1");

        Node node1 = new Node("node1", "org.esa.beam.framework.gpf.TestOps$Op1$Spi");

        Node node2 = new Node("node2", "org.esa.beam.framework.gpf.TestOps$Op2$Spi");
        node2.addSource(new NodeSource("input", "node1"));

        Node node3 = new Node("node3", "org.esa.beam.framework.gpf.TestOps$Op3$Spi");
        node3.addSource(new NodeSource("input1", "node1"));
        node3.addSource(new NodeSource("input2", "node2"));

        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);
        GraphContext graphContext = processor.createGraphContext(graph, ProgressMonitor.NULL);

        assertEquals(node2.getSource(0).getSourceNode(), node1);
        assertEquals(node3.getSource(0).getSourceNode(), node1);
        assertEquals(node3.getSource(1).getSourceNode(), node2);
        assertFalse(graphContext.getNodeContext(node1).isOutput());
        assertFalse(graphContext.getNodeContext(node2).isOutput());
        assertTrue(graphContext.getNodeContext(node3).isOutput());
    }

    public void testSpiCreation() throws GraphException {
        GraphProcessor processor = new GraphProcessor();

        Graph graph = new Graph("chain1");

        Node node1 = new Node("node1", "Op1");

        graph.addNode(node1);
        GraphContext graphContext = processor.createGraphContext(graph, ProgressMonitor.NULL);
        NodeContext nodeContext = graphContext.getNodeContext(node1);
        assertEquals("Op1", graph.getNode("node1").getOperatorName());
        assertEquals("org.esa.beam.framework.gpf.TestOps$Op1", nodeContext.getOperator().getClass().getName());
    }

    public void testTargetProductCreation() throws GraphException {
        GraphProcessor processor = new GraphProcessor();

        Graph graph = new Graph("chain1");

        Node node1 = new Node("node1", "Op1");

        graph.addNode(node1);
        GraphContext graphContext = processor.createGraphContext(graph, ProgressMonitor.NULL);

        Product[] outputProducts = graphContext.getOutputProducts();
        assertNotNull(outputProducts);
        assertEquals(1, outputProducts.length);
        assertNotNull(outputProducts[0]);
    }

    @SuppressWarnings("null")
    public void testTwoOpsExecutionOrder() throws Exception {
        GraphProcessor processor = new GraphProcessor();

        Graph graph = new Graph("chain1");

        Node node1 = new Node("node1", "Op1");
        Node node2 = new Node("node2", "Op2");

        node2.addSource(new NodeSource("input", "node1"));

        graph.addNode(node1);
        graph.addNode(node2);
        GraphContext graphContext = processor.createGraphContext(graph, ProgressMonitor.NULL);

        Product chainOut = graphContext.getOutputProducts()[0];

        assertNotNull(chainOut);
        assertEquals("Op2Name", chainOut.getName());

        processor.executeGraphContext(graphContext, ProgressMonitor.NULL);
        assertEquals("Op1;Op2;", TestOps.getCalls());
        TestOps.clearCalls();
    }

    ////////////////////////////////////////////////////////////////////////
    //            node1
    //            /  \
    //        node2   \
    //            \   /
    //            node3    <-- Target!
    //
    public void testThreeOpsExecutionOrder() throws Exception {

        VerbousTileCache.setVerbous(false);

        Graph graph = new Graph("graph");

        Node node1 = new Node("node1", "Op1");
        Node node2 = new Node("node2", "Op2");
        Node node3 = new Node("node3", "Op3");

        node2.addSource(new NodeSource("input", "node1"));
        node3.addSource(new NodeSource("input1", "node1"));
        node3.addSource(new NodeSource("input2", "node2"));
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);

        GraphProcessor processor = new GraphProcessor();
        GraphContext graphContext = processor.createGraphContext(graph, ProgressMonitor.NULL);
        Product chainOut = graphContext.getOutputProducts()[0];

        assertNotNull(chainOut);
        assertEquals("Op3Name", chainOut.getName());

        processor.executeGraphContext(graphContext, ProgressMonitor.NULL);
        // - Op3 requires the two bands of Op2
        // - Op2 computes all bands
        // --> Op2 should only be called once
        assertEquals("Op1;Op2;Op3;", TestOps.getCalls());
        TestOps.clearCalls();

        VerbousTileCache.setVerbous(false);
    }

    public void testAnnotationsProcessed() throws Exception {
        GraphProcessor processor = new GraphProcessor();

        Graph graph = new Graph("graph");

        Node node1 = new Node("node1", "Op1");
        Node node2 = new Node("node2", "Op2");
        Node node3 = new Node("node3", "Op3");

        node2.addSource(new NodeSource("input", "node1"));
        node3.addSource(new NodeSource("input1", "node1"));
        node3.addSource(new NodeSource("input2", "node2"));
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);
        GraphContext graphContext = processor.createGraphContext(graph, ProgressMonitor.NULL);

        NodeContext nodeContext2 = graphContext.getNodeContext(node2);
        NodeContext nodeContext3 = graphContext.getNodeContext(node3);

        TestOps.Op2 op2 = ((TestOps.Op2) nodeContext2.getOperator());
        TestOps.Op3 op3 = ((TestOps.Op3) nodeContext3.getOperator());

        assertNotNull(op2.input);
        assertSame(nodeContext2.getSourceProduct("input"), op2.input);
        assertNotNull(op3.input1);
        assertNotNull(op3.input2);
        assertSame(nodeContext3.getSourceProduct("input1"), op3.input1);
        assertSame(nodeContext3.getSourceProduct("input2"), op3.input2);

        assertNotNull(op3.inputs);
        assertEquals(0, op3.inputs.length);

        assertEquals(false, op3.ignoreSign);        // has NO default value
        assertEquals("NN", op3.interpolMethod);     // has default value
        assertEquals(1.5, op3.factor);              // has default value

    }

    public void testMissingNode() {
        GraphProcessor processor = new GraphProcessor();

        Graph graph = new Graph("graph");

        Node node1 = new Node("node1", "Op1");
        Node node2 = new Node("node2", "Op2");
        Node node3 = new Node("node3", "Op3");

        node2.addSource(new NodeSource("input", "node1"));
        node3.addSource(new NodeSource("input1", "node1"));
        node3.addSource(new NodeSource("input2", "node2"));

        try {
            graph.addNode(node1);
            graph.addNode(node3);
            processor.createGraphContext(graph, ProgressMonitor.NULL);
            fail("GraphException expected.");
        } catch (GraphException e) {
            // expected
        }

    }

    public void testMissingSpi() {
        GraphProcessor processor = new GraphProcessor();

        Graph graph = new Graph("graph");

        Node node1 = new Node("node1", "Opa");
        Node node2 = new Node("node2", "Op2");
        Node node3 = new Node("node3", "Op3");

        node2.addSource(new NodeSource("input", "node1"));
        node3.addSource(new NodeSource("input1", "node1"));
        node3.addSource(new NodeSource("input2", "node2"));

        try {
            graph.addNode(node1);
            graph.addNode(node3);
            processor.createGraphContext(graph, ProgressMonitor.NULL);
            fail("GraphException expected.");
        } catch (GraphException e) {
            // expected
        }
    }

}
