/*
 * $Id: TransectProfileData.java,v 1.2 2006/12/08 13:48:37 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ShapeRasterizer;
import org.esa.beam.util.math.MathUtils;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.io.IOException;


/**
 * A container for data which fully describes a transect profile.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class TransectProfileData {

    private final Point2D[] shapeVertices;
    private final int[] shapeVertexIndexes;
    private final Point2D[] pixelPositions;
    private final GeoPos[] geoPositions;
    private final float[] sampleValues;
    private float sampleMin;
    private float sampleMax;

    public static TransectProfileData create(RasterDataNode raster, Shape shape) throws IOException {
        return new TransectProfileData(raster, shape);
    }

    private TransectProfileData(RasterDataNode raster, Shape shape) throws IOException {
        Guardian.assertNotNull("raster", raster);
        Guardian.assertNotNull("shape", shape);
        if (raster.getProduct() == null) {
            throw new IllegalArgumentException("raster without product");
        }
        raster.ensureValidMaskComputed(ProgressMonitor.NULL);

        ShapeRasterizer rasterizer = new ShapeRasterizer();
        shapeVertices = rasterizer.getVertices(shape);
        shapeVertexIndexes = new int[shapeVertices.length];
        pixelPositions = rasterizer.rasterize(shapeVertices, shapeVertexIndexes);
        sampleValues = new float[pixelPositions.length];
        sampleMin = Float.MAX_VALUE;
        sampleMax = -Float.MAX_VALUE;

        GeoCoding geoCoding = raster.getGeoCoding();
        if (geoCoding != null) {
            geoPositions = new GeoPos[pixelPositions.length];
        } else {
            geoPositions = null;
        }

        PixelPos pixelPos = new PixelPos();
        float[] sampleBuffer = new float[1];
        for (int i = 0; i < pixelPositions.length; i++) {
            pixelPos.x = (float) pixelPositions[i].getX() + 0.5f;
            pixelPos.y = (float) pixelPositions[i].getY() + 0.5f;
            final int x = MathUtils.floorInt(pixelPos.x);
            final int y = MathUtils.floorInt(pixelPos.y);
            if (x >= 0 && x < raster.getSceneRasterWidth()
                && y >= 0 && y < raster.getSceneRasterHeight()) {

                float sampleValue;
                if (raster.hasRasterData()) {
                    sampleValue = raster.getPixelFloat(x, y);
                } else {
                    raster.readPixels(x, y, 1, 1, sampleBuffer, ProgressMonitor.NULL);
                    sampleValue = sampleBuffer[0];
                }
                if (raster.isPixelValid(x, y)) {
                    if (sampleValue < sampleMin) {
                        sampleMin = sampleValue;
                    }
                    if (sampleValue > sampleMax) {
                        sampleMax = sampleValue;
                    }
                    sampleValues[i] = sampleValue;
                }else {
                    sampleValues[i] = Float.NaN;
                }
            }

            if (geoPositions != null) {
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

    public float getSampleMin() {
        return sampleMin;
    }

    public float getSampleMax() {
        return sampleMax;
    }
}
