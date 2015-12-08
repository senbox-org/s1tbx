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
import org.esa.snap.core.util.ShapeRasterizer;
import org.esa.snap.core.util.math.MathUtils;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Arrays;


/**
 * A container for data which fully describes a transect profile. Use {@link TransectProfileDataBuilder} to create
 * instances.
 *
 * @author Thomas Storm
 * @author Norman Fomferra
 */
public class TransectProfileData {

    public static final GeoPos NO_GEO_POS = new GeoPos(Float.NaN, Float.NaN);
    private final Point2D[] shapeVertices;
    private final int[] shapeVertexIndexes;
    private final Point2D[] pixelPositions; // todo - ts02Apr2012 - better use integer point class
    private final GeoPos[] geoPositions;
    private final float[] sampleValues;
    private final float[] sampleSigmas;
    private float sampleMin;
    private float sampleMax;

    Config config;

    /**
     * Since 4.5
     *
     * @deprecated since 4.10, use {@link TransectProfileDataBuilder} instead
     */
    public static TransectProfileData create(RasterDataNode raster, Shape path) throws IOException {
        Config config = new Config();
        config.raster = raster;
        config.path = path;
        config.boxSize = 1;
        config.useRoiMask = false;
        config.roiMask = null;
        config.connectVertices = true;
        return new TransectProfileData(config);
    }

    TransectProfileData(Config config) throws IOException {
        this.config = config;
        final ShapeRasterizer rasterizer = new ShapeRasterizer();
        final GeoCoding geoCoding = config.raster.getGeoCoding();
        final AffineTransform i2m = Product.findImageToModelTransform(geoCoding);
        if (!i2m.isIdentity()) {
            try {
                rasterizer.setTransform(i2m.createInverse());
            } catch (NoninvertibleTransformException e) {
                // cannot happen
            }
        }
        shapeVertices = rasterizer.getVertices(config.path);
        shapeVertexIndexes = new int[shapeVertices.length];
        pixelPositions = rasterizer.rasterize(shapeVertices, shapeVertexIndexes);
        sampleValues = new float[pixelPositions.length];
        Arrays.fill(sampleValues, Float.NaN);
        sampleSigmas = new float[pixelPositions.length];
        Arrays.fill(sampleSigmas, Float.NaN);

        sampleMin = Float.MAX_VALUE;
        sampleMax = -Float.MAX_VALUE;

        if (geoCoding != null) {
            geoPositions = new GeoPos[pixelPositions.length];
            Arrays.fill(geoPositions, NO_GEO_POS);
        } else {
            geoPositions = new GeoPos[0];
        }

        final Rectangle sceneRect = new Rectangle(config.raster.getRasterWidth(),
                                                  config.raster.getRasterHeight());
        PixelPos pixelPos = new PixelPos();
        int k = 0;
        for (int i = 0; i < pixelPositions.length; i++) {
            final int xC = MathUtils.floorInt(pixelPositions[i].getX() + 0.5f);
            final int yC = MathUtils.floorInt(pixelPositions[i].getY() + 0.5f);
            if (i == shapeVertexIndexes[k]) {
                k++;
            } else if (!config.connectVertices) {
                continue;
            }

            if (!sceneRect.contains(xC, yC)) {
                continue;
            }
            final Rectangle box = sceneRect.intersection(new Rectangle(xC - config.boxSize / 2,
                                                                       yC - config.boxSize / 2,
                                                                       config.boxSize, config.boxSize));
            if (box.isEmpty()) {
                continue;
            }
            float[] sampleBuffer = new float[box.width * box.height];
            config.raster.readPixels(box.x, box.y, box.width, box.height, sampleBuffer, ProgressMonitor.NULL);

            int[] maskBuffer = null;
            if (config.useRoiMask && config.roiMask != null) {
                maskBuffer = new int[box.width * box.height];
                config.roiMask.readPixels(box.x, box.y, box.width, box.height, maskBuffer, ProgressMonitor.NULL);
            }

            double sum = 0;
            double sumSqr = 0;
            int n = 0;
            for (int y = 0; y < box.height; y++) {
                for (int x = 0; x < box.width; x++) {
                    final int index = y * box.width + x;
                    if (config.raster.isPixelValid(box.x + x, box.y + y)
                            && (maskBuffer == null || maskBuffer[index] != 0)) {
                        final float v = sampleBuffer[index];
                        sum += v;
                        sumSqr += v * v;
                        n++;

                        if (v < sampleMin) {
                            sampleMin = v;
                        }
                        if (v > sampleMax) {
                            sampleMax = v;
                        }
                    }
                }
            }

            if (n > 0) {
                final double mean = sum / n;
                final double variance = n > 1 ? (sumSqr - (sum * sum) / n) / (n - 1) : 0.0;
                sampleValues[i] = (float) mean;
                sampleSigmas[i] = (float) (variance > 0.0 ? Math.sqrt(variance) : 0.0);
            }

            if (geoCoding != null) {
                pixelPos.x = (float) pixelPositions[i].getX() + 0.5f;
                pixelPos.y = (float) pixelPositions[i].getY() + 0.5f;
                geoPositions[i] = geoCoding.getGeoPos(pixelPos, null);
            }
        }
    }

    public int getNumPixels() {
        return pixelPositions.length;
    }

    public int getNumShapeVertices() {
        return shapeVertices.length;
    }

    public Point2D[] getShapeVertices() {
        return shapeVertices;
    }

    public int[] getShapeVertexIndexes() {
        return shapeVertexIndexes;
    }

    public Point2D[] getPixelPositions() {
        return pixelPositions;
    }

    public GeoPos[] getGeoPositions() {
        return geoPositions;
    }

    public float[] getSampleValues() {
        return sampleValues;
    }

    public float[] getSampleSigmas() {
        return sampleSigmas;
    }

    public float getSampleMin() {
        return sampleMin;
    }

    public float getSampleMax() {
        return sampleMax;
    }

    static class Config {

        RasterDataNode raster;
        Shape path;
        int boxSize;
        boolean useRoiMask;
        Mask roiMask;
        boolean connectVertices;
    }

}
