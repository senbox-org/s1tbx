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

package org.esa.snap.core.image;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.junit.Test;

import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class ImageManagerTest {

    private static final double EPS_L = 1.0e-3;
    private static final double EPS_H = 1.0e-6;

    @Test
    public void testCreateColoredBandImage_Opaque() throws Exception {
        int[] pixel00 = new int[3];
        int[] pixel10 = new int[3];
        int[] pixel01 = new int[3];
        int[] pixel11 = new int[3];

        Product product = new Product("A", "B", 2, 2);
        Band band1 = product.addBand("b1", "X + Y");
        Band band2 = product.addBand("b2", "X + Y");
        Band band3 = product.addBand("b3", "X + Y");
        ImageInfo imageInfo = new ImageInfo(new ColorPaletteDef(new ColorPaletteDef.Point[]{
                new ColorPaletteDef.Point(1.0, Color.YELLOW),
                new ColorPaletteDef.Point(2.0, Color.RED),
                new ColorPaletteDef.Point(3.0, Color.BLUE),
        }));

        RenderedImage image;

        band1.getStx(true, ProgressMonitor.NULL);
        band1.getImageInfo(ProgressMonitor.NULL);
        image = ImageManager.getInstance().createColoredBandImage(new RasterDataNode[]{band1}, null, 0);
        image.getData().getPixel(0, 0, pixel00);
        image.getData().getPixel(1, 0, pixel10);
        image.getData().getPixel(0, 1, pixel01);
        image.getData().getPixel(1, 1, pixel11);
        assertArrayEquals(new int[]{0, 0, 0}, pixel00);
        assertArrayEquals(new int[]{0, 0, 0}, pixel10);
        assertArrayEquals(new int[]{0, 0, 0}, pixel01);
        assertArrayEquals(new int[]{255, 255, 255}, pixel11);

        band2.setImageInfo(imageInfo);
        image = ImageManager.getInstance().createColoredBandImage(new RasterDataNode[]{band2}, null, 0);
        image.getData().getPixel(0, 0, pixel00);
        image.getData().getPixel(1, 0, pixel10);
        image.getData().getPixel(0, 1, pixel01);
        image.getData().getPixel(1, 1, pixel11);
        assertArrayEquals(new int[]{255, 255, 0}, pixel00);
        assertArrayEquals(new int[]{255, 1, 0}, pixel10);
        assertArrayEquals(new int[]{255, 1, 0}, pixel01);
        assertArrayEquals(new int[]{0, 0, 255}, pixel11);

        image = ImageManager.getInstance().createColoredBandImage(new RasterDataNode[]{band3}, imageInfo, 0);
        image.getData().getPixel(0, 0, pixel00);
        image.getData().getPixel(1, 0, pixel10);
        image.getData().getPixel(0, 1, pixel01);
        image.getData().getPixel(1, 1, pixel11);
        assertArrayEquals(new int[]{255, 255, 0}, pixel00);
        assertArrayEquals(new int[]{255, 1, 0}, pixel10);
        assertArrayEquals(new int[]{255, 1, 0}, pixel01);
        assertArrayEquals(new int[]{0, 0, 255}, pixel11);
    }

    @Test
    public void testCreateColoredBandImage_SemiTransparent() throws Exception {
        int[] pixel00 = new int[4];
        int[] pixel10 = new int[4];
        int[] pixel01 = new int[4];
        int[] pixel11 = new int[4];

        Product product = new Product("A", "B", 2, 2);
        Band band1 = product.addBand("b1", "X + Y");
        Band band2 = product.addBand("b2", "X + Y");
        Band band3 = product.addBand("b3", "X + Y");
        ImageInfo imageInfo = new ImageInfo(new ColorPaletteDef(new ColorPaletteDef.Point[]{
                new ColorPaletteDef.Point(1.0, Color.YELLOW),
                new ColorPaletteDef.Point(2.0, new Color(255, 0, 0, 127)),
                new ColorPaletteDef.Point(3.0, Color.BLUE),
        }));

        RenderedImage image;

        band1.getStx(true, ProgressMonitor.NULL);
        band1.getImageInfo(ProgressMonitor.NULL);
        image = ImageManager.getInstance().createColoredBandImage(new RasterDataNode[]{band1}, null, 0);
        image.getData().getPixel(0, 0, pixel00);
        image.getData().getPixel(1, 0, pixel10);
        image.getData().getPixel(0, 1, pixel01);
        image.getData().getPixel(1, 1, pixel11);
        assertArrayEquals(new int[]{0, 0, 0, 0}, pixel00);
        assertArrayEquals(new int[]{0, 0, 0, 0}, pixel10);
        assertArrayEquals(new int[]{0, 0, 0, 0}, pixel01);
        assertArrayEquals(new int[]{255, 255, 255, 0}, pixel11);

        band2.setImageInfo(imageInfo);
        image = ImageManager.getInstance().createColoredBandImage(new RasterDataNode[]{band2}, null, 0);
        image.getData().getPixel(0, 0, pixel00);
        image.getData().getPixel(1, 0, pixel10);
        image.getData().getPixel(0, 1, pixel01);
        image.getData().getPixel(1, 1, pixel11);
        assertArrayEquals(new int[]{255, 255, 0, 255}, pixel00);
        assertArrayEquals(new int[]{255, 1, 0, 128}, pixel10);
        assertArrayEquals(new int[]{255, 1, 0, 128}, pixel01);
        assertArrayEquals(new int[]{0, 0, 255, 255}, pixel11);

        image = ImageManager.getInstance().createColoredBandImage(new RasterDataNode[]{band3}, imageInfo, 0);
        image.getData().getPixel(0, 0, pixel00);
        image.getData().getPixel(1, 0, pixel10);
        image.getData().getPixel(0, 1, pixel01);
        image.getData().getPixel(1, 1, pixel11);
        assertArrayEquals(new int[]{255, 255, 0, 255}, pixel00);
        assertArrayEquals(new int[]{255, 1, 0, 128}, pixel10);
        assertArrayEquals(new int[]{255, 1, 0, 128}, pixel01);
        assertArrayEquals(new int[]{0, 0, 255, 255}, pixel11);
    }

    @Test
    public void testSentinel2L1CTileResolutions() throws Exception {
        DefaultMultiLevelModel model = new DefaultMultiLevelModel(6, new AffineTransform(), new Rectangle2D.Double(0, 0, 1, 1));

        // S-2 MSI 10m and 20m Tile
        testGetLevelImageBounds(4096, 4096, 0, model);
        testGetLevelImageBounds(2048, 4096, 1, model);
        testGetLevelImageBounds(1024, 4096, 2, model);
        testGetLevelImageBounds(512, 4096, 3, model);
        testGetLevelImageBounds(256, 4096, 4, model);
        testGetLevelImageBounds(128, 4096, 5, model);

        // S-2 MSI 60m Tile
        testGetLevelImageBounds(1826, 1826, 0, model);
        testGetLevelImageBounds(913, 1826, 1, model);
        testGetLevelImageBounds(457, 1826, 2, model);
        testGetLevelImageBounds(229, 1826, 3, model);
        testGetLevelImageBounds(115, 1826, 4, model);
        testGetLevelImageBounds(58, 1826, 5, model);
    }

    private void testGetLevelImageBounds(int expectedSize, int size, int level, DefaultMultiLevelModel model) {
        Rectangle expected = new Rectangle(0, 0, expectedSize, expectedSize);
        ResolutionLevel resolutionLevel = new ResolutionLevel(level, model.getScale(level));
        ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(DataBuffer.TYPE_USHORT, size, size, null, resolutionLevel);
        assertEquals("at resolution level " + level + ":",
                     expected,
                     new Rectangle(imageLayout.getMinX(null),
                                   imageLayout.getMinY(null),
                                   imageLayout.getWidth(null),
                                   imageLayout.getHeight(null))
        );
    }

    @Test
    public void testBandWithNoScaling() {
        Band band = createBand(1.0, 0.0, false);
        checkTargetImageSampleValues(band, EPS_H);
    }

    @Test
    public void testBandWithLinearScaling() {
        Band band = createBand(0.1, 0.5, false);
        checkTargetImageSampleValues(band, EPS_H);
    }

    @Test
    public void testBandWithLog10Scaling() {
        Band band = createBand(1.0, 0.0, true);
        checkTargetImageSampleValues(band, EPS_L);
    }

    @Test
    public void testBandWithLinearAndLog10Scaling() {
        Band band = createBand(0.1, 0.5, true);
        checkTargetImageSampleValues(band, EPS_H);
    }

    /**
     * size of source image is calculated in
     * {@code com.bc.ceres.glevel.support.DefaultMultiLevelSource#createImage(int)}
     * <p>
     * size of mask image is calculated in
     * {@code org.esa.snap.jai.ImageManager#createSingleBandedImageLayout(RasterDataNode)}
     * <p>
     * they shall not produce different results.
     */
    @Test
    public void testImageAndMaskSize() {
        Product p = new Product("n", "t", 8501, 7651);
        Band b = p.addBand("b", ProductData.TYPE_FLOAT32);
        b.setNoDataValue(13);
        b.setNoDataValueUsed(true);
        b.setSourceImage(ConstantDescriptor.create((float) p.getSceneRasterWidth(),
                                                   (float) p.getSceneRasterHeight(),
                                                   new Float[]{42f}, null));
        ImageManager imageManager = ImageManager.getInstance();
        int levelCount = b.getSourceImage().getModel().getLevelCount();

        PlanarImage sourceImage = imageManager.getSourceImage(b, levelCount - 1);
        PlanarImage maskImage = imageManager.getValidMaskImage(b, levelCount - 1);

        assertEquals(sourceImage.getWidth(), maskImage.getWidth());
        assertEquals(sourceImage.getHeight(), maskImage.getHeight());
    }

    private Band createBand(double factor, double offset, boolean log10Scaled) {
        Product p = new Product("n", "t", 2, 2);
        Band band = p.addBand("b", ProductData.TYPE_INT8);
        band.setScalingFactor(factor);
        band.setScalingOffset(offset);
        band.setLog10Scaled(log10Scaled);
        band.setSourceImage(createSourceImage());
        return band;
    }

    private static void checkTargetImageSampleValues(Band band, final double eps) {
        PlanarImage image = createTargetImage(band);
        assertEquals(band.scale(0), getSample(image, 0, 0), eps);
        assertEquals(band.scale(1), getSample(image, 1, 0), eps);
        assertEquals(band.scale(2), getSample(image, 0, 1), eps);
        assertEquals(band.scale(3), getSample(image, 1, 1), eps);
    }

    private static double getSample(PlanarImage image, int x, int y) {
        return image.getData().getSampleDouble(x, y, 0);
    }

    private static PlanarImage createTargetImage(RasterDataNode rdn) {
        return rdn.getGeophysicalImage();
    }

    private static PlanarImage createSourceImage() {
        final BufferedImage bi = new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_GRAY);
        bi.getRaster().setSample(0, 0, 0, 0);
        bi.getRaster().setSample(1, 0, 0, 1);
        bi.getRaster().setSample(0, 1, 0, 2);
        bi.getRaster().setSample(1, 1, 0, 3);
        return PlanarImage.wrapRenderedImage(bi);
    }

    @Test
    public void testCreateLinearColorPalette() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new ColorPaletteDef.Point[]{
                    new ColorPaletteDef.Point(100, Color.WHITE),
                    new ColorPaletteDef.Point(200, Color.BLUE),
                    new ColorPaletteDef.Point(300, Color.RED),
                    new ColorPaletteDef.Point(400, Color.GREEN),
        }, 7);
        final ImageInfo imageInfo = new ImageInfo(cpd);
        imageInfo.setLogScaled(false);

        final Color[] palette = ImageManager.createColorPalette(imageInfo);

        assertNotNull(palette);
        assertEquals(7, palette.length);
        assertEquals(new Color(255, 255, 255), palette[0]);
        assertEquals(new Color(128, 128, 255), palette[1]);
        assertEquals(new Color(0, 0, 255), palette[2]);
        assertEquals(new Color(128, 0, 128), palette[3]);
        assertEquals(new Color(255, 0, 0), palette[4]);
        assertEquals(new Color(128, 128, 0), palette[5]);
        assertEquals(new Color(0, 255, 0), palette[6]);
    }

    @Test
    public void testCreateLog10ColorPalette() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new ColorPaletteDef.Point[]{
                    new ColorPaletteDef.Point(1, Color.WHITE),
                    new ColorPaletteDef.Point(10, Color.BLUE),
                    new ColorPaletteDef.Point(100, Color.RED),
                    new ColorPaletteDef.Point(1000, Color.GREEN),
        }, 7);
        final ImageInfo imageInfo = new ImageInfo(cpd);
        imageInfo.setLogScaled(true);

        final Color[] palette = ImageManager.createColorPalette(imageInfo);
        assertNotNull(palette);
        assertEquals(7, palette.length);
        assertEquals(new Color(255, 255, 255), palette[0]);
        assertEquals(new Color(128, 128, 255), palette[1]);
        assertEquals(new Color(0, 0, 255), palette[2]);
        assertEquals(new Color(128, 0, 128), palette[3]);
        assertEquals(new Color(255, 0, 0), palette[4]);
        assertEquals(new Color(128, 128, 0), palette[5]);
        assertEquals(new Color(0, 255, 0), palette[6]);
    }

    @Test
    public void testCreateLinearColorPalette_Discrete() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new ColorPaletteDef.Point[]{
                    new ColorPaletteDef.Point(100, Color.WHITE),
                    new ColorPaletteDef.Point(200, Color.BLUE),
                    new ColorPaletteDef.Point(300, Color.RED),
                    new ColorPaletteDef.Point(400, Color.GREEN),
        }, 7);
        cpd.setDiscrete(true);
        final ImageInfo imageInfo = new ImageInfo(cpd);
        imageInfo.setLogScaled(false);

        final Color[] palette = ImageManager.createColorPalette(imageInfo);

        assertNotNull(palette);
        assertEquals(7, palette.length);
        assertEquals(new Color(255, 255, 255), palette[0]);
        assertEquals(new Color(255, 255, 255), palette[1]);
        assertEquals(new Color(0, 0, 255), palette[2]);
        assertEquals(new Color(0, 0, 255), palette[3]);
        assertEquals(new Color(255, 0, 0), palette[4]);
        assertEquals(new Color(255, 0, 0), palette[4]);
        assertEquals(new Color(0, 255, 0), palette[6]);
    }

    @Test
    public void testCreateLog10ColorPalette_Discrete() {
        final ColorPaletteDef cpd = new ColorPaletteDef(new ColorPaletteDef.Point[]{
                    new ColorPaletteDef.Point(1, Color.WHITE),
                    new ColorPaletteDef.Point(10, Color.BLUE),
                    new ColorPaletteDef.Point(100, Color.RED),
                    new ColorPaletteDef.Point(1000, Color.GREEN),
        }, 7);
        cpd.setDiscrete(true);
        final ImageInfo imageInfo = new ImageInfo(cpd);
        imageInfo.setLogScaled(true);

        final Color[] palette = ImageManager.createColorPalette(imageInfo);
        assertNotNull(palette);
        assertEquals(7, palette.length);
        assertEquals(new Color(255, 255, 255), palette[0]);
        assertEquals(new Color(255, 255, 255), palette[1]);
        assertEquals(new Color(0, 0, 255), palette[2]);
        assertEquals(new Color(0, 0, 255), palette[3]);
        assertEquals(new Color(255, 0, 0), palette[4]);
        assertEquals(new Color(255, 0, 0), palette[5]);
        assertEquals(new Color(0, 255, 0), palette[6]);
    }
}
