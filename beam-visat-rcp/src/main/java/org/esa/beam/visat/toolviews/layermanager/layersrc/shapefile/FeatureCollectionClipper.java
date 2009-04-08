package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
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


class FeatureCollectionClipper {

    private static int idCounter;

    private FeatureCollectionClipper() {
    }

    public static FeatureCollection<SimpleFeatureType, SimpleFeature> doOperation(
            FeatureCollection<SimpleFeatureType, SimpleFeature> sourceCollection,
            Geometry clipGeometry,
            CoordinateReferenceSystem targetCrs) {


        SimpleFeatureType sourceSchema = sourceCollection.getSchema();
        CoordinateReferenceSystem sourceCrs = sourceSchema.getGeometryDescriptor().getCoordinateReferenceSystem();
        if (sourceCrs == null) {
            sourceCrs = DefaultGeographicCRS.WGS84;
        }
        if (targetCrs == null) {
            targetCrs = sourceCrs;
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
                MathTransform transform = CRS.findMathTransform(sourceCrs, targetCrs);
                transformer = new GeometryCoordinateSequenceTransformer();
                transformer.setMathTransform(transform);
                transformer.setCoordinateReferenceSystem(targetCrs);
            } catch (SchemaException e) {
                throw new IllegalStateException(e);
            } catch (FactoryException e) {
                throw new IllegalStateException(e);
            }
        }

        FeatureCollection<SimpleFeatureType, SimpleFeature> targetCollection = new DefaultFeatureCollection(createId(),
                                                                                                            targetSchema);
        Iterator<SimpleFeature> iterator = sourceCollection.iterator();
        try {
            while (iterator.hasNext()) {
                SimpleFeature sourceFeature = iterator.next();
                Geometry sourceGeometry = (Geometry) sourceFeature.getDefaultGeometry();
                Geometry targetGeometry = sourceGeometry.intersection(clipGeometry);
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

    private static String createId() {
        idCounter++;
        return FeatureCollectionClipper.class + "_" + idCounter;
    }
}
