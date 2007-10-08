package org.esa.beam.framework.gpf.graph;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.TestOps;

import java.io.StringReader;
import java.io.StringWriter;

public class GraphIOTest extends TestCase {
    private TestOps.Op1.Spi operatorSpi1 = new TestOps.Op1.Spi();
    private TestOps.Op2.Spi operatorSpi2 = new TestOps.Op2.Spi();
    private TestOps.Op3.Spi operatorSpi3 = new TestOps.Op3.Spi();

    @Override
    protected void setUp() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(operatorSpi1);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(operatorSpi2);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(operatorSpi3);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(operatorSpi1);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(operatorSpi2);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(operatorSpi3);
    }

    public void testEmptyChain() throws Exception {
        Graph chain1 = new Graph("myEmptyChain");

        Graph chain2 = doIO(chain1);

        Node[] nodes = chain2.getNodes();
        assertNotNull(nodes);
        assertEquals(0, nodes.length);

        assertEquals(chain1.getId(), chain2.getId());
    }

    public void testOneNodeChain() throws Exception {
        Graph graph1 = new Graph("myOneNodeGraph");
        Node node1 = new Node("node1", "Op1");
        graph1.addNode(node1);

        Graph chain2 = doIO(graph1);

        Node[] nodes = chain2.getNodes();
        assertNotNull(nodes);
        assertEquals(1, nodes.length);
        assertNotNull(nodes[0]);

        assertEquals(graph1.getId(), chain2.getId());
        Node node1Copy = chain2.getNode("node1");
        assertSame(nodes[0], node1Copy);
        assertEquals("Op1", node1Copy.getOperatorName());
    }

    public void testTwoNodeChain() {
        Graph graph1 = new Graph("myTwoNodeGraph");
        Node node1 = new Node("node1", "Op1");

        Node node2 = new Node("node2", "Op2");

        node2.addSource(new NodeSource("key1", "node1"));
        graph1.addNode(node1);
        graph1.addNode(node2);

        Graph graph2 = doIO(graph1);
        assertEquals(graph1.getId(), graph2.getId());

        Node[] nodes = graph2.getNodes();
        assertNotNull(nodes);
        assertEquals(2, nodes.length);
        assertNotNull(nodes[0]);
        assertNotNull(nodes[1]);

        Node node1Copy = graph2.getNode("node1");
        Node node2Copy = graph2.getNode("node2");
        assertSame(nodes[0], node1Copy);
        assertSame(nodes[1], node2Copy);

        NodeSource[] sources1 = node1Copy.getSources();
        assertNotNull(sources1);
        assertEquals(0, sources1.length);

        NodeSource[] sources2 = node2Copy.getSources();
        assertNotNull(sources2);
        assertEquals(1, sources2.length);
        NodeSource source2 = node2Copy.getSource(0);
        assertSame(sources2[0], source2);

        assertEquals("Op1", node1Copy.getOperatorName());
        assertEquals("Op2", node2Copy.getOperatorName());
        assertEquals("node1", source2.getSourceNodeId());
    }

    public void testReadXml() {
        String xml =
                "<graph>\n" +
                        "  <id>foo</id>\n" +
                        "  <node>\n" +
                        "    <id>grunt</id>\n" +
                        "    <operator>Op1</operator>\n" +
                        "  </node>\n" +
                        "  <node>\n" +
                        "    <id>baz</id>\n" +
                        "    <operator>Op2</operator>\n" +
                        "    <sources>\n" +
                        "      <input>grunt</input>\n" +
                        "    </sources>\n" +
                        "    <parameters>\n" +
                        "       <threshold>0.86</threshold>\n" +
                        "    </parameters>\n" +
                        "  </node>\n" +
                        "  <node>\n" +
                        "    <id>bar</id>\n" +
                        "    <operator>Op3</operator>\n" +
                        "    <sources>\n" +
                        "      <input1>grunt</input1>\n" +
                        "      <input2>baz</input2>\n" +
                        "    </sources>\n" +
                        "    <parameters>\n" +
                        "       <ignoreSign>true</ignoreSign>\n" +
                        "       <expression>A+B</expression>\n" +
                        "    </parameters>\n" +
                        "  </node>\n" +
                        "</graph>";

        Graph graph = GraphIO.read(new StringReader(xml));
        assertEquals("foo", graph.getId());

        Node[] nodes = graph.getNodes();
        assertNotNull(nodes);
        assertEquals(3, nodes.length);
        assertNotNull(nodes[0]);
        assertNotNull(nodes[1]);

        Node node1 = graph.getNode("grunt");
        Node node2 = graph.getNode("baz");
        Node node3 = graph.getNode("bar");
        assertSame(nodes[0], node1);
        assertSame(nodes[1], node2);
        assertSame(nodes[2], node3);

        assertEquals("Op1", node1.getOperatorName());
        assertEquals("Op2", node2.getOperatorName());
        assertEquals("Op3", node3.getOperatorName());

        NodeSource[] sources1 = node1.getSources();
        assertNotNull(sources1);
        assertEquals(0, sources1.length);

        NodeSource[] sources2 = node2.getSources();
        assertNotNull(sources2);
        assertEquals(1, sources2.length);
        assertEquals("grunt", sources2[0].getSourceNodeId());

        NodeSource[] sources3 = node3.getSources();
        assertNotNull(sources3);
        assertEquals(2, sources3.length);
        assertEquals("grunt", sources3[0].getSourceNodeId());
        assertEquals("baz", sources3[1].getSourceNodeId());

        GraphProcessor processor = new GraphProcessor();
        GraphContext graphContext = null;
        try {
            graphContext = processor.createGraphContext(graph, ProgressMonitor.NULL);
        } catch (GraphException e) {
            fail(e.getMessage());
        }

        NodeContext nodeContext2 = graphContext.getNodeContext(node2);
        TestOps.Op2 op2 = (TestOps.Op2) nodeContext2.getOperator();
        assertEquals(0.86, op2.threshold, 1.e-10);

        NodeContext nodeContext3 = graphContext.getNodeContext(node3);
        TestOps.Op3 op3 = (TestOps.Op3) nodeContext3.getOperator();
        assertEquals(true, op3.ignoreSign);
        assertEquals("A+B", op3.expression);
    }

    private Graph doIO(Graph chain1) {
        StringWriter writer = new StringWriter();
        GraphIO.write(chain1, writer);
        String xml = writer.toString();
        // System.out.println("xml = " + xml);
        StringReader reader = new StringReader(xml);
        return GraphIO.read(reader);
    }
}
