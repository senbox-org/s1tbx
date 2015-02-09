package org.esa.beam.framework.datamodel;/*
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

import org.esa.beam.util.math.DistanceMeasure;
import org.esa.beam.util.math.SphericalDistance;

import javax.media.jai.PlanarImage;
import java.awt.geom.Dimension2D;

class SimplePixelDimensionEstimator implements PixelDimensionEstimator {

    @Override
    public Dimension2D getPixelDimension(PlanarImage lonImage, PlanarImage latImage, PlanarImage maskImage) {
        final int w = latImage.getWidth();
        final int h = latImage.getHeight();

        double pixelSizeX = Double.NaN;
        for (int i = 5; i > 2; i--) {
            if (Double.isNaN(pixelSizeX)) {
                final int x0 = w / i;
                final int y0 = h / i;
                if (maskImage == null || getSampleBoolean(maskImage, x0, y0)) {
                    final double lat0 = getSampleDouble(latImage, x0, y0, -90.0, 90.0);
                    final double lon0 = getSampleDouble(lonImage, x0, y0, -180.0, 180.0);
                    final DistanceMeasure calculator = new SphericalDistance(lon0, lat0);
                    final int x1 = ((i - 1) * w) / i;

                    if (maskImage == null || getSampleBoolean(maskImage, x1, y0)) {
                        final double latX = getSampleDouble(latImage, x1, y0, -90.0, 90.0);
                        final double lonX = getSampleDouble(lonImage, x1, y0, -180.0, 180.0);
                        pixelSizeX = Math.toDegrees(calculator.distance(lonX, latX)) / ((w * (i - 2)) / i);
                    }
                }
            }
        }

        return new PixelDimension(pixelSizeX);
    }

    private static boolean getSampleBoolean(PlanarImage image, int x, int y) {
        final int tileX = image.XToTileX(x);
        final int tileY = image.YToTileY(y);

        return image.getTile(tileX, tileY).getSample(x, y, 0) != 0;
    }

    private static double getSampleDouble(PlanarImage image, int x, int y, double minValue, double maxValue) {
        final int tileX = image.XToTileX(x);
        final int tileY = image.YToTileY(y);

        final double value = image.getTile(tileX, tileY).getSampleDouble(x, y, 0);
        if (value >= minValue && value <= maxValue) {
            return value;
        }
        return Double.NaN;
    }

    private static final class PixelDimension extends Dimension2D {

        public double pixelSizeX;
        public double pixelSizeY;

        private PixelDimension(double pixelSizeX) {
            //noinspection SuspiciousNameCombination
            this(pixelSizeX, pixelSizeX);
        }

        private PixelDimension(double pixelSizeX, double pixelSizeY) {
            this.pixelSizeX = pixelSizeX;
            this.pixelSizeY = pixelSizeY;
        }

        @Override
        public double getWidth() {
            return pixelSizeX;
        }

        @Override
        public double getHeight() {
            return pixelSizeY;
        }

        @Override
        public void setSize(double width, double height) {
            pixelSizeX = width;
            pixelSizeY = height;
        }
    }
}
