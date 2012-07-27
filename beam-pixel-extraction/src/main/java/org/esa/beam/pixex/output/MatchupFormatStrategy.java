package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.measurement.Measurement;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class MatchupFormatStrategy extends AbstractFormatStrategy {

    final Measurement[] originalMeasurements;

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
    public void writeMeasurements(PrintWriter writer, Measurement[] measurements) {
        for (Measurement measurement : measurements) {
            Measurement matchingMeasurement = findMatchingMeasurement(measurement);
            if (expression == null || exportExpressionResult || measurement.isValid()) {
                final boolean withExpression = expression != null && exportExpressionResult;
                writeLine(writer, matchingMeasurement, measurement, withExpression);
            }
        }
    }

    Measurement findMatchingMeasurement(Measurement measurement) {
        for (Measurement currentMeasurement : originalMeasurements) {
            if (currentMeasurement.getCoordinateID() == measurement.getCoordinateID()) {
                return currentMeasurement;
            }
        }
        throw new IllegalArgumentException("No matching measurement found for measurement '" + measurement.toString() + "'.");
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
                }
            }
        }
        return attributeNames;
    }
}
