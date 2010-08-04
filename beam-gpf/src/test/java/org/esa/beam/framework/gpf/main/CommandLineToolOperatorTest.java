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

import com.sun.media.jai.util.SunTileScheduler;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.TestOps;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.media.jai.JAI;
import javax.media.jai.TileScheduler;

public class CommandLineToolOperatorTest extends TestCase {
    private OpCommandLineContext context;
    private CommandLineTool clTool;
    private static final TestOps.Op3.Spi OP3_SPI = new TestOps.Op3.Spi();
    private static final TestOps.Op4.Spi OP4_SPI = new TestOps.Op4.Spi();
    private static final TestOps.Op5.Spi OP5_SPI = new TestOps.Op5.Spi();
    private TileScheduler jaiTileScheduler;

    @Override
    protected void setUp() throws Exception {
        context = new OpCommandLineContext();
        clTool = new CommandLineTool(context);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(OP3_SPI);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(OP4_SPI);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(OP5_SPI);
        JAI jai = JAI.getDefaultInstance();
        jaiTileScheduler = jai.getTileScheduler();
        SunTileScheduler tileScheduler = new SunTileScheduler();
        tileScheduler.setParallelism(Runtime.getRuntime().availableProcessors());
        jai.setTileScheduler(tileScheduler);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(OP3_SPI);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(OP5_SPI);
        JAI.getDefaultInstance().setTileScheduler(jaiTileScheduler);
    }

    public void testPrintUsage() throws Exception {
        assertTrue(context.output.length() == 0);
        clTool.run(new String[]{"-h"});
        assertTrue(context.output.startsWith("Usage:\n  gpt <op>|<graph-file> [options] "));

//        System.out.println("\n" + context.output + "\n");
    }

    public void testPrintOp3Usage() throws Exception {
        assertTrue(context.output.length() == 0);
        clTool.run(new String[]{"Op3", "-h"});
        assertTrue(context.output.startsWith("Usage:\n  gpt Op3 [options] "));

//        System.out.println("\n" + context.output + "\n");
    }

    public void testPrintOp4Usage() throws Exception {
        assertTrue(context.output.length() == 0);
        clTool.run(new String[]{"Op4", "-h"});
//        System.out.println("\n" + context.output + "\n");
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
        clTool.run(new String[]{"Op3", "-Sinput1=vercingetorix.dim"});
        assertEquals("s0=" + new File("vercingetorix.dim").getCanonicalPath() + ";o=Op3;t0=" + CommandLineTool.DEFAULT_TARGET_FILEPATH + ";", context.logString);
        assertEquals("Op3", context.opName);
    }

    public void testOperatorTwoSources() throws Exception {
        clTool.run(new String[]{"Op3", "-Sinput1=vercingetorix.dim", "-Sinput2=asterix.N1"});
        String expectedLog = "s0=" + new File("vercingetorix.dim").getCanonicalPath() + ";" +
                "s1=" + new File("asterix.N1").getCanonicalPath() + ";" +
                "o=Op3;t0=" + CommandLineTool.DEFAULT_TARGET_FILEPATH + ";";
        assertEquals(expectedLog, context.logString);
        assertEquals("Op3", context.opName);
        assertNotNull(context.parameters);
    }

    public void testOperatorMultiSources() throws Exception {
        clTool.run(new String[]{"Op5", "-SVincent=vincent.dim", "asterix.N1", "obelix.nc"});
        String expectedLog = "s0=" + new File("vincent.dim").getCanonicalPath() + ";" +
                "s1=" + new File("asterix.N1").getCanonicalPath() + ";" +
                "s2=" + new File("obelix.nc").getCanonicalPath() + ";" +
                "o=Op5;t0=" + CommandLineTool.DEFAULT_TARGET_FILEPATH + ";";
        assertEquals(expectedLog, context.logString);
        assertEquals("Op5", context.opName);
        assertNotNull(context.parameters);
    }

    public void testOperatorTargetProduct() throws Exception {
        clTool.run(new String[]{"Op3", "-t", "obelix.dim"});
        assertEquals("o=Op3;t0=obelix.dim;", context.logString);
        assertEquals("Op3", context.opName);
        assertNotNull(context.parameters);
    }

    public void testOperatorWithParameters() throws Exception {
        clTool.run(new String[]{"Op3", "-Pexpression=log(1+radiance_13)", "-PignoreSign=true", "-Pfactor=-0.025"});
        assertEquals("o=Op3;t0=" + CommandLineTool.DEFAULT_TARGET_FILEPATH + ";", context.logString);
        assertEquals("Op3", context.opName);

        Map<String, Object> parameters = context.parameters;
        assertNotNull(parameters);
        assertEquals(4, parameters.size());
        assertEquals("log(1+radiance_13)", parameters.get("expression"));
        assertEquals(true, parameters.get("ignoreSign"));
        assertEquals(-0.025, parameters.get("factor"));
        assertEquals("NN", parameters.get("interpolMethod"));
    }

    public void testFailureNoReaderFound() {
        CommandLineTool tool = new CommandLineTool(new OpCommandLineContext() {
            @Override
            public Product readProduct(String productFilepath) throws IOException {
                return null;  // returning null to simulate an error
            }

        });
        try {
            tool.run(new String[]{"Op3", "-Sinput1=vercingetorix.dim", "-Sinput2=asterix.N1", "-e"});
            fail("Exception expected for reason: " + "No reader found");
        } catch (Exception e) {
            // expected
        }

    }


    private static class OpCommandLineContext implements CommandLineContext {
        public String logString;
        private int readProductCounter;
        private int writeProductCounter;
        private String opName;
        private Map<String, Object> parameters;
        private Map<String, Product> sourceProducts;
        private String output = "";

        public OpCommandLineContext() {
            logString = "";
        }

        public Product readProduct(String productFilepath) throws IOException {
            logString += "s" + readProductCounter + "=" + productFilepath + ";";
            readProductCounter++;
            return new Product("S", "ST", 10, 10);
        }

        public void writeProduct(Product targetProduct, String filePath, String formatName) throws IOException {
            logString += "t" + writeProductCounter + "=" + filePath + ";";
            writeProductCounter++;
        }

        public Graph readGraph(String filepath, Map<String, String> parameterMap) throws IOException {
            fail("did not expect to come here");
            return null;
        }

        public void executeGraph(Graph graph) throws GraphException {
            fail("did not expect to come here");
        }

        public Map<String, String> readParameterFile(String propertiesFilepath) throws IOException {
            HashMap<String, String> hashMap = new HashMap<String, String>();
            hashMap.put("expression", "sqrt(x*x + y*y)");
            hashMap.put("threshold", "-0.5125");
            return hashMap;
        }

        public Product createOpProduct(String opName, Map<String, Object> parameters, Map<String, Product> sourceProducts) throws OperatorException {
            this.opName = opName;
            this.parameters = parameters;
            this.sourceProducts = sourceProducts;
            logString += "o=" + opName + ";";
            return new Product("T", "TT", 10, 10);
        }

        public void print(String m) {
            this.output += m;
        }
    }

}
