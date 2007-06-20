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
 * @version $Revision: 1.2 $ $Date: 2006/12/08 13:48:37 $
 */
public class TransectProfileData {

    private final Point2D[] _shapeVertices;
    private final int[] _shapeVertexIndexes;
    private final Point2D[] _pixelPositions;
    private final GeoPos[] _geoPositions;
    private final float[] _sampleValues;
    private float _sampleMin;
    private float _sampleMax;

    public static TransectProfileData create(RasterDataNode raster, Shape shape) throws IOException {
        return new TransectProfileData(raster, shape);
    }

    private TransectProfileData(RasterDataNode raster, Shape shape) throws IOException {
        Guardian.assertNotNull("raster", raster);
        Guardian.assertNotNull("shape", shape);
        if (raster.getProduct() == null) {
            throw new IllegalArgumentException("raster without product");
        }

        ShapeRasterizer rasterizer = new ShapeRasterizer();
        _shapeVertices = rasterizer.getVertices(shape);
        _shapeVertexIndexes = new int[_shapeVertices.length];
        _pixelPositions = rasterizer.rasterize(_shapeVertices, _shapeVertexIndexes);
        _sampleValues = new float[_pixelPositions.length];
        _sampleMin = Float.MAX_VALUE;
        _sampleMax = -Float.MAX_VALUE;

        GeoCoding geoCoding = raster.getGeoCoding();
        if (geoCoding != null) {
            _geoPositions = new GeoPos[_pixelPositions.length];
        } else {
            _geoPositions = null;
        }

        PixelPos pixelPos = new PixelPos();
        float sampleValue;
        float[] sampleBuffer = new float[1];
        for (int i = 0; i < _pixelPositions.length; i++) {
            pixelPos.x = (float) _pixelPositions[i].getX() + 0.5f;
            pixelPos.y = (float) _pixelPositions[i].getY() + 0.5f;
            final int x = MathUtils.floorInt(pixelPos.x);
            final int y = MathUtils.floorInt(pixelPos.y);
            if (x >= 0 && x < raster.getSceneRasterWidth()
                && y >= 0 && y < raster.getSceneRasterHeight()) {

                if (raster.hasRasterData()) {
                    sampleValue = raster.getPixelFloat(x, y);
                } else {
                    raster.readPixels(x, y, 1, 1, sampleBuffer, ProgressMonitor.NULL);
                    sampleValue = sampleBuffer[0];
                }
                if (sampleValue < _sampleMin) {
                    _sampleMin = sampleValue;
                }
                if (sampleValue > _sampleMax) {
                    _sampleMax = sampleValue;
                }
                _sampleValues[i] = sampleValue;
            }

            if (_geoPositions != null) {
                _geoPositions[i] = geoCoding.getGeoPos(pixelPos, null);
            }
        }
    }

    public int getNumPixels() {
        return _pixelPositions.length;
    }

    public int getNumShapeVertices() {
        return _shapeVertices.length;
    }

    public Point2D[] getShapeVertices() {
        return _shapeVertices;
    }

    public int[] getShapeVertexIndexes() {
        return _shapeVertexIndexes;
    }

    public Point2D[] getPixelPositions() {
        return _pixelPositions;
    }

    public GeoPos[] getGeoPositions() {
        return _geoPositions;
    }

    public float[] getSampleValues() {
        return _sampleValues;
    }

    public float getSampleMin() {
        return _sampleMin;
    }

    public float getSampleMax() {
        return _sampleMax;
    }
}
