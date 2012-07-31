package org.esa.beam.pixex;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.csv.dataio.CsvFile;
import org.esa.beam.csv.dataio.CsvSource;
import org.esa.beam.csv.dataio.CsvSourceParser;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.io.File;
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
        CsvSourceParser csvSourceParser = null;
        try {
            csvSourceParser = CsvFile.createCsvSourceParser(matchupFile.getAbsolutePath());
            final CsvSource csvSource = csvSourceParser.parseMetadata();
            csvSourceParser.parseRecords(0, csvSource.getRecordCount());

            final SimpleFeatureType extendedFeatureType = getExtendedFeatureType(
                    csvSource.getFeatureType());

            final SimpleFeature[] simpleFeatures = csvSource.getSimpleFeatures();
            final SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(extendedFeatureType);
            for (SimpleFeature simpleFeature : simpleFeatures) {
                simpleFeatureBuilder.init(simpleFeature);
                SimpleFeature extendedFeature = simpleFeatureBuilder.buildFeature(simpleFeature.getID());
                extendedFeature.setDefaultGeometry(simpleFeature.getDefaultGeometry());
                extendedFeature.getUserData().put("originalAttributes", simpleFeature.getAttributes());
                result.add(extendedFeature);
            }
        } finally {
            if (csvSourceParser != null) {
                csvSourceParser.close();
            }
        }

        return result;
    }
}
