package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.measurement.Measurement;
import org.esa.beam.measurement.writer.FormatStrategy;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Locale;

public abstract class AbstractFormatStrategy implements FormatStrategy {

    private static final DateFormat DATE_FORMAT = ProductData.UTC.createDateFormat("yyyy-MM-dd\tHH:mm:ss");
    protected RasterNamesFactory rasterNamesFactory;
    protected String expression;
    protected int windowSize;
    protected boolean exportExpressionResult;

    protected AbstractFormatStrategy(RasterNamesFactory rasterNamesFactory, String expression, int windowSize,
                                     boolean exportExpressionResult) {
        this.rasterNamesFactory = rasterNamesFactory;
        this.expression = expression;
        this.windowSize = windowSize;
        this.exportExpressionResult = exportExpressionResult;
    }

    protected void writeLine(PrintWriter writer, Measurement originalMeasurement, Measurement measurement,
                             boolean withExpression) {
        if (originalMeasurement != null) {
            writeValues(writer, originalMeasurement.getValues());
            writer.printf(Locale.ENGLISH, "\t");
        }

        if (withExpression) {
            writer.printf(Locale.ENGLISH, "%s\t", String.valueOf(measurement.isValid()));
        }
        final ProductData.UTC time = measurement.getTime();
        String timeString;
        if (time != null) {
            timeString = DATE_FORMAT.format(time.getAsDate());
        } else {
            timeString = " \t ";
        }
        writer.printf(Locale.ENGLISH,
                      "%d\t%d\t%s\t%.6f\t%.6f\t%.3f\t%.3f\t%s\t",
                      measurement.getProductId(), measurement.getCoordinateID(),
                      measurement.getCoordinateName(),
                      measurement.getLat(), measurement.getLon(),
                      measurement.getPixelX(), measurement.getPixelY(),
                      timeString);
        final Object[] values = measurement.getValues();
        writeValues(writer, values);
        writer.println();
    }

    private void writeValues(PrintWriter writer, Object[] values) {
        for (int i = 0; i < values.length; i++) {
            final Object value = values[i];
            if (value instanceof Number) {
                if (Double.isNaN(((Number) value).doubleValue())) {
                    writer.printf(Locale.ENGLISH, "%s", "");
                } else {
                    writer.printf(Locale.ENGLISH, "%s", value);
                }
            } else {
                writer.printf(Locale.ENGLISH, value.toString()); // todo - check: maybe need to be more specific here
            }
            if (i != values.length - 1) {
                writer.print("\t");
            }
        }
    }

}
