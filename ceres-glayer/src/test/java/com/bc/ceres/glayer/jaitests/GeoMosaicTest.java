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

package com.bc.ceres.glayer.jaitests;

import com.bc.ceres.glayer.tools.Tools;
import org.junit.Ignore;

import javax.media.jai.RenderedOp;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.IOException;

@Ignore
public class GeoMosaicTest {

    private static class ImageFeature {

        RenderedImage image;
        AffineTransform transform;

        ImageFeature(RenderedImage image, AffineTransform transform) {
            this.image = image;
            this.transform = transform;
        }
    }

    public static void main(String[] args) throws IOException {
        Tools.configureJAI();


        String dir = "C:\\Data\\jai-sandbox\\geomosaic\\";

        String[] filePaths = new String[]{
//                dir + "l5_20060718_georef_rgb432",
                dir + "l7_20020715_197-23_georef_rgb321",
                dir + "l7_20020715_197-22_georef_rgb321",
        };

        ImageFeature[] imageFeatures = new ImageFeature[filePaths.length];
        for (int i = 0; i < imageFeatures.length; i++) {
            final String filePath = filePaths[i];
            imageFeatures[i] = new ImageFeature(Tools.loadImage(filePath + ".png"),
                                                Tools.loadWorldFile(filePath + ".pgw"));
        }

        final AffineTransform t0 = imageFeatures[0].transform;
        final double scaleX = t0.getScaleX();
        final double scaleY = t0.getScaleY();
        t0.preConcatenate(AffineTransform.getScaleInstance(1.0 / scaleX, 1.0 / scaleY));
        final double translateX = t0.getTranslateX();
        final double translateY = t0.getTranslateY();
        t0.translate(-translateX, -translateY);
        for (int i = 1; i < imageFeatures.length; i++) {
            ImageFeature imageFeature = imageFeatures[i];
            final AffineTransform transform = imageFeature.transform;
            transform.preConcatenate(AffineTransform.getScaleInstance(1.0 / scaleX, 1.0 / scaleY));
            transform.translate(-translateX, -translateY);
        }

        final RenderedImage[] sourceImages = new RenderedImage[imageFeatures.length];
        for (int i = 0; i < sourceImages.length; i++) {
            final AffineTransform transform = imageFeatures[i].transform;
            sourceImages[i] = Tools.transformImage(imageFeatures[i].image, transform);
        }
        RenderedOp image = Tools.createMosaic(sourceImages);
        Tools.displayImage("GeoMosaicTest", image, new AffineTransform(), 8);

        //Tools.storeTiffPyramid(image, "geomosaic", 8);
    }
}