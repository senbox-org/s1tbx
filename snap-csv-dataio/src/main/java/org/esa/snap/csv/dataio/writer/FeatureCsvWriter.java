package org.esa.snap.csv.dataio.writer;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.io.IOException;
import java.util.List;

/**
 * Implementation of {@link CsvWriter} capable of writing a feature type and an associated feature collection.
 * There are the following different kinds of valid input:
 * <ol>
 * <li>an array containing an instance of {@link SimpleFeatureType} and an instance of {@link FeatureCollection}</li>
 * <li>an array containing an instance of {@link SimpleFeatureType} and instances of {@link SimpleFeature}</li>
 * </ol>
 *
 * @author Thomas Storm
 */
class FeatureCsvWriter implements CsvWriter {

    private WriteStrategy writer;
    private OutputFormatStrategy targetFormat;
    private SimpleFeatureType featureType;
    private ListFeatureCollection featureCollection;

    FeatureCsvWriter(WriteStrategy writer, OutputFormatStrategy targetFormat) {
        this.writer = writer;
        this.targetFormat = targetFormat;
    }

    @Override
    public void writeCsv(Object... input) throws IOException {
        validate(input);
        extract(input);

        final StringBuilder csv = new StringBuilder();
        final List<AttributeDescriptor> attributeDescriptors = featureType.getAttributeDescriptors();
        String[] attributes = new String[attributeDescriptors.size()];
        Class[] types = new Class[attributeDescriptors.size()];
        for (int i = 0; i < attributeDescriptors.size(); i++) {
            AttributeDescriptor attributeDescriptor = attributeDescriptors.get(i);
            attributes[i] = attributeDescriptor.getName().toString();
            types[i] = attributeDescriptor.getType().getBinding();
        }
        final String header = targetFormat.formatHeader(attributes, types);
        csv.append(header);
        SimpleFeature[] simpleFeatureArray = toSimpleFeatureArray(featureCollection);
        for (int recordNo = 0; recordNo < simpleFeatureArray.length; recordNo++) {
            SimpleFeature feature = simpleFeatureArray[recordNo];
            String[] values = new String[feature.getAttributeCount()];
            for (int i = 0; i < values.length; i++) {
                values[i] = feature.getAttribute(i).toString();
            }
            csv.append("\n");
            csv.append(targetFormat.formatRecord(recordNo + "", values));
        }
        csv.append("\n");
        writer.writeCsv(csv.toString());
    }

    @Override
    public boolean isValidInput(Object... input) {
        if(input == null) {
            return false;
        }
        if (input.length == 2 && (input[0] instanceof SimpleFeatureType && input[1] instanceof FeatureCollection)) {
            return true;
        } else if (input.length >= 2 && input[0] instanceof SimpleFeatureType) {
            for (int i = 1; i < input.length; i++) {
                if (!(input[i] instanceof SimpleFeature)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private void validate(Object... input) {
        if (!isValidInput(input)) {
            final StringBuilder message = new StringBuilder("Illegal input for writing pixels as CSV file: '");
            for (int i = 0; i < input.length; i++) {
                Object o = input[i];
                message.append(o.toString());
                if (i == input.length - 1) {
                    message.append(", ");
                }
            }
            message.append("'");
            throw new IllegalArgumentException(message.toString());
        }
    }

    private void extract(Object[] input) throws IOException {
        if (input.length == 2 && (input[0] instanceof SimpleFeatureType && input[1] instanceof FeatureCollection)) {
            this.featureType = (SimpleFeatureType) input[0];
            FeatureCollection<SimpleFeatureType, SimpleFeature> simpleFeatureFeatureCollection = (FeatureCollection<SimpleFeatureType, SimpleFeature>) input[1];
            this.featureCollection = new ListFeatureCollection(new DefaultFeatureCollection(simpleFeatureFeatureCollection));
        } else if (input.length >= 2 && input[0] instanceof SimpleFeatureType) {
            this.featureType = (SimpleFeatureType) input[0];
            this.featureCollection = new ListFeatureCollection(featureType);
            for (int i = 1; i < input.length; i++) {
                featureCollection.add((SimpleFeature) input[i]);
            }
        }
    }

    private SimpleFeature[] toSimpleFeatureArray(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
        final Object[] objects = featureCollection.toArray(new Object[featureCollection.size()]);
        final SimpleFeature[] simpleFeatures = new SimpleFeature[objects.length];
        for (int i = 0; i < simpleFeatures.length; i++) {
            simpleFeatures[i] = (SimpleFeature)objects[i];
        }
        return simpleFeatures;
    }
}
