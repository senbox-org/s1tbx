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

    public static FeatureCollection<SimpleFeatureType, SimpleFeature> doOperation(
            FeatureCollection<SimpleFeatureType, SimpleFeature> sourceCollection,
            Geometry clipGeometry,
            String targetID,
            CoordinateReferenceSystem targetCrs) {


        SimpleFeatureType sourceSchema = sourceCollection.getSchema();
        CoordinateReferenceSystem sourceCrs = sourceSchema.getGeometryDescriptor().getCoordinateReferenceSystem();
        if (sourceCrs == null) {
            sourceCrs = DefaultGeographicCRS.WGS84;
        }
        if (targetCrs == null) {
            targetCrs = sourceCrs;
        }
        if (targetID == null || targetID.isEmpty()) {
            targetID = sourceCollection.getID();
        }

        final Class<?> geometryBinding = sourceSchema.getGeometryDescriptor().getType().getBinding();
        final GeometryFactory geometryFactory = new GeometryFactory();

        GeometryCoordinateSequenceTransformer transformer;
        SimpleFeatureType targetSchema;
        if (sourceCrs == targetCrs || sourceCrs.equals(targetCrs)) {
            targetSchema = sourceSchema;
            transformer = null;
        } else {
            try {
                targetSchema = FeatureTypes.transform(sourceSchema, targetCrs);
                transformer = getTransform(sourceCrs, targetCrs);
            } catch (SchemaException e) {
                throw new IllegalStateException(e);
            }
        }

        FeatureCollection<SimpleFeatureType, SimpleFeature> targetCollection = new DefaultFeatureCollection(targetID, targetSchema);
        Iterator<SimpleFeature> iterator = sourceCollection.iterator();
        try {
            while (iterator.hasNext()) {
                SimpleFeature sourceFeature = iterator.next();
                Geometry sourceGeometry = (Geometry) sourceFeature.getDefaultGeometry();

                Geometry targetGeometry;
                try {
                    targetGeometry = sourceGeometry.intersection(clipGeometry);
                } catch (TopologyException e) {
                    continue;
                }
                
                if (!targetGeometry.isEmpty()) {

                    if (MultiPolygon.class.isAssignableFrom(geometryBinding)) {
                        if (targetGeometry instanceof Polygon) {
                            targetGeometry = geometryFactory.createMultiPolygon(
                                    new Polygon[]{(Polygon) targetGeometry});
                        }
                    }

                    SimpleFeature targetFeature;
                    if (transformer != null) {
                        try {
                            targetGeometry = transformer.transform(targetGeometry);
                        } catch (TransformException ignored) {
                            continue;
                        }
                        targetFeature = SimpleFeatureBuilder.retype(sourceFeature, targetSchema);
                    } else {
                        targetFeature = SimpleFeatureBuilder.copy(sourceFeature);
                    }

                    targetFeature.setDefaultGeometry(targetGeometry);
                    targetCollection.add(targetFeature);
                }
            }
        } finally {
            sourceCollection.close(iterator);
        }

        return targetCollection;
    }
    
    public static GeometryCoordinateSequenceTransformer getTransform(CoordinateReferenceSystem sourceCrs, CoordinateReferenceSystem targetCrs) {
        GeometryCoordinateSequenceTransformer transformer;
        try {
            MathTransform transform = CRS.findMathTransform(sourceCrs, targetCrs);
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
