package org.esa.beam.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
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

import java.util.Iterator;


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
     *
     * @return the clipped and possibly reprojectd {@link FeatureCollection}
     */
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
     *
     * @return the clipped and possibly reprojectd {@link FeatureCollection}
     *
     * @throws IllegalStateException if the {@code sourceCollection} has no associated CRS and {@code defaultSourceCrs}
     *                               is {@code null}
     */
    public static FeatureCollection<SimpleFeatureType, SimpleFeature> doOperation(
            FeatureCollection<SimpleFeatureType, SimpleFeature> sourceCollection,
            CoordinateReferenceSystem defaultSourceCrs,
            Geometry clipGeometry, CoordinateReferenceSystem clipCrs,
            String targetID, CoordinateReferenceSystem targetCrs) {

        SimpleFeatureType sourceSchema = sourceCollection.getSchema();
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
        if (sourceCrs == targetCrs || sourceCrs.equals(targetCrs)) {
            targetSchema = sourceSchema;
            source2TargetTransformer = null;
        } else {
            try {
                targetSchema = FeatureTypes.transform(sourceSchema, targetCrs);
                source2TargetTransformer = getTransform(sourceCrs, targetCrs);
            } catch (SchemaException e) {
                throw new IllegalStateException(e);
            }
        }

        DefaultFeatureCollection targetCollection = new DefaultFeatureCollection(targetID, targetSchema);

        Iterator<SimpleFeature> iterator = sourceCollection.iterator();
        try {
            while (iterator.hasNext()) {
                SimpleFeature sourceFeature = iterator.next();

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
                    targetCollection.add(targetFeature);
                }
            }
        } finally {
            sourceCollection.close(iterator);
        }

        return targetCollection;
    }

    private static SimpleFeature createTargetFeature(Geometry targetGeometry, SimpleFeatureType targetSchema,
                                                     SimpleFeature sourceFeature,
                                                     GeometryCoordinateSequenceTransformer source2TargetTransformer) {
        SimpleFeature targetFeature;
        if (source2TargetTransformer != null) {
            try {
                targetGeometry = source2TargetTransformer.transform(targetGeometry);
            } catch (TransformException ignored) {
//                            continue;
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
        Coordinate[] coordinates = new Coordinate[geoPositions.length + 1];
        for (int i = 0; i < geoPositions.length; i++) {
            GeoPos geoPos = geoPositions[i];
            coordinates[i] = new Coordinate(geoPos.lon, geoPos.lat);
        }
        coordinates[coordinates.length - 1] = coordinates[0];
        return gf.createPolygon(gf.createLinearRing(coordinates), null);
    }
}
