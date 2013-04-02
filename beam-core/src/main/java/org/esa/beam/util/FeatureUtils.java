/*
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

package org.esa.beam.util;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.jai.ImageManager;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Unstable API. Use at own risk.
 */
public class FeatureUtils {

    public static FeatureCollection<SimpleFeatureType, SimpleFeature> createFeatureCollection(URL url,
                                                                                              CoordinateReferenceSystem targetCrs,
                                                                                              Geometry clipGeometry) throws
            IOException {
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = getFeatureSource(url);
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = featureSource.getFeatures();
        featureCollection = clipCollection(featureCollection,
                                           DefaultGeographicCRS.WGS84,
                                           clipGeometry,
                                           DefaultGeographicCRS.WGS84,
                                           null,
                                           targetCrs,
                                           ProgressMonitor.NULL);
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
        pm.beginTask("Loading Shapefile", 100);
        try {
            FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = loadFeatureCollectionFromShapefile(file);
            pm.worked(10);
            return clipFeatureCollectionToProductBounds(featureCollection, product, crsProvider, pm);
        } finally {
            pm.done();
        }
    }

    public static FeatureCollection<SimpleFeatureType, SimpleFeature> clipFeatureCollectionToProductBounds(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection, Product product, FeatureCrsProvider crsProvider, ProgressMonitor pm) {
        final CoordinateReferenceSystem targetCrs = ImageManager.getModelCrs(product.getGeoCoding());
        final Geometry clipGeometry = createGeoBoundaryPolygon(product);
        pm.worked(10);
        CoordinateReferenceSystem featureCrs = featureCollection.getSchema().getCoordinateReferenceSystem();
        if (featureCrs == null) {
            featureCrs = crsProvider.getFeatureCrs(product);
        }
        return FeatureUtils.clipCollection(featureCollection,
                                           featureCrs,
                                           clipGeometry,
                                           DefaultGeographicCRS.WGS84,
                                           null,
                                           targetCrs,
                                           SubProgressMonitor.create(pm, 80));
    }

    public static FeatureCollection<SimpleFeatureType, SimpleFeature> loadFeatureCollectionFromShapefile(File shapefile) throws IOException {
        final URL shapefileUrl = shapefile.toURI().toURL();
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = getFeatureSource(shapefileUrl);
        return featureSource.getFeatures();
    }

    public static String createFeatureTypeName(String defaultGeometry) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMMyyyy'T'HH.mm.ss");
        String currentTime = simpleDateFormat.format(Calendar.getInstance().getTime());
        return String.format("org.esa.beam_%s_%s", defaultGeometry, currentTime);
    }

    public static String createFeatureId(int base) {
        return "ID" + String.format("%08d", base);
    }

    public static interface FeatureCrsProvider {
        CoordinateReferenceSystem getFeatureCrs(Product product);
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
     * @return the clipped and possibly reprojected {@link FeatureCollection}
     * @throws IllegalStateException if the {@code sourceCollection} has no associated CRS and {@code defaultSourceCrs}
     *                               is {@code null}
     */
    public static FeatureCollection<SimpleFeatureType, SimpleFeature> clipCollection(
            FeatureCollection<SimpleFeatureType, SimpleFeature> sourceCollection,
            CoordinateReferenceSystem defaultSourceCrs,
            Geometry clipGeometry, CoordinateReferenceSystem clipCrs,
            String targetID, CoordinateReferenceSystem targetCrs, ProgressMonitor pm) {

        try {
            pm.beginTask("Clipping features", sourceCollection.size());

            SimpleFeatureType sourceSchema = sourceCollection.getSchema();
            Map<Object, Object> userData = sourceSchema.getUserData();
            CoordinateReferenceSystem sourceCrs = sourceSchema.getCoordinateReferenceSystem();
            if (targetID == null || targetID.isEmpty()) {
                targetID = sourceCollection.getID();
            }
            if (sourceCrs == null) {
                sourceCrs = defaultSourceCrs;
            }
            if (sourceCrs == null) {
                throw new IllegalStateException("'sourceCollection' has no CRS defined and 'defaultSourceCrs' is null");
            }
            try {
                sourceSchema = FeatureTypes.transform(sourceSchema, sourceCrs);
            } catch (SchemaException e) {
                throw new IllegalStateException(e);
            }
            if (targetCrs == null) {
                targetCrs = sourceCrs;
            }

            try {
                GeometryCoordinateSequenceTransformer clip2SourceTransformer = getTransform(clipCrs, sourceCrs);
                clipGeometry = clip2SourceTransformer.transform(clipGeometry);
            } catch (TransformException e) {
                throw new IllegalStateException(e);
            }

            GeometryCoordinateSequenceTransformer source2TargetTransformer;
            SimpleFeatureType targetSchema;

            try {
                targetSchema = FeatureTypes.transform(sourceSchema, targetCrs);
                targetSchema.getUserData().putAll(userData);
                source2TargetTransformer = getTransform(sourceCrs, targetCrs);
            } catch (SchemaException e) {
                throw new IllegalStateException(e);
            }

            DefaultFeatureCollection targetCollection = new DefaultFeatureCollection(targetID, targetSchema);

            final FeatureIterator<SimpleFeature> features = sourceCollection.features();
            try {
                while (features.hasNext()) {
                    SimpleFeature sourceFeature = features.next();

                    pm.worked(1);

                    Geometry targetGeometry;
                    try {
                        Geometry sourceGeometry = (Geometry) sourceFeature.getDefaultGeometry();
                        targetGeometry = getClippedGeometry(sourceGeometry, clipGeometry);
                    } catch (TopologyException ignored) {
                        continue;
                    }

                    if (!targetGeometry.isEmpty()) {
                        SimpleFeature targetFeature = createTargetFeature(targetGeometry, targetSchema,
                                                                          sourceFeature, source2TargetTransformer);
                        if (targetFeature != null) {
                            targetCollection.add(targetFeature);
                        }
                    }
                }
            } finally {
                features.close();
            }

            return targetCollection;
        } finally {
            pm.done();
        }
    }

    private static SimpleFeature createTargetFeature(Geometry targetGeometry, SimpleFeatureType targetSchema,
                                                     SimpleFeature sourceFeature,
                                                     GeometryCoordinateSequenceTransformer source2TargetTransformer) {
        SimpleFeature targetFeature;
        if (source2TargetTransformer != null) {
            try {
                targetGeometry = source2TargetTransformer.transform(targetGeometry);
            } catch (Exception e) {
                Debug.trace(e);
                return null;
            }
            targetFeature = SimpleFeatureBuilder.retype(sourceFeature, targetSchema);
        } else {
            targetFeature = SimpleFeatureBuilder.copy(sourceFeature);
        }

        targetFeature.setDefaultGeometry(targetGeometry);
        return targetFeature;
    }

    private static Geometry getClippedGeometry(Geometry sourceGeometry, Geometry clipGeometry) {
        Geometry targetGeometry = sourceGeometry.intersection(clipGeometry);
        if (targetGeometry instanceof Polygon) {
            final GeometryFactory geometryFactory = new GeometryFactory();
            if (MultiPolygon.class.isAssignableFrom(sourceGeometry.getClass())) {
                targetGeometry = geometryFactory.createMultiPolygon(new Polygon[]{(Polygon) targetGeometry});
            }
        }
        return targetGeometry;
    }

    public static GeometryCoordinateSequenceTransformer getTransform(CoordinateReferenceSystem sourceCrs,
                                                                     CoordinateReferenceSystem targetCrs) {
        GeometryCoordinateSequenceTransformer transformer;
        try {
            MathTransform transform = CRS.findMathTransform(sourceCrs, targetCrs, true);
            transformer = new GeometryCoordinateSequenceTransformer();
            transformer.setMathTransform(transform);
            transformer.setCoordinateReferenceSystem(targetCrs);
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        }
        return transformer;
    }

    public static Geometry createGeoBoundaryPolygon(Product product) {
        GeometryFactory gf = new GeometryFactory();
        GeoPos[] geoPositions = ProductUtils.createGeoBoundary(product, 100);
        Coordinate[] coordinates;
        if(geoPositions.length >= 0 && geoPositions.length <= 3) {
            coordinates = new Coordinate[0];
        } else {
            coordinates = new Coordinate[geoPositions.length + 1];
            for (int i = 0; i < geoPositions.length; i++) {
                GeoPos geoPos = geoPositions[i];
                coordinates[i] = new Coordinate(geoPos.lon, geoPos.lat);
            }
            coordinates[coordinates.length - 1] = coordinates[0];
        }
        return gf.createPolygon(gf.createLinearRing(coordinates), null);
    }

    private static FeatureCollection<SimpleFeatureType, SimpleFeature> transformPixelPosToGeoPos(FeatureCollection<SimpleFeatureType, SimpleFeature> fc, GeoCoding geoCoding) {
        Iterator<SimpleFeature> iterator = fc.iterator();
        FeatureCollection<SimpleFeatureType, SimpleFeature> transformedFc = new DefaultFeatureCollection(fc.getID(), fc.getSchema());
        while (iterator.hasNext()) {
            SimpleFeature sourceFeature = iterator.next();
            Geometry geometry = (Geometry) sourceFeature.getDefaultGeometry();
            GeometryCoordinateSequenceTransformer transformer = new GeometryCoordinateSequenceTransformer();
            transformer.setMathTransform(geoCoding.getImageToMapTransform());
            transformer.setCoordinateReferenceSystem(geoCoding.getMapCRS());
            try {
                geometry = transformer.transform(geometry);
            } catch (TransformException e) {
                throw new IllegalStateException(e);
            }
            sourceFeature.setDefaultGeometry(geometry);
            transformedFc.add(sourceFeature);
        }
        return transformedFc;
    }
}
