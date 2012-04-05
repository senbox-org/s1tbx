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
import org.esa.beam.jai.ImageManager;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Unstable API. Use at own risk.
 */
public class ShapefileUtils {

    public static FeatureCollection<SimpleFeatureType, SimpleFeature> createFeatureCollection(URL url,
                                                                                              CoordinateReferenceSystem targetCrs,
                                                                                              Geometry clipGeometry) throws
            IOException {
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = getFeatureSource(url);
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = featureSource.getFeatures();
        featureCollection = FeatureCollectionClipper.doOperation(featureCollection, DefaultGeographicCRS.WGS84,
                                                                 clipGeometry, DefaultGeographicCRS.WGS84,
                                                                 null, targetCrs);
        return featureCollection;
    }

    public static FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(URL url) throws IOException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ShapefileDataStoreFactory.URLP.key, url);
        map.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.TRUE);
        DataStore shapefileStore = DataStoreFinder.getDataStore(map);
        String typeName = shapefileStore.getTypeNames()[0]; // Shape files do only have one type name
        return shapefileStore.getFeatureSource(typeName);
    }

    public static FeatureCollection<SimpleFeatureType, SimpleFeature> loadShapefileForProduct(File file,
                                                                                              Product product,
                                                                                              FeatureCrsProvider crsProvider, ProgressMonitor pm) throws IOException {
        final URL url = file.toURI().toURL();
        final CoordinateReferenceSystem targetCrs = ImageManager.getModelCrs(product.getGeoCoding());
        final Geometry clipGeometry = FeatureCollectionClipper.createGeoBoundaryPolygon(product);
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = getFeatureSource(url);
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = featureSource.getFeatures();
        CoordinateReferenceSystem featureCrs = featureCollection.getSchema().getCoordinateReferenceSystem();
        if (featureCrs == null) {
            featureCrs = crsProvider.getCrs(product, featureCollection);
            if (featureCrs == null) {
                featureCrs = DefaultGeographicCRS.WGS84;
            }
        }
        return FeatureCollectionClipper.doOperation(featureCollection, featureCrs,
                                                    clipGeometry, DefaultGeographicCRS.WGS84,
                                                    null, targetCrs);
    }

    public static interface FeatureCrsProvider {
        CoordinateReferenceSystem getCrs(Product product, FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection);
    }
}
