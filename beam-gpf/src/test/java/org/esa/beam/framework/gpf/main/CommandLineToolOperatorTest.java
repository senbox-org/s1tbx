/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.gpf.TestOps;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.gpf.graph.GraphProcessingObserver;
import org.esa.beam.framework.gpf.internal.OperatorContext;
import org.esa.beam.framework.gpf.internal.OperatorProductReader;

import javax.media.jai.JAI;
import javax.media.jai.TileScheduler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Logger;

public class CommandLineToolOperatorTest extends TestCase {

    private OpCommandLineContext context;
    private CommandLineTool clTool;
    private static final TestOps.Op3.Spi OP3_SPI = new TestOps.Op3.Spi();
    private static final TestOps.Op4.Spi OP4_SPI = new TestOps.Op4.Spi();
    private static final TestOps.Op5.Spi OP5_SPI = new TestOps.Op5.Spi();
    private static final TestOps.OpImplementingOutput.Spi OUTPUT_OP_SPI = new TestOps.OpImplementingOutput.Spi();
    private TileScheduler jaiTileScheduler;

    @Override
    protected void setUp() throws Exception {
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

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(OP3_SPI);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(OP4_SPI);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(OP5_SPI);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(OUTPUT_OP_SPI);
        JAI.getDefaultInstance().setTileScheduler(jaiTileScheduler);
    }

    public void testPrintUsage() throws Exception {
        assertTrue(context.output.length() == 0);
        clTool.run("-h");
        assertTrue(context.output.startsWith("Usage:\n  gpt <op>|<graph-file> [options] "));
    }

    public void testPrintOp3Usage() throws Exception {
        assertTrue(context.output.length() == 0);
        clTool.run("Op3", "-h");
        assertTrue(context.output.startsWith("Usage:\n  gpt Op3 [options] "));
    }

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

    public void testOperatorSingleSource() throws Exception {
        clTool.run("Op3", "-Sinput1=vercingetorix.dim");
        assertEquals("s0=" + new File(
                "vercingetorix.dim").getCanonicalPath() + ";o=Op3;t0=" + CommandLineArgs.DEFAULT_TARGET_FILEPATH + ";",
                     context.logString);
        assertEquals("Op3", context.opName);
    }

    public void testOperatorTwoSources() throws Exception {
        clTool.run("Op3", "-Sinput1=vercingetorix.dim", "-Sinput2=asterix.N1");
        String expectedLog = "s0=" + new File("vercingetorix.dim").getCanonicalPath() + ";" +
                "s1=" + new File("asterix.N1").getCanonicalPath() + ";" +
                "o=Op3;t0=" + CommandLineArgs.DEFAULT_TARGET_FILEPATH + ";";
        assertEquals(expectedLog, context.logString);
        assertEquals("Op3", context.opName);
        assertNotNull(context.parameters);
    }

    public void testOperatorMultiSources() throws Exception {
        clTool.run("Op5", "-SVincent=vincent.dim", "asterix.N1", "obelix.nc");
        String expectedLog = "s0=" + new File("vincent.dim").getCanonicalPath() + ";" +
                "s1=" + new File("asterix.N1").getCanonicalPath() + ";" +
                "s2=" + new File("obelix.nc").getCanonicalPath() + ";" +
                "o=Op5;t0=" + CommandLineArgs.DEFAULT_TARGET_FILEPATH + ";";
        assertEquals(expectedLog, context.logString);
        assertEquals("Op5", context.opName);
        assertNotNull(context.parameters);
    }

    public void testOperatorTargetProduct() throws Exception {
        clTool.run("Op3", "-t", "obelix.dim");
        assertEquals("o=Op3;t0=obelix.dim;", context.logString);
        assertEquals("Op3", context.opName);
        assertNotNull(context.parameters);
    }

    public void testOperatorWithParametersFromLineArgs() throws Exception {
        clTool.run("Op3", "-Pexpression=log(1+radiance_13)", "-PignoreSign=true", "-Pfactor=-0.025");
        assertEquals("o=Op3;t0=" + CommandLineArgs.DEFAULT_TARGET_FILEPATH + ";", context.logString);
        assertEquals("Op3", context.opName);

        Map<String, Object> parameters = context.parameters;
        assertNotNull(parameters);
        assertEquals(4, parameters.size());
        assertEquals("log(1+radiance_13)", parameters.get("expression"));
        assertEquals(true, parameters.get("ignoreSign"));
        assertEquals(-0.025, parameters.get("factor"));
        assertEquals("NN", parameters.get("interpolMethod"));
    }

    public void testOperatorWithParametersFromFile() throws Exception {
        clTool.run("Op3", "-p", "testOperatorWithParametersFromFile");
        assertEquals("o=Op3;t0=" + CommandLineArgs.DEFAULT_TARGET_FILEPATH + ";", context.logString);
        assertEquals("Op3", context.opName);

        Map<String, Object> parameters = context.parameters;
        assertNotNull(parameters);
        assertEquals(4, parameters.size());
        assertEquals("log(2+radiance_13)", parameters.get("expression"));
        assertEquals(true, parameters.get("ignoreSign"));
        assertEquals(-0.035, parameters.get("factor"));
        assertEquals("NN", parameters.get("interpolMethod"));
    }

    public void testOperatorWithParametersFromXMLFile() throws Exception {
        clTool.run("Op3", "-p", "testOperatorWithParametersFromXMLFile");
        assertEquals("o=Op3;t0=" + CommandLineArgs.DEFAULT_TARGET_FILEPATH + ";", context.logString);
        assertEquals("Op3", context.opName);

        Map<String, Object> parameters = context.parameters;
        assertNotNull(parameters);
        assertEquals(4, parameters.size());
        assertEquals("log(2+radiance_13)", parameters.get("expression"));
        assertEquals(true, parameters.get("ignoreSign"));
        assertEquals(-0.035, parameters.get("factor"));
        assertEquals("NN", parameters.get("interpolMethod"));
    }


    public void testThatOperatorLineArgsOverwriteParametersFromFile() throws Exception {
        clTool.run("Op3", "-p", "testOperatorWithParametersFromFile", "-Pfactor=0.99");
        assertEquals("o=Op3;t0=" + CommandLineArgs.DEFAULT_TARGET_FILEPATH + ";", context.logString);
        assertEquals("Op3", context.opName);

        Map<String, Object> parameters = context.parameters;
        assertNotNull(parameters);
        assertEquals(4, parameters.size());
        assertEquals("log(2+radiance_13)", parameters.get("expression"));
        assertEquals(true, parameters.get("ignoreSign"));
        assertEquals(0.99, parameters.get("factor"));
        assertEquals("NN", parameters.get("interpolMethod"));
    }

    public void testOperatorImplementingOutputInterface() throws Exception {
        clTool.run("OutputOp");
        assertEquals(0, context.writeProductCounter);
    }

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


    private static class OpCommandLineContext implements CommandLineContext {

        private String logString;
        private int readProductCounter;
        private int writeProductCounter;
        private String opName;
        private Map<String, Object> parameters;
        private Map<String, Product> sourceProducts;
        private String output = "";

        private OpCommandLineContext() {
            logString = "";
        }

        @Override
        public Product readProduct(String productFilepath) throws IOException {
            logString += "s" + readProductCounter + "=" + productFilepath + ";";
            readProductCounter++;
            return new Product("S", "ST", 10, 10);
        }

        @Override
        public void writeProduct(Product targetProduct, String filePath, String formatName,
                                 boolean clearCacheAfterRowWrite) throws IOException {
            logString += "t" + writeProductCounter + "=" + filePath + ";";
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
            if ("testOperatorWithParametersFromFile".equals(fileName)) {
                return new StringReader("expression = log(2+radiance_13)\n" +
                                                "ignoreSign = true\n" +
                                                "factor = -0.035");
            } else if ("testOperatorWithParametersFromXMLFile".equals(fileName)) {
                return new StringReader(
                        "<parameters>" +
                                "<expression>log(2+radiance_13)</expression>" +
                                "<ignoreSign>true</ignoreSign>" +
                                "<factor>-0.035</factor>" +
                                "</parameters>");
            } else {
                return new StringReader("expression = sqrt(x*x + y*y)\n"
                                                + "threshold = -0.5125");
            }
        }

        @Override
        public Product createOpProduct(String opName, Map<String, Object> parameters,
                                       Map<String, Product> sourceProducts) throws OperatorException {
            this.opName = opName;
            this.parameters = parameters;
            this.sourceProducts = sourceProducts;
            logString += "o=" + opName + ";";

            final Product product = new Product("T", "TT", 10, 10);
            final OperatorSpiRegistry operatorSpiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
            final Operator operator = operatorSpiRegistry.getOperatorSpi(opName).createOperator();
            product.setProductReader(new OperatorProductReader(new OperatorContext(operator)));
            return product;
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
