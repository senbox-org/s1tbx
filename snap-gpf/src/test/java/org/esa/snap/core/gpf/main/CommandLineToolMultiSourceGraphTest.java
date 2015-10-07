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

package org.esa.snap.core.gpf.main;

import com.bc.ceres.binding.dom.DomElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.TestOps;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.GraphProcessingObserver;
import org.esa.snap.core.gpf.graph.Node;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class CommandLineToolMultiSourceGraphTest {

    private static final TestOps.Op5.Spi OP5_SPI = new TestOps.Op5.Spi();
    private GraphCommandLineContext context;
    private CommandLineTool clTool;

    @BeforeClass
    public static void setupTest() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(OP5_SPI);
    }

    @AfterClass
    public static void tearDownTest() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(OP5_SPI);
    }

    @Before
    public void setUp() throws Exception {
        context = new GraphCommandLineContext();
        clTool = new CommandLineTool(context);
    }

    @Test
    public void testGraphWithTwoSources() throws Exception {
        final Map<String, String> map = new HashMap<String, String>();
        // todo - generated source IDs are not logical (mz,nf 2012.04.14)
        // we would expect that this one is also valid:
        // map.put("ReadOp@sourceProduct.1", "ernie.dim");

        map.put("ReadOp@sourceProduct", "ernie.dim");
        map.put("ReadOp@sourceProduct.2", "idefix.dim");
        testGraph(new String[]{"graph.xml", "ernie.dim", "idefix.dim"},
                  4,
                  "g=graph.xml;e=chain1;",
                  map,
                  "WriteOp@node1", "target.dim", "BEAM-DIMAP"
        );
    }

    @Test
    public void testGraphWithWith3Sources() throws Exception {
        final Map<String, String> map = new HashMap<String, String>();
        // todo - generated source IDs are not logical (mz,nf 2012.04.14)
        // we would expect that this one is also valid:
        // map.put("ReadOp@sourceProduct.1", "ernie.dim");

        map.put("ReadOp@sourceProduct", "ernie.dim");
        map.put("ReadOp@sourceProduct.2", "idefix.dim");
        map.put("ReadOp@sourceProduct.3", "obelix.dim");
        testGraph(new String[]{"graph.xml", "ernie.dim", "idefix.dim", "obelix.dim"},
                  5,
                  "g=graph.xml;e=chain1;",
                  map,
                  "WriteOp@node1", "target.dim", "BEAM-DIMAP"
        );
    }

    @Test
    public void testGraphWith2SourcesAndOneNamedSource() throws Exception {
        final Map<String, String> map = new HashMap<String, String>();

        // todo - generated source IDs are not logical (mz,nf 2012.04.14)
        // we would expect:
//        map.put("ReadOp@Vincent", "vincent.dim");
//        map.put("ReadOp@sourceProduct.2", "ernie.dim");
//        map.put("ReadOp@sourceProduct.3", "idefix.dim");

        map.put("ReadOp@Vincent", "vincent.dim");
        map.put("ReadOp@sourceProduct", "ernie.dim");
        map.put("ReadOp@sourceProduct.2", "idefix.dim");

        testGraph(new String[]{"graph.xml", "-SVincent=vincent.dim", "ernie.dim", "idefix.dim"},
                  5,
                  "g=graph.xml;e=chain1;",
                  map,
                  "WriteOp@node1",
                  "target.dim",
                  "BEAM-DIMAP"
        );
    }

    @Test
    public void testGraphWithOnlyNamedSources() throws Exception {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("ReadOp@Vincent", "vincent.dim");
        map.put("ReadOp@ernie", "ernie.dim");
        map.put("ReadOp@idefix", "idefix.dim");

        testGraph(new String[]{"graph.xml", "-SVincent=vincent.dim", "-Sernie=ernie.dim", "-Sidefix=idefix.dim"},
                  5,
                  "g=graph.xml;e=chain1;",
                  map,
                  "WriteOp@node1",
                  "target.dim",
                  "BEAM-DIMAP"
        );
    }


    private void testGraph(String[] args,
                           int expectedNodeCount,
                           String expectedLog,
                           Map<String, String> expectedSourceNodeIdFilePathMap,
                           String expectedTargetNodeId,
                           String expectedTargetFilepath,
                           String expectedTargetFormat) throws Exception {
        clTool.run(args);

        assertEquals(expectedLog, context.logString);

        Graph executedGraph = context.executedGraph;
        assertNotNull(executedGraph);
        assertEquals(expectedNodeCount, executedGraph.getNodeCount());

        for (Map.Entry<String, String> entry : expectedSourceNodeIdFilePathMap.entrySet()) {
            String expectedSourceFilePath = entry.getValue();
            if (expectedSourceFilePath != null) {
                final String key = entry.getKey();
                Node generatedReaderNode1 = executedGraph.getNode(key);
                assertNotNull("Source ID not found: " + key, generatedReaderNode1);
                assertEquals(expectedSourceFilePath,
                             generatedReaderNode1.getConfiguration().getChild("file").getValue());
            }

        }
        Node generatedWriterNode = executedGraph.getNode(expectedTargetNodeId);
        assertNotNull(generatedWriterNode);
        assertEquals("node1", generatedWriterNode.getSource(0).getSourceNodeId());

        DomElement parameters = generatedWriterNode.getConfiguration();
        assertNotNull(parameters);
        assertNotNull(expectedTargetFilepath, parameters.getChild("file").getValue());
        assertNotNull(expectedTargetFormat, parameters.getChild("formatName").getValue());
    }


    private static class GraphCommandLineContext implements CommandLineContext {

        private String logString;
        private int readProductCounter;
        private int writeProductCounter;
        private Graph executedGraph;

        private GraphCommandLineContext() {
            logString = "";
        }

        @Override
        public Product readProduct(String productFilepath) throws IOException {
            logString += "s" + readProductCounter + "=" + productFilepath + ";";
            readProductCounter++;
            return new Product("P", "T", 10, 10);
        }

        @Override
        public void writeProduct(Product targetProduct, String filePath, String formatName, boolean clearCacheAfterRowWrite) throws IOException {
            logString += "t" + writeProductCounter + "=" + filePath + ";";
            writeProductCounter++;
        }

        @Override
        public Graph readGraph(String filepath, Map<String, String> templateVariables) throws IOException, GraphException {

            logString += "g=" + filepath + ";";

            String xml =
                    "<graph id=\"chain1\">" +
                            "<version>1.0</version>\n" +
                            "<node id=\"node1\">" +
                            "  <operator>org.esa.snap.core.gpf.TestOps$Op5$Spi</operator>\n" +
                            "  <sources>\n" +
                            "    <sourceProducts>${sourceProducts}</sourceProducts>\n" +
                            "  </sources>\n" +
                            "</node>" +
                            "</graph>";

            return GraphIO.read(new StringReader(xml), templateVariables);
        }

        @Override
        public void executeGraph(Graph graph, GraphProcessingObserver observer) throws GraphException {
            logString += "e=" + graph.getId() + ";";
            executedGraph = graph;
        }


        @Override
        public void print(String m) {
        }

        @Override
        public Logger getLogger() {
            return Logger.getLogger("test");
        }

        @Override
        public Reader createReader(String fileName) throws FileNotFoundException {
            return new StringReader(fileName);
        }

        @Override
        public Writer createWriter(String fileName) throws IOException {
            return new StringWriter();
        }

        @Override
        public String[] list(String path) throws IOException {
            return new String[0];
        }

        @Override
        public boolean isFile(String path) {
            return true;
        }

        @Override
        public boolean fileExists(String fileName) {
            return false;
        }
    }
}
