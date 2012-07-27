package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.measurement.Measurement;

import java.io.PrintWriter;

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
}
