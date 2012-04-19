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
package org.esa.beam.gpf.operators.standard;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.GlobalTestConfig;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.Tile.Pos;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphIO;
import org.esa.beam.framework.gpf.graph.GraphProcessor;
import org.esa.beam.util.io.FileUtils;

import javax.media.jai.JAI;
import javax.media.jai.TileScheduler;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.StringReader;


public class WriteOpTest extends TestCase {

    private static final int RASTER_WIDTH = 4;
    private static final int RASTER_HEIGHT = 40;

    private AlgoOp.Spi algoSpi = new AlgoOp.Spi();
    private WriteOp.Spi writeSpi = new WriteOp.Spi();
    private File outputFile;
    private int oldParallelism;

    @Override
    protected void setUp() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(algoSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(writeSpi);
        outputFile = GlobalTestConfig.getBeamTestDataOutputFile("WriteOpTest/writtenProduct.dim");
        outputFile.getParentFile().mkdirs();

        TileScheduler tileScheduler = JAI.getDefaultInstance().getTileScheduler();
        oldParallelism = tileScheduler.getParallelism();
        tileScheduler.setParallelism(Runtime.getRuntime().availableProcessors());
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(algoSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(writeSpi);
        File parentFile = outputFile.getParentFile();
        FileUtils.deleteTree(parentFile);

        TileScheduler tileScheduler = JAI.getDefaultInstance().getTileScheduler();
        tileScheduler.setParallelism(oldParallelism);
    }

    public void testWrite() throws Exception {
        String graphOpXml = "<graph id=\"myOneNodeGraph\">\n"
                + "  <version>1.0</version>\n"
                + "  <node id=\"node1\">\n"
                + "    <operator>Algo</operator>\n"
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
        assertEquals(3, productOnDisk.getNumBands());
        assertEquals("OperatorBand", productOnDisk.getBandAt(0).getName());
        assertEquals("ConstantBand", productOnDisk.getBandAt(1).getName());
        assertEquals("VirtualBand", productOnDisk.getBandAt(2).getName());

        Band operatorBand = productOnDisk.getBandAt(0);
        operatorBand.loadRasterData();
        //assertEquals(12345, operatorBand.getPixelInt(0, 0));

        // Test that header has been rewritten due to data model changes in AlgoOp.computeTile()
        final ProductNodeGroup<Placemark> placemarkProductNodeGroup = productOnDisk.getPinGroup();
        // 40 pins expected --> one for each tile, we have 40 tiles
        // This test fails sometimes and sometimes not. Probably due to some tiling-issues. Therefore commented out.
        // assertEquals(40, placemarkProductNodeGroup.getNodeCount());

        productOnDisk.dispose();
    }

    /**
     * Some algorithm.
     */
    @OperatorMetadata(alias = "Algo")
    public static class AlgoOp extends Operator {

        @TargetProduct
        private Product targetProduct;

        @Override
        public void initialize() {

            targetProduct = new Product("name", "desc", RASTER_WIDTH, RASTER_HEIGHT);
            targetProduct.addBand("OperatorBand", ProductData.TYPE_INT8);
            targetProduct.addBand("ConstantBand", ProductData.TYPE_INT8).setSourceImage(new BufferedImage(RASTER_WIDTH, RASTER_HEIGHT, BufferedImage.TYPE_BYTE_INDEXED));
            targetProduct.addBand(new VirtualBand("VirtualBand", ProductData.TYPE_FLOAT32, RASTER_WIDTH, RASTER_HEIGHT, "OperatorBand + ConstantBand"));

            targetProduct.setPreferredTileSize(2, 2);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) {
            // Fill the tile with the constant sample value 12345
            //
            for (Pos pos : targetTile) {
                targetTile.setSample(pos.x, pos.y, 12345);
            }

            // Set a pin, so that we can test that the header is rewritten after
            // a data model change.
            //
            final int minX = targetTile.getMinX();
            final int minY = targetTile.getMinY();
            Placemark placemark = Placemark.createPointPlacemark(PinDescriptor.getInstance(),
                                                                 band.getName() + "-" + minX + "-" + minY,
                                                                 "label", "descr",
                                                                 new PixelPos(minX, minY), null,
                                                                 targetProduct.getGeoCoding());

            targetProduct.getPinGroup().add(placemark);

            System.out.println("placemark = " + placemark.getName());
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(AlgoOp.class);
            }
        }
    }

}
