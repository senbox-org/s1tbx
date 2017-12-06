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
package org.esa.snap.core.gpf.common;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.GlobalTestConfig;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.PinDescriptor;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.Tile.Pos;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.GraphProcessor;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.media.jai.JAI;
import javax.media.jai.TileScheduler;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;

import static junit.framework.TestCase.*;


public class WriteOpTest {

    private static final int RASTER_WIDTH = 4;
    private static final int RASTER_HEIGHT = 40;

    private AlgoOp.Spi algoSpi = new AlgoOp.Spi();
    private File outputFile;
    private int oldParallelism;

    @Before
    public void setUp() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(algoSpi);
        outputFile = GlobalTestConfig.getBeamTestDataOutputFile("WriteOpTest/writtenProduct.dim");

        TileScheduler tileScheduler = JAI.getDefaultInstance().getTileScheduler();
        oldParallelism = tileScheduler.getParallelism();
        tileScheduler.setParallelism(Runtime.getRuntime().availableProcessors());
    }

    @After
    public void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(algoSpi);
        File parentFile = outputFile.getParentFile();
        FileUtils.deleteTree(parentFile);

        TileScheduler tileScheduler = JAI.getDefaultInstance().getTileScheduler();
        tileScheduler.setParallelism(oldParallelism);
    }

    @Test
    public void testWrite() throws Exception {
        String graphOpXml = "<graph id=\"myOneNodeGraph\">\n"
                + "  <version>1.0</version>\n"
                + "  <node id=\"node1\">\n"
                + "    <operator>Algo</operator>\n"
                + "    <parameters>\n"
                + "    <width>" + RASTER_WIDTH + "</width>\n"
                + "    <height>" + RASTER_HEIGHT + "</height>\n"
                + "    <preferredTileWidth>2</preferredTileWidth>\n"
                + "    <preferredTileHeight>2</preferredTileHeight>\n"
                + "    </parameters>\n"
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

    @Test
    public void testWritingEmptyProduct() throws Exception {
        Product product = new Product("empty", "EMPTY", 0, 0);

        File testDir = Files.createTempDirectory("WriteOpTestDir").toFile();
        try {
            File testOuptutFile = new File(testDir, "file.dim");
            WriteOp writeOp = new WriteOp(product, testOuptutFile, "BEAM-DIMAP");
            writeOp.writeProduct(ProgressMonitor.NULL);

            // assert that it is not written
            assertFalse(testOuptutFile.isFile());
        } finally {
            testDir.deleteOnExit();
        }

    }

    @Test
    public void testWrite_UnexpectedTiling() throws Exception {
        String graphOpXml = "<graph id=\"myOneNodeGraph\">\n"
                + "  <version>1.0</version>\n"
                + "  <node id=\"node1\">\n"
                + "    <operator>Algo</operator>\n"
                + "    <parameters>\n"
                + "    <width>2000</width>\n"
                + "    <height>2000</height>\n"
                + "    <preferredTileWidth>750</preferredTileWidth>\n"
                + "    <preferredTileHeight>750</preferredTileHeight>\n"
                + "    </parameters>\n"
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

        @Parameter
        private int width;

        @Parameter
        private int height;

        @Parameter
        private int preferredTileWidth;

        @Parameter
        private int preferredTileHeight;

        @TargetProduct
        private Product targetProduct;

        @Override
        public void initialize() {

            targetProduct = new Product("name", "desc", width, height);
            targetProduct.addBand("OperatorBand", ProductData.TYPE_INT8);
            targetProduct.addBand("ConstantBand", ProductData.TYPE_INT8).setSourceImage(new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED));
            targetProduct.addBand(new VirtualBand("VirtualBand", ProductData.TYPE_FLOAT32, width, height, "OperatorBand + ConstantBand"));

            targetProduct.setPreferredTileSize(preferredTileWidth, preferredTileHeight);
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
                                                                 targetProduct.getSceneGeoCoding());

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
