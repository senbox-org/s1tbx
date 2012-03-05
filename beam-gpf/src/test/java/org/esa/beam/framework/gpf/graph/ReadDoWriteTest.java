/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.gpf.graph;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.GlobalTestConfig;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.gpf.operators.standard.ReadOp;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;
import java.io.File;
import java.io.StringReader;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class ReadDoWriteTest {

    private ReadOp.Spi readSpi = new ReadOp.Spi();
    private DoOp.Spi doSpi = new DoOp.Spi();
    private WriteOp.Spi writeSpi = new WriteOp.Spi();
    private File outputFile;
    private File inputFile;

    @Before
    public void setUp() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(readSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(doSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(writeSpi);
        URI inputURI = GPFFacadeTest.class.getResource("test-product.dim").toURI();
        inputFile = new File(inputURI);
        outputFile = GlobalTestConfig.getBeamTestDataOutputFile("ReadDoWriteTest/writtenProduct.dim");
        outputFile.getParentFile().mkdirs();
    }

    @After
    public void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(readSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(doSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(writeSpi);
        File parentFile = outputFile.getParentFile();
        FileUtils.deleteTree(parentFile);
    }

    @Test
    public void testWrite() throws Exception {
        String graphOpXml = "<graph id=\"myOneNodeGraph\">\n"
                + "  <version>1.0</version>\n"
                + "  <node id=\"node0\">\n"
                + "    <operator>Read</operator>\n"
                + "    <parameters>\n"
                + "       <file>" + inputFile.getAbsolutePath() + "</file>\n"
                + "    </parameters>\n"                
                + "  </node>\n"
                + "  <node id=\"node1\">\n"
                + "    <operator>Do</operator>\n"
                + "    <sources>\n"
                + "      <source refid=\"node0\"/>\n"
                + "    </sources>\n"
                + "  </node>\n"
                + "  <node id=\"node2\">\n"
                + "    <operator>Write</operator>\n"
                + "    <sources>\n"
                + "      <source refid=\"node1\"/>\n"
                + "    </sources>\n"
                + "    <parameters>\n"
                + "       <file>" + outputFile.getAbsolutePath() + "</file>\n"
                + "       <deleteOutputOnFailure>false</deleteOutputOnFailure>\n"
                + "    </parameters>\n"
                + "  </node>\n"
                + "</graph>";
        StringReader reader = new StringReader(graphOpXml);
        Graph graph = GraphIO.read(reader);

        GraphProcessor processor = new GraphProcessor();
        processor.executeGraph(graph, ProgressMonitor.NULL);

        Product productOnDisk = ProductIO.readProduct(outputFile);
        assertNotNull(productOnDisk);

        assertEquals("writtenProduct", productOnDisk.getName());
        assertEquals(4, productOnDisk.getNumBands());
        assertEquals("OperatorBand", productOnDisk.getBandAt(0).getName());
        assertEquals("ConstantBand", productOnDisk.getBandAt(1).getName());
        assertEquals("VirtualBand", productOnDisk.getBandAt(2).getName());
        assertEquals("forrest_abundance", productOnDisk.getBandAt(3).getName());

        Band operatorBand = productOnDisk.getBandAt(0);
        operatorBand.loadRasterData();
        assertEquals(42, operatorBand.getPixelInt(0, 0));

        Band constBand = productOnDisk.getBandAt(1);
        constBand.loadRasterData();
        assertEquals(66.6f, constBand.getPixelFloat(0, 0), 1e-6);

        Band virtualBand = productOnDisk.getBandAt(2);
        virtualBand.loadRasterData();
        assertEquals(42 + 66.6f, virtualBand.getPixelFloat(0, 0), 1e-6);

        Band transferedBand = productOnDisk.getBandAt(3);
        transferedBand.loadRasterData();
        assertEquals(0.567, transferedBand.getPixelFloat(0, 0), 0.01);

        productOnDisk.dispose();
    }
    
    /**
     * Do something.....
     */
    @OperatorMetadata(alias = "Do")
    public static class DoOp extends Operator {

        @SourceProduct(alias = "source")
        private Product sourceProduct;
        @TargetProduct
        private Product targetProduct;

        @Override
        public void initialize() {
            int width = sourceProduct.getSceneRasterWidth();
            int height = sourceProduct.getSceneRasterHeight();

            PlanarImage constImage = ConstantDescriptor.create((float) width, (float) height, new Float[]{66.6f}, null);

            targetProduct = new Product("name", "desc", width, height);
            targetProduct.addBand("OperatorBand", ProductData.TYPE_INT8);
            targetProduct.addBand("ConstantBand", ProductData.TYPE_FLOAT32).setSourceImage(constImage);
            targetProduct.addBand(new VirtualBand("VirtualBand", ProductData.TYPE_FLOAT32, width, height, "OperatorBand + ConstantBand"));
            ProductUtils.copyBand(sourceProduct.getBandAt(0).getName(), sourceProduct, targetProduct, true);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) {
            if (!band.getName().equals("OperatorBand")) {
                throw new OperatorException("operator called for wrong band: "+ band.getName());
            }
            // Fill the tile with the constant sample value 42
            //
            for (Tile.Pos pos : targetTile) {
                targetTile.setSample(pos.x, pos.y, 42);
            }
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(DoOp.class);
            }
        }
    }


}
