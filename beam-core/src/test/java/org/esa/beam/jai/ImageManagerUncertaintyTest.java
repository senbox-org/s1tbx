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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.ImageUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Arrays;

/**
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class ImageManagerUncertaintyTest {

    public static final int SIZE = 4;
    public static final int WIDTH = SIZE;
    public static final int HEIGHT = SIZE;
    public static final Color NO_DATA_COLOR = new Color(0, 0, 0, 0);
    public static final String[] CHANNELS = new String[]{"R", "G", "B", "A"};

    private Product product;
    private Band errorBand;
    private Band valueBand;

    @Before
    public void setUp() throws Exception {

        short[] valueData = {
                0, 0, 0, 0,  // Transparency (no-data color)
                1, 1, 1, 1,  // BLACK
                2, 2, 2, 2,  // BLUE
                3, 3, 3, 3,  // WHITE
        };

        short[] errorData = {
                0, 1, 2, 3,
                0, 1, 2, 3,
                0, 1, 2, 3,
                0, 1, 2, 3,
        };

        ImageInfo valueImageInfo = new ImageInfo(new ColorPaletteDef(new ColorPaletteDef.Point[]{
                new ColorPaletteDef.Point(1, Color.BLACK),
                new ColorPaletteDef.Point(2, Color.BLUE),
                new ColorPaletteDef.Point(3, Color.WHITE),
        }));
        valueImageInfo.setNoDataColor(NO_DATA_COLOR);

        ImageInfo errorImageInfo = new ImageInfo(new ColorPaletteDef(new ColorPaletteDef.Point[]{
                new ColorPaletteDef.Point(1, Color.WHITE),
                new ColorPaletteDef.Point(2, new Color(255, 127, 127)),
                new ColorPaletteDef.Point(3, Color.RED),
        }));
        errorImageInfo.setNoDataColor(Color.YELLOW);

        Product product = new Product("test", "test", WIDTH, HEIGHT);

        valueBand = product.addBand("value", ProductData.TYPE_UINT16);
        valueBand.setSourceImage(ImageUtils.createRenderedImage(WIDTH, HEIGHT, ProductData.createInstance(valueData)));
        valueBand.setNoDataValue(0);
        valueBand.setNoDataValueUsed(true);
        valueBand.getStx(true, ProgressMonitor.NULL);
        valueBand.setImageInfo(valueImageInfo);

        errorBand = product.addBand("error", ProductData.TYPE_UINT16);
        errorBand.setSourceImage(ImageUtils.createRenderedImage(WIDTH, HEIGHT, ProductData.createInstance(errorData)));
        errorBand.setNoDataValue(0);
        errorBand.setNoDataValueUsed(true);
        errorBand.getStx(true, ProgressMonitor.NULL);
        errorBand.setImageInfo(errorImageInfo);

        valueBand.setAncillaryBand("error", errorBand);

    }

    @Test
    public void testUncertaintyVisualisation_None() throws Exception {

        errorBand.getImageInfo().setUncertaintyVisualisationMode(ImageInfo.UncertaintyVisualisationMode.None);

        Color[][] expectedImage = new Color[][]{
                /*y=0*/
                {
                        new Color(0, 0, 0, 0),
                        new Color(0, 0, 0, 0),
                        new Color(0, 0, 0, 0),
                        new Color(0, 0, 0, 0)
                },
                /*y=1*/
                {
                        new Color(0, 0, 0, 255),
                        new Color(0, 0, 0, 255),
                        new Color(0, 0, 0, 255),
                        new Color(0, 0, 0, 255)
                },
                /*y=2*/
                {
                        new Color(1, 1, 255, 255),
                        new Color(1, 1, 255, 255),
                        new Color(1, 1, 255, 255),
                        new Color(1, 1, 255, 255)
                },
                /*y=3*/
                {
                        new Color(255, 255, 255, 255),
                        new Color(255, 255, 255, 255),
                        new Color(255, 255, 255, 255),
                        new Color(255, 255, 255, 255)
                },
        };

        testImageData(expectedImage);
    }

    @Test
    public void testUncertaintyVisualisation_Transparency_Blending() throws Exception {

        errorBand.getImageInfo().setUncertaintyVisualisationMode(ImageInfo.UncertaintyVisualisationMode.Transparency_Blending);

        Color[][] expectedImage = new Color[][]{
                /*y=0*/
                {
                        new Color(0, 0, 0, 0),
                        new Color(0, 0, 0, 0),
                        new Color(0, 0, 0, 0),
                        new Color(0, 0, 0, 0)
                },
                /*y=1*/
                {
                        new Color(0, 0, 0, 255),
                        new Color(0, 0, 0, 255),
                        new Color(0, 0, 0, 128),
                        new Color(0, 0, 0, 0)
                },
                /*y=2*/
                {
                        new Color(1, 1, 255, 255),
                        new Color(1, 1, 255, 255),
                        new Color(1, 1, 255, 128),
                        new Color(0, 0, 0, 0)
                },
                /*y=3*/
                {
                        new Color(255, 255, 255, 255),
                        new Color(255, 255, 255, 255),
                        new Color(255, 255, 255, 128),
                        new Color(0, 0, 0, 0)
                },
        };

        testImageData(expectedImage);
    }

    @Test
    public void testUncertaintyVisualisation_Monochromatic_Blending() throws Exception {

        errorBand.getImageInfo().setUncertaintyVisualisationMode(ImageInfo.UncertaintyVisualisationMode.Monochromatic_Blending);

        Color[][] expectedImage = new Color[][]{
                /*y=0*/
                {
                        new Color(255, 0, 0, 255),
                        new Color(255, 0, 0, 255),
                        new Color(255, 0, 0, 255),
                        new Color(255, 0, 0, 255)
                },
                /*y=1*/
                {
                        new Color(0, 0, 0, 255),
                        new Color(0, 0, 0, 255),
                        new Color(126, 0, 0, 255),
                        new Color(255, 0, 0, 255)
                },
                /*y=2*/
                {
                        new Color(1, 1, 255, 255),
                        new Color(1, 1, 255, 255),
                        new Color(127, 0, 128, 255),
                        new Color(255, 0, 0, 255)
                },
                /*y=3*/
                {
                        new Color(255, 255, 255, 255),
                        new Color(255, 255, 255, 255),
                        new Color(255, 128, 128, 255),
                        new Color(255, 0, 0, 255)
                },
        };

        testImageData(expectedImage);
    }

    @Test
    public void testUncertaintyVisualisation_Polychromatic_Blending() throws Exception {

        errorBand.getImageInfo().setUncertaintyVisualisationMode(ImageInfo.UncertaintyVisualisationMode.Polychromatic_Blending);

        Color[][] expectedImage = new Color[][]{
                /*y=0*/
                {
                        new Color(255, 255, 0, 255),
                        new Color(255, 255, 255, 255),
                        new Color(255, 127, 127, 255),
                        new Color(255, 0, 0, 255)
                },
                /*y=1*/
                {
                        new Color(0, 0, 0, 255),
                        new Color(0, 0, 0, 255),
                        new Color(126, 63, 63, 255),
                        new Color(255, 0, 0, 255)
                },
                /*y=2*/
                {
                        new Color(1, 1, 255, 255),
                        new Color(1, 1, 255, 255),
                        new Color(127, 63, 191, 255),
                        new Color(255, 0, 0, 255)
                },
                /*y=3*/
                {
                        new Color(255, 255, 255, 255),
                        new Color(255, 255, 255, 255),
                        new Color(255, 191, 191, 255),
                        new Color(255, 0, 0, 255)
                },
        };

        testImageData(expectedImage);
    }

    @Test
    public void testUncertaintyVisualisation_Polychromatic_Overlay() throws Exception {

        errorBand.getImageInfo().setUncertaintyVisualisationMode(ImageInfo.UncertaintyVisualisationMode.Polychromatic_Overlay);

        Color[][] expectedImage = new Color[][]{
                /*y=0*/
                {
                        new Color(255, 255, 0, 255),
                        new Color(255, 255, 255, 255),
                        new Color(255, 127, 127, 255),
                        new Color(255, 0, 0, 255),
                },
                /*y=1*/
                {
                        new Color(127, 127, 0, 255),
                        new Color(127, 127, 127, 255),
                        new Color(127, 63, 63, 255),
                        new Color(127, 0, 0, 255),
                },
                /*y=2*/
                {
                        new Color(128, 128, 127, 255),
                        new Color(128, 128, 255, 255),
                        new Color(128, 64, 190, 255),
                        new Color(128, 0, 127, 255),
                },
                /*y=3*/
                {
                        new Color(255, 255, 127, 255),
                        new Color(255, 255, 255, 255),
                        new Color(255, 190, 190, 255),
                        new Color(255, 127, 127, 255),
                },
        };

        testImageData(expectedImage);
    }

    private void testImageData(Color[][] expectedImage) {
        RenderedImage image = ImageManager.getInstance().createColoredBandImage(new RasterDataNode[]{valueBand}, null, 0);

        Raster data = image.getData();
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                Color expectedColor = expectedImage[y][x];
                int[] expectedPixel = new int[]{expectedColor.getRed(), expectedColor.getGreen(), expectedColor.getBlue(), expectedColor.getAlpha()};
                int[] actualPixel = data.getPixel(x, y, (int[]) null);
                for (int b = 0; b < 4; b++) {
                    if (expectedPixel[b] != actualPixel[b]) {
                        Assert.fail(String.format("x=%d, y=%d: in channel %s: expected %s, but got %s",
                                                  x, y, CHANNELS[b],
                                                  Arrays.toString(expectedPixel),
                                                  Arrays.toString(actualPixel)));
                    }
                }
            }
        }
    }
}
