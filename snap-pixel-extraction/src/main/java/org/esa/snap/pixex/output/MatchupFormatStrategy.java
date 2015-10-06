package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.measurement.Measurement;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MatchupFormatStrategy extends AbstractFormatStrategy {

    final Measurement[] originalMeasurements;
    private HashMap<String, Integer> attributeIndices = new HashMap<String, Integer>();

    public MatchupFormatStrategy(Measurement[] originalMeasurements,
                                 RasterNamesFactory rasterNamesFactory, int windowSize,
                                 String expression, boolean exportExpressionResult) {
        super(rasterNamesFactory, expression, windowSize, exportExpressionResult);
        this.originalMeasurements = originalMeasurements;
    }

    @Override
    public void writeHeader(PrintWriter writer, Product product) {
        writeStandardHeader(writer);
        writeWavelengthLine(writer, product);

        writeOriginalMeasurementsColumns(writer);
        writeStandardColumnNames(writer);
        writeRasterNames(writer, product);
        writer.println();
    }

    private void writeOriginalMeasurementsColumns(PrintWriter writer) {
        final List<String> originalAttributeNames = getOriginalAttributeNames();
        for (final String attributeName : originalAttributeNames) {
            writer.write(attributeName + "\t");
        }
    }

    @Override
    public void writeMeasurements(Product product, PrintWriter writer, Measurement[] measurements) {
        for (Measurement measurement : measurements) {
            Measurement matchingMeasurement = findMatchingMeasurement(measurement, originalMeasurements);
            final boolean withExpression = expression != null && exportExpressionResult;
            if (expression == null || exportExpressionResult || measurement.isValid()) {
                for (int i = 0; i < getOriginalAttributeNames().size(); i++) {
                    for (int j = 0; j < matchingMeasurement.getOriginalAttributeNames().length; j++) {
                        String originalAttributeName = matchingMeasurement.getOriginalAttributeNames()[j];
                        Integer index = attributeIndices.get(originalAttributeName);
                        if (index == i) {
                            writeValue(writer, matchingMeasurement.getValues()[j]);
                        }
                    }
                    writer.write("\t");
                }
                writeLine(writer, measurement, withExpression);
                writer.write("\n");
            }
        }
    }

    static Measurement findMatchingMeasurement(Measurement measurement, Measurement[] originalMeasurements) {
        for (Measurement currentMeasurement : originalMeasurements) {
            if (currentMeasurement.getCoordinateID() == measurement.getCoordinateID()) {
                return currentMeasurement;
            }
        }
        throw new IllegalArgumentException(
                "No matching measurement found for measurement '" + measurement.toString() + "'.");
    }

    @Override
    protected int getAttributeCount() {
        return getOriginalAttributeNames().size() + super.getAttributeCount();
    }

    private List<String> getOriginalAttributeNames() {
        List<String> attributeNames = new ArrayList<String>();
        for (Measurement originalMeasurement : originalMeasurements) {
            for (String attributeName : originalMeasurement.getOriginalAttributeNames()) {
                if (!attributeNames.contains(attributeName)) {
                    attributeNames.add(attributeName);
                    attributeIndices.put(attributeName, attributeNames.size() - 1);
                }
            }
        }
        return attributeNames;
    }
}
