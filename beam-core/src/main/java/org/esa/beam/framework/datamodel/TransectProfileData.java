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
package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ShapeRasterizer;
import org.esa.beam.util.math.MathUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Arrays;


/**
 * A container for data which fully describes a transect profile.
 *
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

    /*
     * Since 4.5
     */
    public static TransectProfileData create(RasterDataNode raster, Shape path) throws IOException {
        return create(raster, path, 1, null);
    }

    /*
     * Since 4.10
     */
    public static TransectProfileData create(RasterDataNode raster, Shape path, int boxSize, Mask roiMask) throws IOException {
        return new TransectProfileData(raster, path, boxSize, roiMask);
    }

    /*
    * Since 4.10
    */
    public static TransectProfileData create(RasterDataNode raster, VectorDataNode pointData, int boxSize, Mask roiMask) throws IOException {
        return create(raster, createPath(pointData), boxSize, roiMask);
    }

    private static Path2D createPath(VectorDataNode pointData) {
        Path2D.Double path = new Path2D.Double();
        SimpleFeature[] simpleFeatures = pointData.getFeatureCollection().toArray(new SimpleFeature[0]);
        for (int i = 0; i < simpleFeatures.length; i++) {
            SimpleFeature simpleFeature = simpleFeatures[i];
            Geometry geometry = (Geometry) simpleFeature.getDefaultGeometry();
            Point centroid = geometry.getCentroid();
            if (i == 0) {
                path.moveTo(centroid.getX(), centroid.getY());
            } else {
                path.lineTo(centroid.getX(), centroid.getY());
            }
        }
        return path;
    }


    private TransectProfileData(RasterDataNode raster, Shape path, int boxSize, Mask roiMask) throws IOException {
        Guardian.assertNotNull("raster", raster);
        Guardian.assertNotNull("path", path);
        if (raster.getProduct() == null) {
            throw new IllegalArgumentException("raster without product");
        }

        ShapeRasterizer rasterizer = new ShapeRasterizer();
        shapeVertices = rasterizer.getVertices(path);
        shapeVertexIndexes = new int[shapeVertices.length];
        pixelPositions = rasterizer.rasterize(shapeVertices, shapeVertexIndexes);
        sampleValues = new float[pixelPositions.length];
        Arrays.fill(sampleValues, Float.NaN);
        sampleSigmas = new float[pixelPositions.length];
        Arrays.fill(sampleSigmas, Float.NaN);

        sampleMin = Float.MAX_VALUE;
        sampleMax = -Float.MAX_VALUE;

        GeoCoding geoCoding = raster.getGeoCoding();
        if (geoCoding != null) {
            geoPositions = new GeoPos[pixelPositions.length];
            Arrays.fill(geoPositions, NO_GEO_POS);
        } else {
            geoPositions = null;
        }

        final Rectangle sceneRect = new Rectangle(raster.getSceneRasterWidth(), raster.getSceneRasterHeight());
        PixelPos pixelPos = new PixelPos();
        for (int i = 0; i < pixelPositions.length; i++) {
            final int xC = MathUtils.floorInt(pixelPositions[i].getX() + 0.5f);
            final int yC = MathUtils.floorInt(pixelPositions[i].getY() + 0.5f);
            if (!sceneRect.contains(xC, yC)) {
                continue;
            }
            final Rectangle box = sceneRect.intersection(new Rectangle(xC - boxSize / 2,
                                                                       yC - boxSize / 2,
                                                                       boxSize, boxSize));
            if (box.isEmpty()) {
                continue;
            }
            float[] sampleBuffer = new float[box.width * box.height];
            raster.readPixels(box.x, box.y, box.width, box.height, sampleBuffer, ProgressMonitor.NULL);

            int[] maskBuffer = null;
            if (roiMask != null) {
                maskBuffer = new int[box.width * box.height];
                roiMask.readPixels(box.x, box.y, box.width, box.height, maskBuffer, ProgressMonitor.NULL);
            }

            float sum = 0;
            float sumSqr = 0;
            int n = 0;
            for (int y = 0; y < box.height; y++) {
                for (int x = 0; x < box.width; x++) {
                    final int index = y * box.height + x;
                    if (raster.isPixelValid(box.x + x, box.y + y)
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
                final float mean = sum / n;
                sampleValues[i] = mean;
                sampleSigmas[i] = n > 1 ? (float) Math.sqrt(1.0 / (n - 1.0) * (sumSqr - (sum * sum) / n)) : 0.0F;
            }

            if (geoPositions != null) {
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

}
