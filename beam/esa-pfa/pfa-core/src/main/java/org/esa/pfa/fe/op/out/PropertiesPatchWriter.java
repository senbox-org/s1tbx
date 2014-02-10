package org.esa.pfa.fe.op.out;

import com.bc.ceres.binding.PropertySet;
import org.esa.beam.framework.datamodel.Product;
import org.esa.pfa.fe.op.AttributeType;
import org.esa.pfa.fe.op.Feature;
import org.esa.pfa.fe.op.FeatureType;
import org.esa.pfa.fe.op.Patch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Writes a single Java properties file "fex-metadata.txt" and for each patch a "features.txt" into the patch directory of a product.
 *
 * @author Norman Fomferra
 */
public class PropertiesPatchWriter implements PatchWriter {

    private static final String METADATA_FILE_NAME = "fex-metadata.txt";
    public static final String FEATURES_FILE_NAME = "features.txt";

    private final File productTargetDir;

    public PropertiesPatchWriter(File productTargetDir) throws IOException {
        this.productTargetDir = productTargetDir;
    }

    @Override
    public void initialize(PropertySet configuration, Product sourceProduct, FeatureType... featureTypes) throws IOException {
        final Writer writer = new FileWriter(new File(productTargetDir, METADATA_FILE_NAME));
        try {
            writeFeatureTypes(featureTypes, writer);
        } finally {
            writer.close();
        }
    }

    @Override
    public void writePatch(Patch patch, Feature... features) throws IOException {
        final File patchTargetDir = new File(productTargetDir, patch.getPatchName());
        final File file = new File(patchTargetDir, FEATURES_FILE_NAME);

        final Writer writer = new FileWriter(file);
        try {
            for (Feature feature : features) {
                writeFeatureProperties(feature, writer);
            }
        } finally {
            writer.close();
        }
    }

    @Override
    public void close() throws IOException {
    }

    public static void writeFeatureTypes(FeatureType[] featureTypes, Writer writer) throws IOException {
        writer.write(String.format("featureTypes.length = %d%n", featureTypes.length));
        for (int i = 0; i < featureTypes.length; i++) {
            FeatureType featureType = featureTypes[i];
            writeFeatureType(featureType, i, writer);
        }
    }

    public static void writeFeatureType(FeatureType featureType, int i, Writer writer) throws IOException {
        writer.write(String.format("#%n"));
        writer.write(String.format("# Feature '%s'%n", featureType.getName()));
        writer.write(String.format("#%n"));
        writer.write(String.format("featureTypes.%d.name = %s%n", i, featureType.getName()));
        writer.write(String.format("featureTypes.%d.description = %s%n", i, featureType.getDescription()));
        if (featureType.hasAttributes()) {
            AttributeType[] attributeTypes = featureType.getAttributeTypes();
            writer.write(String.format("featureTypes.%d.attributeTypes.length = %s%n", i, attributeTypes.length));
            for (int j = 0; j < attributeTypes.length; j++) {
                AttributeType attributeType = attributeTypes[j];
                writer.write(String.format("featureTypes.%d.attributeTypes.%d.name = %s%n", i, j, attributeType.getName()));
                writer.write(String.format("featureTypes.%d.attributeTypes.%d.description = %s%n", i, j, attributeType.getDescription()));
                writer.write(String.format("featureTypes.%d.attributeTypes.%d.valueType = %s%n", i, j, attributeType.getValueType().getSimpleName()));
            }
        } else {
            writer.write(String.format("featureTypes.%d.valueType = %s%n", i, featureType.getValueType().getSimpleName()));
        }
    }


    private void writeFeatureProperties(Feature feature, Writer writer) throws IOException {
        if (feature.hasAttributes()) {
            AttributeType[] attributeTypes = feature.getFeatureType().getAttributeTypes();
            Object[] attributeValues = feature.getAttributeValues();
            for (int i = 0; i < attributeValues.length; i++) {
                if (isSupportedType(attributeTypes[i].getValueType())) {
                    Object attributeValue = attributeValues[i];
                    writer.write(String.format("%s.%s = %s%n",
                                               feature.getFeatureType().getName(),
                                               attributeTypes[i].getName(),
                                               attributeValue));
                }
            }
        } else {
            if (isSupportedType(feature.getFeatureType().getValueType())) {
                writer.write(String.format("%s = %s%n",
                                           feature.getFeatureType().getName(), feature.getValue()));
            }
        }
    }

    private static boolean isSupportedType(Class<?> valueType) {
        return Number.class.isAssignableFrom(valueType)
                || Boolean.class.isAssignableFrom(valueType)
                || Character.class.isAssignableFrom(valueType)
                || String.class.isAssignableFrom(valueType);
    }
}
