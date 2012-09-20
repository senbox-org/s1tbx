package org.esa.beam.pixex;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.dataio.geometry.VectorDataNodeReader;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.PointDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.util.FeatureUtils;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tonio Fincke
 * @author Thomas Storm
 */
public class PixExOpUtils {

    public static GeoPos getGeoPos(SimpleFeature feature) throws IOException {
        final Geometry defaultGeometry = (Geometry) feature.getDefaultGeometry();
        if (defaultGeometry == null) {
            throw new IOException("Could not read geometry of feature '" + feature.getID() + "'.");
        }
        final Point centroid = defaultGeometry.getCentroid();
        return new GeoPos((float) centroid.getY(), (float) centroid.getX());
    }

    public static SimpleFeatureType getExtendedFeatureType(SimpleFeatureType featureType) {
        final SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.init(featureType);
        final SimpleFeatureType pointFeatureType = Placemark.createPointFeatureType(
                featureType.getName().getLocalPart());
        for (AttributeDescriptor attributeDescriptor : pointFeatureType.getAttributeDescriptors()) {
            builder.add(attributeDescriptor);
        }
        List<AttributeDescriptor> attributeDescriptors = featureType.getAttributeDescriptors();
        final SimpleFeatureType extendedFeatureType = builder.buildFeatureType();
        extendedFeatureType.getUserData().put("originalAttributeDescriptors", attributeDescriptors);
        return extendedFeatureType;
    }

    public static List<SimpleFeature> extractFeatures(File matchupFile) throws IOException {
        final List<SimpleFeature> result = new ArrayList<SimpleFeature>();
        final Product dummyProduct = new Product("dummy", "dummy", 10, 10);
        FeatureIterator<SimpleFeature> featureIterator = null;
        FileReader reader = null;
        try {
            reader = new FileReader(matchupFile);
            final VectorDataNode vdn = VectorDataNodeReader.read(
                    matchupFile.getName(),
                    reader,
                    dummyProduct,
                    new FeatureUtils.FeatureCrsProvider() {
                        @Override
                        public CoordinateReferenceSystem getFeatureCrs(Product product) {
                            return DefaultGeographicCRS.WGS84;
                        }
                    },
                    new VectorDataNodeReader.PlacemarkDescriptorProvider() {
                        @Override
                        public PlacemarkDescriptor getPlacemarkDescriptor(SimpleFeatureType simpleFeatureType) {
                            return PointDescriptor.getInstance();
                        }
                    },
                    DefaultGeographicCRS.WGS84,
                    '\t',
                    false,
                    ProgressMonitor.NULL
            );
            final SimpleFeatureType extendedFeatureType = getExtendedFeatureType(vdn.getFeatureType());
            final SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(extendedFeatureType);
            featureIterator = null;
            featureIterator = vdn.getFeatureCollection().features();
            while (featureIterator.hasNext()) {
                final SimpleFeature simpleFeature = featureIterator.next();
                simpleFeatureBuilder.init(simpleFeature);
                SimpleFeature extendedFeature = simpleFeatureBuilder.buildFeature(simpleFeature.getID());
                extendedFeature.setDefaultGeometry(simpleFeature.getDefaultGeometry());
                extendedFeature.getUserData().put("originalAttributes", simpleFeature.getAttributes());
                result.add(extendedFeature);
            }
        } finally {
            if (featureIterator != null) {
                featureIterator.close();
            }
            if (reader != null) {
                reader.close();
            }
        }
        return result;
    }
}
