/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.sun.media.jai.util.SunTileScheduler;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.TestOps;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.gpf.graph.GraphProcessingObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.media.jai.JAI;
import javax.media.jai.TileScheduler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class CommandLineToolOperatorTest {

    private OpCommandLineContext context;
    private CommandLineTool clTool;
    private static final TestOps.Op3.Spi OP3_SPI = new TestOps.Op3.Spi();
    private static final TestOps.Op4.Spi OP4_SPI = new TestOps.Op4.Spi();
    private static final TestOps.Op5.Spi OP5_SPI = new TestOps.Op5.Spi();
    private static final TestOps.OpImplementingOutput.Spi OUTPUT_OP_SPI = new TestOps.OpImplementingOutput.Spi();
    private TileScheduler jaiTileScheduler;

    @Before
    public void setUp() throws Exception {
        context = new OpCommandLineContext();
        clTool = new CommandLineTool(context);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(OP3_SPI);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(OP4_SPI);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(OP5_SPI);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(OUTPUT_OP_SPI);
        JAI jai = JAI.getDefaultInstance();
        jaiTileScheduler = jai.getTileScheduler();
        SunTileScheduler tileScheduler = new SunTileScheduler();
        tileScheduler.setParallelism(Runtime.getRuntime().availableProcessors());
        jai.setTileScheduler(tileScheduler);
    }

    @After
    public void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(OP3_SPI);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(OP4_SPI);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(OP5_SPI);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(OUTPUT_OP_SPI);
        JAI.getDefaultInstance().setTileScheduler(jaiTileScheduler);
    }

    @Test
    public void testPrintUsage() throws Exception {
        assertTrue(context.output.length() == 0);
        clTool.run("-h");
        assertTrue(context.output.startsWith("Usage:\n  gpt <op>|<graph-file> [options] "));
    }

    @Test
    public void testPrintOp3Usage() throws Exception {
        assertTrue(context.output.length() == 0);
        clTool.run("Op3", "-h");
        assertTrue(context.output.startsWith("Usage:\n  gpt Op3 [options] "));
    }

    @Test
    public void testPrintOp4Usage() throws Exception {
        assertTrue(context.output.length() == 0);
        clTool.run("Op4", "-h");
        assertTrue(context.output.startsWith(
                "Usage:\n" +
                        "  gpt Op4 [options] \n" +
                        "\n" +
                        "Computed Properties:\n" +
                        "String[] names\n" +
                        "double PI         The ratio of any circle's circumference to its diameter"
        ));

    }

    @Test
    public void testOperatorImplementingOutputInterface() throws Exception {
        clTool.run("OutputOp");
        assertEquals(0, context.writeProductCounter);
    }

    @Test
    public void testFailureNoReaderFound() {
        CommandLineTool tool = new CommandLineTool(new OpCommandLineContext() {
            @Override
            public Product readProduct(String productFilepath) {
                return null;  // returning null to simulate an error
            }
        });

        try {
            tool.run("Op3", "-Sinput1=vercingetorix.dim", "-Sinput2=asterix.N1");
            fail("Exception expected for reason: " + "No reader found");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void testSystemProperty() throws Exception {
        assertTrue(context.output.length() == 0);
        assertNull(System.getProperty("foo"));
        int originalSize = System.getProperties().size();
        clTool.run("Op4", "-Dfoo=bar");
        assertEquals("bar", System.getProperty("foo"));
        assertEquals(originalSize + 1, System.getProperties().size());
    }

    private static class OpCommandLineContext implements CommandLineContext {

        private int writeProductCounter;
        private String output = "";

        private OpCommandLineContext() {

        }

        @Override
        public Product readProduct(String productFilepath) throws IOException {
            return new Product("S", "ST", 10, 10);
        }

        @Override
        public void writeProduct(Product targetProduct, String filePath, String formatName,
                                 boolean clearCacheAfterRowWrite) throws IOException {
            writeProductCounter++;
        }

        @Override
        public Graph readGraph(String filepath, Map<String, String> templateVariables) throws IOException {
            fail("did not expect to come here");
            return null;
        }

        @Override
        public void executeGraph(Graph graph, GraphProcessingObserver observer) throws GraphException {
            fail("did not expect to come here");
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
        public Reader createReader(String fileName) throws FileNotFoundException {
            switch (fileName) {
                case "testOperatorWithParametersFromFile":
                    return new StringReader("expression = log(2+radiance_13)\n" +
                                            "ignoreSign = true\n" +
                                            "factor = -0.035");
                case "testOperatorWithParametersFromXMLFile":
                    return new StringReader(
                            "<parameters>" +
                            "<expression>log(2+radiance_13)</expression>" +
                            "<ignoreSign>true</ignoreSign>" +
                            "<factor>-0.035</factor>" +
                            "</parameters>"
                    );
                default:
                    return new StringReader("expression = sqrt(x*x + y*y)\n" +
                                            "threshold = -0.5125");
            }
        }

        @Override
        public void print(String m) {
            this.output += m;
        }

        @Override
        public Logger getLogger() {
            return Logger.getLogger("test");
        }

        @Override
        public boolean fileExists(String fileName) {
            return false;
        }
    }

}
