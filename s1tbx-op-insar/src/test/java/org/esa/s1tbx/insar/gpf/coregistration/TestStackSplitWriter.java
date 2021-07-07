/*
 * Copyright (C) 2021 SkyWatch. https://www.skywatch.com
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
package org.esa.s1tbx.insar.gpf.coregistration;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.graph.*;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.StringReader;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class TestStackSplitWriter extends ProcessorTest {

    private final static File inFile1 = new File(S1TBXTests.inputPathProperty + "/SAR/Stack/coregistered_stack.dim");

    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(inFile1 + " not found", inFile1.exists());
    }

    @Test
    public void testStackSplitWriter() throws Exception  {
        final File tmpFolder = createTmpFolder("stackSplit");

        String graphOpXml =
                "<graph id=\"Graph\">\n" +
                        "  <version>1.0</version>\n" +
                        "  <node id=\"Read\">\n" +
                        "    <operator>Read</operator>\n" +
                        "    <sources/>\n" +
                        "    <parameters class=\"com.bc.ceres.binding.dom.XppDomElement\">\n" +
                        "      <file>"+inFile1.getAbsolutePath()+"</file>\n" +
                        "    </parameters>\n" +
                        "  </node>\n" +
                        "  <node id=\"Subset\">\n" +
                        "    <operator>Subset</operator>\n" +
                        "    <sources>\n" +
                        "      <sourceProduct refid=\"Read\"/>\n" +
                        "    </sources>\n" +
                        "    <parameters class=\"com.bc.ceres.binding.dom.XppDomElement\">\n" +
                        "      <sourceBands/>\n" +
                        "      <region>500,100,100,100</region>\n" +
                        "      <referenceBand/>\n" +
                        "      <geoRegion/>\n" +
                        "      <subSamplingX>1</subSamplingX>\n" +
                        "      <subSamplingY>1</subSamplingY>\n" +
                        "      <fullSwath>false</fullSwath>\n" +
                        "      <tiePointGridNames/>\n" +
                        "      <copyMetadata>true</copyMetadata>\n" +
                        "    </parameters>\n" +
                        "  </node>\n" +
                        "  <node id=\"Stack-Split\">\n" +
                        "    <operator>Stack-Split</operator>\n" +
                        "    <sources>\n" +
                        "      <sourceProduct refid=\"Subset\"/>\n" +
                        "    </sources>\n" +
                        "    <parameters class=\"com.bc.ceres.binding.dom.XppDomElement\">\n" +
                        "      <targetFolder>"+tmpFolder.getAbsolutePath()+"</targetFolder>\n" +
                        "      <formatName>BEAM-DIMAP</formatName>\n" +
                        "    </parameters>\n" +
                        "  </node>\n" +
                        "</graph>";
        Graph graph = GraphIO.read(new StringReader(graphOpXml));

        GraphContext graphContext = new GraphContext(graph);
        GraphProcessor processor = new GraphProcessor();
        Product chainOut = processor.executeGraph(graphContext, ProgressMonitor.NULL)[0];

        assertNotNull(chainOut);
        assertEquals("Subset_coregistered_stack", chainOut.getName());

        File referenceFile = new File(tmpFolder, "ASA_IMS_1PNUPA20031203_061259_000000162022_00120_09192_0099.N1.dim");
        assertTrue(referenceFile.exists());
        File secondaryFile = new File(tmpFolder, "ASA_IMS_1PXPDE20040211_061300_000000142024_00120_10194_0013.N1_11Feb2004.dim");
        assertTrue(secondaryFile.exists());

        Product referenceProduct = ProductIO.readProduct(referenceFile);
        assertNotNull(referenceProduct);
        Product secondaryProduct = ProductIO.readProduct(secondaryFile);
        assertNotNull(secondaryProduct);

        assertTrue(referenceProduct.containsBand("i"));
        assertTrue(referenceProduct.containsBand("q"));
        assertTrue(referenceProduct.containsBand("Intensity"));

        assertTrue(secondaryProduct.containsBand("i"));
        assertTrue(secondaryProduct.containsBand("q"));
        assertTrue(secondaryProduct.containsBand("Intensity"));

        secondaryProduct.dispose();
        referenceProduct.dispose();
        chainOut.dispose();

        tmpFolder.delete();
    }
}
