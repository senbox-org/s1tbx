/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.core.Assert;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.io.IOException;

/**
 * Builder for instances of {@link TransectProfileData}.
 * Constraints:
 * <ul>
 *     <li>a non-null raster is mandatory</li>
 *     <li>either a shape or a point data source must be set</li>
 * </ul>
 *
 * @author Thomas Storm
 */
public class TransectProfileDataBuilder {

    final TransectProfileData.Config config;

    private VectorDataNode pointData;

    public TransectProfileDataBuilder() {
        config = new TransectProfileData.Config();
        setDefaults();
    }

    public TransectProfileData build() throws IOException {
        validate();

        if (config.path == null) {
            config.path = createPath(pointData);
        }

        return new TransectProfileData(config);
    }

    public TransectProfileDataBuilder raster(RasterDataNode raster) {
        Assert.argument(raster.getProduct() != null, "raster.getProduct() != null");
        config.raster = raster;
        return this;
    }

    public TransectProfileDataBuilder path(Shape path) {
        config.path = path;
        return this;
    }

    public TransectProfileDataBuilder pointData(VectorDataNode pointData) {
        this.pointData = pointData;
        return this;
    }

    public TransectProfileDataBuilder boxSize(int boxSize) {
        config.boxSize = boxSize;
        return this;
    }

    public TransectProfileDataBuilder useRoiMask(boolean useRoiMask) {
        config.useRoiMask = useRoiMask;
        return this;
    }

    public TransectProfileDataBuilder roiMask(Mask roiMask) {
        config.roiMask = roiMask;
        return this;
    }

    public TransectProfileDataBuilder connectVertices(boolean connectVertices) {
        config.connectVertices = connectVertices;
        return this;
    }

    private void setDefaults() {
        config.boxSize = 1;
        config.connectVertices = true;
    }

    private void validate() {
        Assert.state(config.raster != null, "raster == null");
        Assert.state(config.path != null || pointData != null, "path != null || pointData != null");
        Assert.state(config.path == null && pointData != null
                     || config.path != null && pointData == null,
                     "path == null && pointData != null || path != null && pointData == null");
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
}
