package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.measurement.Measurement;

import java.io.PrintWriter;

public class WriteWithOriginalOutputFormatStrategy extends AbstractFormatStrategy {

    final Measurement[] originalMeasurements;

    public WriteWithOriginalOutputFormatStrategy(Measurement[] originalMeasurements,
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
        Measurement matchingMeasurement = null;
        double minDelta = Double.POSITIVE_INFINITY;
        for (Measurement currentMeasurement : originalMeasurements) {
            final float latDelta = (float) Math.pow(currentMeasurement.getLat() - measurement.getLat(), 2);
            final float lonDelta = (float) Math.pow(currentMeasurement.getLon() - measurement.getLon(), 2);
            double currentDelta = latDelta + lonDelta;
            if (currentDelta < minDelta) {
                minDelta = currentDelta;
                matchingMeasurement = currentMeasurement;
            }
        }
        return matchingMeasurement;
    }
}
