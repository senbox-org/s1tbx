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

package org.esa.beam.util;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.framework.datamodel.Product;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @deprecated since BEAM 4.10, use FeatureUtils instead
 */
@Deprecated
public class FeatureCollectionClipper {

    private FeatureCollectionClipper() {
    }

    /**
     * Clips the given {@code sourceCollection} against the {@code clipGeometry} and reprojects the clipped features
     * to the targetCrs.
     *
     * @param sourceCollection the feature collection to be clipped and reprojected. If it does not
     *                         have an associated CRS, {@link DefaultGeographicCRS#WGS84 WGS_84} is assumed.
     * @param clipGeometry     the geometry used for clipping. Assumed to be in
     *                         {@link DefaultGeographicCRS#WGS84 WGS_84} coordinates.
     * @param targetID         the ID of the resulting {@link FeatureCollection}. If {@code null} the ID of
     *                         the sourceCollection is used.
     * @param targetCrs        the CRS the {@link FeatureCollection} is reprojected to. If {@code null} no reprojection
     *                         is applied.
     * @return the clipped and possibly reprojectd {@link FeatureCollection}
     * @deprecated since BEAM 4.10, use FeatureUtils instead
     */
    @Deprecated
    public static FeatureCollection<SimpleFeatureType, SimpleFeature> doOperation(
            FeatureCollection<SimpleFeatureType, SimpleFeature> sourceCollection,
            Geometry clipGeometry,
            String targetID,
            CoordinateReferenceSystem targetCrs) {

        return doOperation(sourceCollection, DefaultGeographicCRS.WGS84,
                           clipGeometry, DefaultGeographicCRS.WGS84,
                           targetID, targetCrs);
    }

    /**
     * Clips the given {@code sourceCollection} against the {@code clipGeometry} and reprojects the clipped features
     * to the targetCrs.
     *
     * @param sourceCollection the feature collection to be clipped and reprojected. If it does not
     *                         have an associated CRS, the one specified by {@code defaultSourceCrs} is used.
     * @param defaultSourceCrs if {@code sourceCollection} does not have an associated CRS, this one is used.
     * @param clipGeometry     the geometry used for clipping
     * @param clipCrs          the CRS of the {@code clipGeometry}
     * @param targetID         the ID of the resulting {@link FeatureCollection}. If {@code null} the ID of
     *                         the sourceCollection is used.
     * @param targetCrs        the CRS the {@link FeatureCollection} is reprojected to. If {@code null} no reprojection
     *                         is applied.
     * @return the clipped and possibly reprojectd {@link FeatureCollection}
     * @throws IllegalStateException if the {@code sourceCollection} has no associated CRS and {@code defaultSourceCrs}
     *                               is {@code null}
     * @deprecated since BEAM 4.10, use FeatureUtils instead
     */
    @Deprecated
    public static FeatureCollection<SimpleFeatureType, SimpleFeature> doOperation(
            FeatureCollection<SimpleFeatureType, SimpleFeature> sourceCollection,
            CoordinateReferenceSystem defaultSourceCrs,
            Geometry clipGeometry, CoordinateReferenceSystem clipCrs,
            String targetID, CoordinateReferenceSystem targetCrs) {

        return FeatureUtils.clipCollection(sourceCollection,
                                           defaultSourceCrs,
                                           clipGeometry,
                                           clipCrs,
                                           targetID,
                                           targetCrs,
                                           ProgressMonitor.NULL);
    }

    /**
     * @deprecated since BEAM 4.10, use FeatureUtils instead
     */
    @Deprecated
    public static Geometry createGeoBoundaryPolygon(Product product) {
        return FeatureUtils.createGeoBoundaryPolygon(product);
    }

}
