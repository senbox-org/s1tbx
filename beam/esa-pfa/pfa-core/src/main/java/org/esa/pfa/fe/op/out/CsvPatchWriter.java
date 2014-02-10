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
 * Writes a single CSV file "fex-overview.csv" for all feature of a product.
 *
 * @author Norman Fomferra
 */
public class CsvPatchWriter implements PatchWriter {

    private static final String CSV_FILE_NAME = "fex-overview.csv";
    private static final char SEPARATOR = '\t';

    private final File productTargetDir;
    private Writer csvWriter;
    private int columnIndex;
    private int rowIndex;

    public CsvPatchWriter(File productTargetDir) throws IOException {
        this.productTargetDir = productTargetDir;
    }

    @Override
    public void initialize(PropertySet configuration, Product sourceProduct, FeatureType... featureTypes) throws IOException {
        csvWriter = new FileWriter(new File(productTargetDir, CSV_FILE_NAME));

        startRow();
        writeValue("index");
        writeValue("patch");
        for (FeatureType featureType : featureTypes) {
            if (featureType.hasAttributes()) {
                AttributeType[] attributeTypes = featureType.getAttributeTypes();
                for (AttributeType attributeType : attributeTypes) {
                    if (isSupportedType(attributeType.getValueType())) {
                        writeValue(featureType.getName() + "." + attributeType.getName());
                    }
                }
            } else if (isSupportedType(featureType.getValueType())) {
                writeValue(featureType.getName());
            }
        }
        endRow();
    }

    @Override
    public void writePatch(Patch patch, Feature... features) throws IOException {
        startRow();
        writeValue(String.valueOf(rowIndex));
        writeValue(patch.getPatchName());
        for (Feature feature : features) {
            if (feature.hasAttributes()) {
                AttributeType[] attributeTypes = feature.getFeatureType().getAttributeTypes();
                Object[] attributeValues = feature.getAttributeValues();
                for (int i = 0; i < attributeValues.length; i++) {
                    if (isSupportedType(attributeTypes[i].getValueType())) {
                        writeValue(String.valueOf(attributeValues[i]));
                    }
                }
            } else if (isSupportedType(feature.getFeatureType().getValueType())) {
                writeValue(String.valueOf(feature.getValue()));
            }
        }
        endRow();
    }

    @Override
    public void close() throws IOException {
        csvWriter.close();
    }

    private void startRow() {
        columnIndex = 0;
    }

    private void endRow() throws IOException {
        csvWriter.write("\n");
        rowIndex++;
    }

    private void writeValue(String str) throws IOException {
        if (columnIndex > 0) {
            csvWriter.write((int) SEPARATOR);
        }
        csvWriter.write(str);
        columnIndex++;
    }

    private static boolean isSupportedType(Class<?> valueType) {
        return Number.class.isAssignableFrom(valueType)
                || Boolean.class.isAssignableFrom(valueType)
                || Character.class.isAssignableFrom(valueType)
                || String.class.isAssignableFrom(valueType);
    }
}
