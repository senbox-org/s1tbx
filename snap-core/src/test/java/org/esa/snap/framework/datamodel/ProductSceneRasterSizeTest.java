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

package org.esa.snap.framework.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.framework.dataio.ProductIO;
import org.esa.snap.framework.dataop.maptransf.Datum;
import org.junit.Ignore;
import org.junit.Test;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProductSceneRasterSizeTest {
    @Test
    public void testSizeChangeWithInitialSize() throws Exception {
        Product product = new Product("N", "T", 30, 20);
        assertEquals(new Dimension(30, 20), product.getSceneRasterSize());

        product.addBand(new Band("B0", ProductData.TYPE_FLOAT32, 5, 2));

        assertEquals(new Dimension(30, 20), product.getSceneRasterSize());

        product.addBand(new Band("B1", ProductData.TYPE_FLOAT32, 100, 200));
        assertEquals(new Dimension(100, 200), product.getSceneRasterSize());

        product.addBand(new Band("B2", ProductData.TYPE_FLOAT32, 110, 190));
        assertEquals(new Dimension(110, 200), product.getSceneRasterSize());

        product.addMask(Mask.BandMathsType.create("M1", "description1", 115, 210, "true", Color.BLACK, 0.0));
        assertEquals(new Dimension(115, 210), product.getSceneRasterSize());

        product.getMaskGroup().add(Mask.BandMathsType.create("M2", "description2", 120, 220, "true", Color.RED, 0.0));
        assertEquals(new Dimension(120, 220), product.getSceneRasterSize());

        product.addTiePointGrid(new TiePointGrid("TPG1", 500, 2, 0f, 0f, 1f, 1f, new float[1000]));
        assertEquals(new Dimension(500, 220), product.getSceneRasterSize());

        product.getTiePointGridGroup().add(new TiePointGrid("TPG2", 1000, 2, 0f, 0f, 1f, 1f, new float[2000]));
        assertEquals(new Dimension(1000, 220), product.getSceneRasterSize());
    }

    @Test
    public void testSizeChangeWithoutInitialSize() throws Exception {
        Product product = new Product("N", "T");
        assertNull(product.getSceneRasterSize());

        product.addBand(new Band("B1", ProductData.TYPE_FLOAT32, 100, 200));
        assertEquals(new Dimension(100, 200), product.getSceneRasterSize());

        product.addBand(new Band("B2", ProductData.TYPE_FLOAT32, 110, 190));
        assertEquals(new Dimension(110, 200), product.getSceneRasterSize());

        product.addMask(Mask.BandMathsType.create("M1", "description1", 115, 210, "true", Color.BLACK, 0.0));
        assertEquals(new Dimension(115, 210), product.getSceneRasterSize());

        product.getMaskGroup().add(Mask.BandMathsType.create("M2", "description2", 120, 220, "true", Color.RED, 0.0));
        assertEquals(new Dimension(120, 220), product.getSceneRasterSize());

        product.addTiePointGrid(new TiePointGrid("TPG1", 500, 2, 0f, 0f, 1f, 1f, new float[1000]));
        assertEquals(new Dimension(500, 220), product.getSceneRasterSize());

        product.getTiePointGridGroup().add(new TiePointGrid("TPG2", 1000, 2, 0f, 0f, 1f, 1f, new float[2000]));
        assertEquals(new Dimension(1000, 220), product.getSceneRasterSize());

    }

    @Test
    public void testDimapWithMultiSizeBands() throws Exception {

        Product product = new Product("N", "T");
        assertNull(product.getSceneRasterSize());

        Band b1 = new Band("B1", ProductData.TYPE_FLOAT32, 100, 200);
        b1.setSourceImage(ConstantDescriptor.create(100f, 200f, new Float[]{1f}, null));
        product.addBand(b1);

        Band b2 = new Band("B2", ProductData.TYPE_FLOAT32, 800, 190);
        b2.setSourceImage(ConstantDescriptor.create(800f, 190f, new Float[]{2f}, null));
        product.addBand(b2);

        product.addMask(Mask.BandMathsType.create("M1", "description1", 115, 210, "true", Color.BLACK, 0.0));
        product.getMaskGroup().add(Mask.BandMathsType.create("M2", "description2", 120, 220, "true", Color.RED, 0.0));

        product.addTiePointGrid(new TiePointGrid("TPG1", 500, 2, 0f, 0f, 1f, 1f, new float[1000]));
        product.getTiePointGridGroup().add(new TiePointGrid("TPG2", 1000, 2, 0f, 0f, 1f, 1f, new float[2000]));

        TiePointGrid latGrid = new TiePointGrid("latGrid", 2, 2, 0.5f, 0.5f, 500, 500, new float[]{54, 54, 56, 56});
        TiePointGrid lonGrid = new TiePointGrid("lonGrid", 2, 2, 0.5f, 0.5f, 500, 500, new float[]{9, 11, 9, 11});

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);

        product.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84));

        File file = new File("multisize_product.dim");
        try {
            ProductIO.writeProduct(product, file, "BEAM-DIMAP", false);
            Product product2 = ProductIO.readProduct(file);
            assertEquals(new Dimension(100, 200), product2.getBand("B1").getRasterSize());
            assertEquals(new Dimension(800, 190), product2.getBand("B2").getRasterSize());
            assertEquals(new Dimension(115, 210), product2.getMaskGroup().get("M1").getRasterSize());
            assertEquals(new Dimension(120, 220), product2.getMaskGroup().get("M2").getRasterSize());
            assertEquals(new Dimension(500, 2), product2.getTiePointGrid("TPG1").getRasterSize());
            assertEquals(new Dimension(1000, 2), product2.getTiePointGrid("TPG2").getRasterSize());
            assertEquals(new Dimension(1000, 220), product2.getSceneRasterSize());
        } finally {
            if (file.exists()) {
                Files.delete(file.toPath());
                File dataDir = new File(file.getAbsolutePath().replaceAll("dim", "data"));
                Files.walkFileTree(dataDir.toPath(), new PathTreeDeleter());
            }
        }
    }

    @Test
    public void testImageLayoutForMultiSizeProducts_WithPreferredTileSize() throws Exception {
        int size = 900;
        int tileSize = 130;

        Product product = new Product("N", "T", size, size);
        product.setPreferredTileSize(tileSize, tileSize);
        Band b1 = createVirtualBand("B1", 2.4, size);
        Band b2 = createVirtualBand("B2", 2.5, size / 3);
        Band b3 = createVirtualBand("B3", 2.6, size / 9);

        configureAndTestMultiSizeProductImages(tileSize, tileSize, tileSize, product, b1, b2, b3);
    }

    @Test
    public void testImageLayoutForMultiSizeProducts_WithoutPreferredTileSize() throws Exception {
        int size = 900;

        Product product = new Product("N", "T", size, size);
        Band b1 = createVirtualBand("B1", 2.4, size);
        Band b2 = createVirtualBand("B2", 2.5, size / 3);
        Band b3 = createVirtualBand("B3", 2.6, size / 9);

        configureAndTestMultiSizeProductImages(450, 300, 100, product, b1, b2, b3);
    }

    // todo - [multisize_products] remove @Ignore and let test succeed, see SNAP-145 (nf)
    @Ignore
    @Test
    public void testImageLayoutForMultiSizeProducts_WithoutPreferredTileSize_WithCustomSourceImages() throws Exception {
        int size = 900;
        int tileSize1 = 256;
        int tileSize2 = 128;
        int tileSize3 = 64;

        Product product = new Product("N", "T", size, size);
        Band b1 = createBand("B1", 2.4, size, tileSize1);
        Band b2 = createBand("B2", 2.5, size, tileSize2);
        Band b3 = createBand("B3", 2.6, size, tileSize3);

        configureAndTestMultiSizeProductImages(tileSize1, tileSize2, tileSize3, product, b1, b2, b3);
    }

    // todo - [multisize_products] remove @Ignore and let test succeed, see SNAP-145 (nf)
    @Ignore
    @Test
    public void testImageLayoutForMultiSizeProducts_WithPreferredTileSize_WithCustomSourceImages() throws Exception {
        int size = 900;
        int tileSize1 = 256;
        int tileSize2 = 128;
        int tileSize3 = 64;

        Product product = new Product("N", "T", size, size);
        product.setPreferredTileSize(130, 130);
        Band b1 = createBand("B1", 2.4, size, tileSize1);
        Band b2 = createBand("B2", 2.5, size, tileSize2);
        Band b3 = createBand("B3", 2.6, size, tileSize3);

        configureAndTestMultiSizeProductImages(tileSize1, tileSize2, tileSize3, product, b1, b2, b3);
    }

    private VirtualBand createVirtualBand(String name, double value, int size) {
        return new VirtualBand(name, ProductData.TYPE_FLOAT64, size, size, value + "");
    }

    private Band createBand(String name, double value, int size, int tileSize) {
        Band band = new Band(name, ProductData.TYPE_FLOAT64, size, size);
        ImageLayout imageLayout = new ImageLayout();
        imageLayout.setTileWidth(tileSize);
        imageLayout.setTileHeight(tileSize);
        band.setSourceImage(ConstantDescriptor.create((float) size, (float) size,
                                                      new Double[]{value}, new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout)));
        return band;
    }

    private void configureAndTestMultiSizeProductImages(int tileSize1,
                                                        int tileSize2,
                                                        int tileSize3,
                                                        Product product,
                                                        Band b1,
                                                        Band b2,
                                                        Band b3) {

        Mask m1 = Mask.BandMathsType.create("M1", null, b1.getSceneRasterWidth(), b1.getSceneRasterHeight(), "B1 > 0", Color.GREEN, 0.5);
        Mask m2 = Mask.BandMathsType.create("M2", null, b2.getSceneRasterWidth(), b2.getSceneRasterHeight(), "B2 > 0", Color.GREEN, 0.5);
        Mask m3 = Mask.BandMathsType.create("M3", null, b3.getSceneRasterWidth(), b3.getSceneRasterHeight(), "B3 > 0", Color.GREEN, 0.5);

        b1.setNoDataValueUsed(true);
        b2.setNoDataValueUsed(true);
        b3.setNoDataValueUsed(true);

        b1.setNoDataValue(0.0);
        b2.setNoDataValue(0.0);
        b3.setNoDataValue(0.0);

        product.addBand(b1);
        product.addBand(b2);
        product.addBand(b3);

        product.addMask(m1);
        product.addMask(m2);
        product.addMask(m3);

        testImageLayout(b1.getSceneRasterWidth(), tileSize1, b1, m1);
        testImageLayout(b2.getSceneRasterWidth(), tileSize2, b2, m2);
        testImageLayout(b3.getSceneRasterWidth(), tileSize3, b3, m3);

        assertEquals(2.4, b1.getStx(true, ProgressMonitor.NULL).getMean(), 1e-10);
        assertEquals(2.5, b2.getStx(true, ProgressMonitor.NULL).getMean(), 1e-10);
        assertEquals(2.6, b3.getStx(true, ProgressMonitor.NULL).getMean(), 1e-10);
    }

    private void testImageLayout(int size, int tileSize, Band band, Mask mask) {
        Dimension sizeDim = new Dimension(size, size);
        Dimension tileSizeDim = new Dimension(tileSize, tileSize);
        MultiLevelImage bsi = band.getSourceImage();
        MultiLevelImage vmi = band.getValidMaskImage();
        MultiLevelImage msi = mask.getSourceImage();
        assertEquals(sizeDim, new Dimension(bsi.getWidth(), bsi.getHeight()));
        assertEquals(sizeDim, new Dimension(vmi.getWidth(), vmi.getHeight()));
        assertEquals(sizeDim, new Dimension(msi.getWidth(), msi.getHeight()));
        assertEquals(tileSizeDim, new Dimension(bsi.getTileWidth(), bsi.getTileHeight()));
        assertEquals(tileSizeDim, new Dimension(vmi.getTileWidth(), vmi.getTileHeight()));
        assertEquals(tileSizeDim, new Dimension(msi.getTileWidth(), msi.getTileHeight()));
        assertEquals(bsi.getModel().getLevelCount(), vmi.getModel().getLevelCount());
        assertEquals(bsi.getModel().getLevelCount(), msi.getModel().getLevelCount());
    }

    private static class PathTreeDeleter extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }
    }
}

