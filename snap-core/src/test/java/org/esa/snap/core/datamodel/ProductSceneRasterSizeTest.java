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

package org.esa.snap.core.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.dataio.ProductIO;
import org.junit.Ignore;
import org.junit.Test;

import javax.media.jai.ImageFunction;
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

import static java.lang.Math.PI;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProductSceneRasterSizeTest {


    /**
     * See https://senbox.atlassian.net/browse/SNAP-211
     * Cannot create mask with size different than the scene raster size
     */
    @Test
    public void testUseMasksWithMultiSizeProduct() throws Exception {

        final int S1 = 120;
        final int S2 = 60;
        final int S3 = 20;

        final long PC1 = S1 * S1;
        final long PC2 = S2 * S2;
        final long PC3 = S3 * S3;

        Product p = new Product("N", "T", S1, S1);

        Band b1 = new Band("B1", ProductData.TYPE_FLOAT32, S1, S1);
        Band b2 = new Band("B2", ProductData.TYPE_FLOAT32, S2, S2);
        Band b3 = new Band("B3", ProductData.TYPE_FLOAT32, S3, S3);

        p.addBand(b1);
        p.addBand(b2);
        p.addBand(b3);

        b1.setNoDataValueUsed(true);
        b2.setNoDataValueUsed(true);
        b3.setNoDataValueUsed(true);

        b1.setNoDataValue(0);
        b2.setNoDataValue(0);
        b3.setNoDataValue(0);

        String expr = "sin(4 * PI * sqrt(X*X/%1$s/%1$s + Y*Y/%1$s/%1$s))";
        b1.setSourceImage(VirtualBand.createSourceImage(b1, String.format(expr, S1)));
        b2.setSourceImage(VirtualBand.createSourceImage(b2, String.format(expr, S2)));
        b3.setSourceImage(VirtualBand.createSourceImage(b3, String.format(expr, S3)));

        Mask m1 = p.addMask("M1", "B1 > 0.0", "", Color.GREEN, 0.5);
        Mask m2 = p.addMask("M2", "B2 > 0.0", "", Color.GREEN, 0.5);
        Mask m3 = p.addMask("M3", "B3 > 0.0", "", Color.GREEN, 0.5);

        b1.setValidPixelExpression("M1");
        b2.setValidPixelExpression("M2");
        b3.setValidPixelExpression("M3");

        Stx stx1 = b1.getStx(true, ProgressMonitor.NULL);
        Stx stx2 = b2.getStx(true, ProgressMonitor.NULL);
        Stx stx3 = b3.getStx(true, ProgressMonitor.NULL);

        assertEquals(new Dimension(S1, S1), m1.getRasterSize());
        assertEquals(new Dimension(S2, S2), m2.getRasterSize());
        assertEquals(new Dimension(S3, S3), m3.getRasterSize());

        assertTrue(stx1.getSampleCount() > PC1 / 3 && stx1.getSampleCount() < PC1 / 2);
        assertTrue(stx2.getSampleCount() > PC2 / 3 && stx2.getSampleCount() < PC2 / 2);
        assertTrue(stx3.getSampleCount() > PC3 / 3 && stx3.getSampleCount() < PC3 / 2);
    }

    @Test
    public void testThatSizeRemainsConstantOnceInitialized() throws Exception {
        Product product = new Product("N", "T", 30, 20);
        assertEquals(new Dimension(30, 20), product.getSceneRasterSize());

        product.addBand(new Band("B0", ProductData.TYPE_FLOAT32, 5, 2));
        assertEquals(new Dimension(30, 20), product.getSceneRasterSize());

        product.addBand(new Band("B1", ProductData.TYPE_FLOAT32, 100, 200));
        assertEquals(new Dimension(30, 20), product.getSceneRasterSize());

        product.addBand(new Band("B2", ProductData.TYPE_FLOAT32, 110, 190));
        assertEquals(new Dimension(30, 20), product.getSceneRasterSize());

        product.addMask(Mask.BandMathsType.create("M1", "description1", 115, 210, "true", Color.BLACK, 0.0));
        assertEquals(new Dimension(30, 20), product.getSceneRasterSize());

        product.getMaskGroup().add(Mask.BandMathsType.create("M2", "description2", 120, 220, "true", Color.RED, 0.0));
        assertEquals(new Dimension(30, 20), product.getSceneRasterSize());

        product.addTiePointGrid(new TiePointGrid("TPG1", 500, 2, 0f, 0f, 1f, 1f, new float[1000]));
        assertEquals(new Dimension(30, 20), product.getSceneRasterSize());

        product.getTiePointGridGroup().add(new TiePointGrid("TPG2", 1000, 2, 0f, 0f, 1f, 1f, new float[2000]));
        assertEquals(new Dimension(30, 20), product.getSceneRasterSize());
    }

    @Test(expected = IllegalStateException.class)
    public void testThatItMustBePossibleToDeriveSize() throws Exception {
        Product product = new Product("N", "T");
        product.getSceneRasterSize();
    }

    @Test
    public void testThatSizeIsDerivedFromLargestBand() throws Exception {
        Product product = new Product("N", "T");
        product.addBand(new Band("B1", ProductData.TYPE_FLOAT32, 100, 200));
        product.addBand(new Band("B2", ProductData.TYPE_FLOAT32, 110, 190));
        product.addMask(Mask.BandMathsType.create("M1", "description1", 115, 210, "true", Color.BLACK, 0.0));
        product.addTiePointGrid(new TiePointGrid("TPG1", 500, 2, 0f, 0f, 1f, 1f, new float[1000]));
        assertEquals(new Dimension(110, 190), product.getSceneRasterSize());
    }

    @Test
    public void testDimapWithMultiSizeBands() throws Exception {

        Product product = new Product("N", "T");

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

        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));

        File file = new File("multisize_product.dim");
        try {
            ProductIO.writeProduct(product, file, "BEAM-DIMAP", false);
            Product product2 = ProductIO.readProduct(file);
            assertEquals(new Dimension(800, 190), product2.getSceneRasterSize());
            assertEquals(new Dimension(100, 200), product2.getBand("B1").getRasterSize());
            assertEquals(new Dimension(800, 190), product2.getBand("B2").getRasterSize());
            assertEquals(new Dimension(115, 210), product2.getMaskGroup().get("M1").getRasterSize());
            assertEquals(new Dimension(120, 220), product2.getMaskGroup().get("M2").getRasterSize());
            assertEquals(new Dimension(800, 190), product2.getTiePointGrid("TPG1").getRasterSize());
            assertEquals(new Dimension(800, 190), product2.getTiePointGrid("TPG2").getRasterSize());
            assertEquals(500, product2.getTiePointGrid("TPG1").getGridWidth());
            assertEquals(2, product2.getTiePointGrid("TPG1").getGridHeight());
            assertEquals(1000, product2.getTiePointGrid("TPG2").getGridWidth());
            assertEquals(2, product2.getTiePointGrid("TPG2").getGridHeight());
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

        Mask m1 = Mask.BandMathsType.create("M1", null, b1.getRasterWidth(), b1.getRasterHeight(), "B1 > 0", Color.GREEN, 0.5);
        Mask m2 = Mask.BandMathsType.create("M2", null, b2.getRasterWidth(), b2.getRasterHeight(), "B2 > 0", Color.GREEN, 0.5);
        Mask m3 = Mask.BandMathsType.create("M3", null, b3.getRasterWidth(), b3.getRasterHeight(), "B3 > 0", Color.GREEN, 0.5);

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

        testImageLayout(b1.getRasterWidth(), tileSize1, b1, m1);
        testImageLayout(b2.getRasterWidth(), tileSize2, b2, m2);
        testImageLayout(b3.getRasterWidth(), tileSize3, b3, m3);

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

    private static class MyImageFunction implements ImageFunction {

        @Override
        public boolean isComplex() {
            return false;
        }

        @Override
        public int getNumElements() {
            return 1;
        }

        @Override
        public void getElements(float startX, float startY, float deltaX, float deltaY, int countX, int countY, int element, float[] real, float[] imag) {
            System.out.printf("getElements(F): startX = %1.4f, startY = %1.4f, countX=%5d, countY=%5d, element=%s, real.length = %10d, real = %s%n",
                              startX, startY, countX, countY, element, real.length, real);
            int k = 0;
            for (int j = 0; j < countY; j++) {
                for (int i = 0; i < countX; i++) {
                    float x = startX + deltaX * i;
                    float y = startY + deltaY * j;
                    real[k++] = (float) sin(4 * PI * sqrt(x * x + y * y));
                }
            }
        }

        @Override
        public void getElements(double startX, double startY, double deltaX, double deltaY, int countX, int countY, int element, double[] real, double[] imag) {
            System.out.printf("getElements(D): startX = %1.4f, startY = %1.4f, countX=%5d, countY=%5d, element=%s, real.length = %10d, real = %s%n",
                              startX, startY, countX, countY, element, real.length, real);
            int k = 0;
            for (int j = 0; j < countY; j++) {
                for (int i = 0; i < countX; i++) {
                    double x = startX + deltaX * i;
                    double y = startY + deltaY * j;
                    real[k++] = sin(4 * PI * sqrt(x * x + y * y));
                }
            }
        }
    }
}

