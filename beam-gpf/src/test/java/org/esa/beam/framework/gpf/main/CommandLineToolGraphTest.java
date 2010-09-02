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

package org.esa.beam.framework.gpf.main;

import com.bc.ceres.binding.dom.DomElement;
import com.sun.media.jai.util.SunTileScheduler;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.TestOps;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.gpf.graph.GraphIO;
import org.esa.beam.framework.gpf.graph.Node;

import javax.media.jai.JAI;
import javax.media.jai.TileScheduler;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class CommandLineToolGraphTest extends TestCase {

    private String writableGraphXml = getWritableGraphXml();
    private GraphCommandLineContext writableContext;
    private String nonWritableGraphXml = getNonWritableGraphXml();
    private GraphCommandLineContext nonWritableContext;
    private TileScheduler jaiTileScheduler;

    private static final TestOps.Op2.Spi OP2_SPI = new TestOps.Op2.Spi();
    private static final TestOps.Op3.Spi OP3_SPI = new TestOps.Op3.Spi();
    private static final TestOps.NonWritableOp.Spi NON_WRITABLE_OP_SPI = new TestOps.NonWritableOp.Spi();

    @Override
    protected void setUp() throws Exception {
        writableContext = new GraphCommandLineContext(writableGraphXml);
        nonWritableContext = new GraphCommandLineContext(nonWritableGraphXml);
        JAI jai = JAI.getDefaultInstance();
        jaiTileScheduler = jai.getTileScheduler();
        SunTileScheduler tileScheduler = new SunTileScheduler();
        tileScheduler.setParallelism(Runtime.getRuntime().availableProcessors());
        jai.setTileScheduler(tileScheduler);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(OP2_SPI);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(OP3_SPI);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(NON_WRITABLE_OP_SPI);
    }

    @Override
    protected void tearDown() throws Exception {
        JAI.getDefaultInstance().setTileScheduler(jaiTileScheduler);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(OP2_SPI);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(OP3_SPI);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(NON_WRITABLE_OP_SPI);
    }

    public void testGraphUsageMessage() throws Exception {
        final String[] args = new String[]{"-h", "graph.xml"};
        GraphCommandLineContext context = new GraphCommandLineContext(writableGraphXml);
        final CommandLineTool clTool = new CommandLineTool(context);
        clTool.run(args);

        final String message = context.m;
        assertNotNull(message);
        assertTrue(message.contains("Usage:"));
        assertTrue(message.contains("Source Options:"));
        assertTrue(message.contains("sourceProduct1"));
        assertTrue(message.contains("First source product"));
        assertTrue(message.contains("sourceProduct2"));
        assertTrue(message.contains("Parameter Options:"));
        assertTrue(message.contains("threshold"));
        assertTrue(message.contains("Threshold value"));
        assertTrue(message.contains("expression"));

        //System.out.println(message);
    }

    public void testGraphOnly() throws Exception {
        testGraph(writableContext, new String[]{"graph.xml"},
                  3,
                  "g=graph.xml;e=chain1;",
                  "${sourceProduct}", null,
                  "${sourceProduct2}", null,
                  "WriteProduct$node2",
                  "target.dim",
                  "BEAM-DIMAP",
                  "${threshold}",
                  "${expression}"
        );
    }

    public void testGraphWithParameters() throws Exception {
        testGraph(writableContext, new String[]{"graph.xml", "-Pexpression=a+b/c", "-Pthreshold=2.5"},
                  3,
                  "g=graph.xml;e=chain1;",
                  "${sourceProduct}", null,
                  "${sourceProduct2}", null,
                  "WriteProduct$node2", "target.dim", "BEAM-DIMAP", "2.5",
                  "a+b/c"
        );
    }

    public void testGraphWithParametersAndSourceArgs() throws Exception {
        testGraph(writableContext,
                  new String[]{"graph.xml", "-Pexpression=a+b/c", "-Pthreshold=2.5", "ernie.dim", "idefix.dim"},
                  5,
                  "g=graph.xml;e=chain1;",
                  "ReadProduct$0", "ernie.dim",
                  "ReadProduct$1", "idefix.dim",
                  "WriteProduct$node2", "target.dim", "BEAM-DIMAP", "2.5",
                  "a+b/c"
        );
    }

    public void testGraphWithParametersAndSourceOptions() throws Exception {
        testGraph(writableContext, new String[]{
                "graph.xml",
                "-Pexpression=a+b/c",
                "-Pthreshold=2.5",
                "-SsourceProduct=ernie.dim",
                "-SsourceProduct2=idefix.dim"
        },
                  5,
                  "g=graph.xml;e=chain1;",
                  "ReadProduct$0",
                  "ernie.dim",
                  "ReadProduct$1",
                  "idefix.dim",
                  "WriteProduct$node2",
                  "target.dim",
                  "BEAM-DIMAP",
                  "2.5",
                  "a+b/c"
        );
    }

    public void testGraphWithParametersFileOption() throws Exception {
        testGraph(writableContext, new String[]{
                "graph.xml",
                "-p",
                "paramFile.properties",
                "-SsourceProduct=ernie.dim",
                "-SsourceProduct2=idefix.dim"
        },
                  5,
                  "g=graph.xml;e=chain1;",
                  "ReadProduct$0",
                  "ernie.dim",
                  "ReadProduct$1",
                  "idefix.dim",
                  "WriteProduct$node2",
                  "target.dim",
                  "BEAM-DIMAP",
                  "-0.5125",
                  "sqrt(x*x + y*y)"
        );
    }

    public void testGraphWithParametersFileOptionIsOverwrittenByOption() throws Exception {
        testGraph(writableContext, new String[]{
                "graph.xml",
                "-p",
                "paramFile.properties",
                "-Pexpression=atan(y/x)",
                "-SsourceProduct=ernie.dim",
                "-SsourceProduct2=idefix.dim"
        },
                  5,
                  "g=graph.xml;e=chain1;",
                  "ReadProduct$0",
                  "ernie.dim",
                  "ReadProduct$1",
                  "idefix.dim",
                  "WriteProduct$node2",
                  "target.dim",
                  "BEAM-DIMAP",
                  "-0.5125",
                  "atan(y/x)"
        );

    }

    public void testNonWritableGraph() throws Exception {
        testGraphNonWritable(nonWritableContext, new String[]{
                "graph.xml",
                "-p",
                "paramFile.properties",
                "-SsourceProduct1=ernie.dim"
        },
                             3,
                             "g=graph.xml;e=chain1;",
                             "ReadProduct$0",
                             "ernie.dim"
        );

    }


    private void testGraph(GraphCommandLineContext context,
                           String[] args,
                           int expectedNodeCount,
                           String expectedLog,
                           String expectedSourceNodeId1,
                           String expectedSourceFilepath1,
                           String expectedSourceNodeId2,
                           String expectedSourceFilepath2,
                           String expectedTargetNodeId,
                           String expectedTargetFilepath,
                           String expectedTargetFormat,
                           String expectedThreshold,
                           String expectedExpression) throws Exception {
        CommandLineTool clTool = new CommandLineTool(context);
        clTool.run(args);

        assertEquals(expectedLog, context.logString);

        Graph executedGraph = context.executedGraph;
        assertNotNull(executedGraph);
        assertEquals(expectedNodeCount, executedGraph.getNodeCount());

        Node node1 = executedGraph.getNode("node1");
        assertEquals(expectedSourceNodeId1, node1.getSource(0).getSourceNodeId());
        assertEquals(expectedThreshold, node1.getConfiguration().getChild("threshold").getValue());

        Node node2 = executedGraph.getNode("node2");
        assertEquals("node1", node2.getSource(0).getSourceNodeId());
        assertEquals(expectedSourceNodeId2, node2.getSource(1).getSourceNodeId());
        assertEquals(expectedExpression, node2.getConfiguration().getChild("expression").getValue());

        if (expectedSourceFilepath1 != null) {
            Node generatedReaderNode1 = executedGraph.getNode(expectedSourceNodeId1);
            assertNotNull(generatedReaderNode1);
            assertEquals(expectedSourceFilepath1, generatedReaderNode1.getConfiguration().getChild("file").getValue());
        }

        if (expectedSourceFilepath2 != null) {
            Node generatedReaderNode2 = executedGraph.getNode(expectedSourceNodeId2);
            assertNotNull(generatedReaderNode2);
            assertEquals(expectedSourceFilepath2, generatedReaderNode2.getConfiguration().getChild("file").getValue());
        }

            Node generatedWriterNode = executedGraph.getNode(expectedTargetNodeId);
            assertNotNull(generatedWriterNode);
            assertEquals("node2", generatedWriterNode.getSource(0).getSourceNodeId());
            DomElement parameters = generatedWriterNode.getConfiguration();
            assertNotNull(parameters);
            assertNotNull(expectedTargetFilepath, parameters.getChild("file").getValue());
            assertNotNull(expectedTargetFormat, parameters.getChild("formatName").getValue());

    }

    private void testGraphNonWritable(GraphCommandLineContext context,
                                      String[] args,
                                      int expectedNodeCount,
                                      String expectedLog,
                                      String expectedSourceNodeId1,
                                      String expectedSourceFilepath1) throws Exception {
        CommandLineTool clTool = new CommandLineTool(context);
        clTool.run(args);

        assertEquals(expectedLog, context.logString);

        Graph executedGraph = context.executedGraph;
        assertNotNull(executedGraph);
        assertEquals(expectedNodeCount, executedGraph.getNodeCount());

        Node node1 = executedGraph.getNode("node1");
        assertEquals(expectedSourceNodeId1, node1.getSource(0).getSourceNodeId());

        Node node2 = executedGraph.getNode("node2");
        assertEquals("node1", node2.getSource(0).getSourceNodeId());

        Node generatedReaderNode1 = executedGraph.getNode(expectedSourceNodeId1);
        assertNotNull(generatedReaderNode1);
        assertEquals(expectedSourceFilepath1, generatedReaderNode1.getConfiguration().getChild("file").getValue());
    }


    private static class GraphCommandLineContext implements CommandLineContext {

        public String logString;
        private int readProductCounter;
        private int writeProductCounter;
        public Graph executedGraph;
        private String m = "";
        private String graphXml;

        public GraphCommandLineContext(String graphXml) {
            logString = "";
            this.graphXml = graphXml;
        }

        @Override
        public Product readProduct(String productFilepath) throws IOException {
            logString += "s" + readProductCounter + "=" + productFilepath + ";";
            readProductCounter++;
            return new Product("P", "T", 10, 10);
        }

        @Override
        public void writeProduct(Product targetProduct, String filePath, String formatName) throws IOException {
            logString += "t" + writeProductCounter + "=" + filePath + ";";
            writeProductCounter++;
        }

        @Override
        public Graph readGraph(String filepath, Map<String, String> parameterMap) throws IOException, GraphException {

            logString += "g=" + filepath + ";";

            return GraphIO.read(new StringReader(graphXml), parameterMap);
        }

        @Override
        public void executeGraph(Graph graph) throws GraphException {
            logString += "e=" + graph.getId() + ";";
            executedGraph = graph;
        }


        @Override
        public Product createOpProduct(String opName, Map<String, Object> parameters,
                                       Map<String, Product> sourceProducts) throws OperatorException {
            fail("did not expect to come here");
            return null;
        }

        @Override
        public Map<String, String> readParameterFile(String propertiesFilepath) throws IOException {
            HashMap<String, String> hashMap = new HashMap<String, String>();
            hashMap.put("expression", "sqrt(x*x + y*y)");
            hashMap.put("threshold", "-0.5125");
            return hashMap;
        }

        @Override
        public void print(String m) {
            this.m += m;
        }
    }

    private String getWritableGraphXml() {
        return "<graph id=\"chain1\">" +
               "<version>1.0</version>\n" +
               "<header>\n" +
               "<target refid=\"node2\"/>\n" +
               "<source name=\"sourceProduct1\" description=\"First source product\"/>\n" +
               "<source name=\"sourceProduct2\"/>\n" +
               "<parameter name=\"threshold\" type=\"double\" description=\"Threshold value\"/>\n" +
               "<parameter name=\"expression\" type=\"String\"/>\n" +
               "</header>\n" +
               "<node id=\"node1\">" +
               "  <operator>org.esa.beam.framework.gpf.TestOps$Op2$Spi</operator>\n" +
               "  <sources>\n" +
               "    <input>${sourceProduct}</input>\n" +
               "  </sources>\n" +
               "  <parameters>\n" +
               "    <threshold>${threshold}</threshold>\n" +
               "  </parameters>\n" +
               "</node>" +
               "<node id=\"node2\">" +
               "  <operator>org.esa.beam.framework.gpf.TestOps$Op3$Spi</operator>\n" +
               "  <sources>\n" +
               "    <input1 refid=\"node1\"/>\n" +
               "    <input2>${sourceProduct2}</input2>\n" +
               "  </sources>\n" +
               "  <parameters>\n" +
               "    <expression>${expression}</expression>\n" +
               "  </parameters>\n" +
               "</node>" +
               "</graph>";
    }

    private String getNonWritableGraphXml() {
        return "<graph id=\"chain1\">" +
               "<version>1.0</version>\n" +
               "<header>\n" +
               "<target refid=\"node2\"/>\n" +
               "<source name=\"sourceProduct1\" description=\"First source product\"/>\n" +
               "</header>\n" +
               "<node id=\"node1\">" +
               "  <operator>org.esa.beam.framework.gpf.TestOps$Op2$Spi</operator>\n" +
               "  <sources>\n" +
               "    <input>${sourceProduct1}</input>\n" +
               "  </sources>\n" +
               "</node>" +
               "<node id=\"node2\">" +
               "  <operator>org.esa.beam.framework.gpf.TestOps$NonWritableOp$Spi</operator>\n" +
               "  <sources>\n" +
               "    <input refid=\"node1\"/>\n" +
               "  </sources>\n" +
               "</node>" +
               "</graph>";
    }
}
