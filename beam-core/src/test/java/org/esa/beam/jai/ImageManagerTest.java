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

package org.esa.beam.jai;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.image.BufferedImage;

/**
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class ImageManagerTest extends TestCase {

    private static final double EPS_L = 1.0e-3;
    private static final double EPS_H = 1.0e-6;

    public void testBandWithNoScaling() {
        Band band = createBand(1.0, 0.0, false);
        checkTargetImageSampleValues(band, EPS_H);
    }

    public void testBandWithLinearScaling() {
        Band band = createBand(0.1, 0.5, false);
        checkTargetImageSampleValues(band, EPS_H);
    }

    public void testBandWithLog10Scaling() {
        Band band = createBand(1.0, 0.0, true);
        checkTargetImageSampleValues(band, EPS_L);
    }

    public void testBandWithLinearAndLog10Scaling() {
        Band band = createBand(0.1, 0.5, true);
        checkTargetImageSampleValues(band, EPS_H);
    }

    /**
     * size of source image is calculated in
     * {@code com.bc.ceres.glevel.support.DefaultMultiLevelSource#createImage(int)}
     * <p/>
     * size of mask image is calculated in
     * {@code org.esa.beam.jai.ImageManager#createSingleBandedImageLayout(org.esa.beam.framework.datamodel.RasterDataNode)}
     * <p/>
     * they shall not produce different results.
     */
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
        Band band = new Band("b", ProductData.TYPE_INT8, 2, 2);
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
}
