package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.measurement.Measurement;
import org.esa.snap.measurement.writer.FormatStrategy;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public abstract class AbstractFormatStrategy implements FormatStrategy {

    private static final String[] STANDARD_COLUMN_NAMES = {
            "ProdID",
            "CoordID",
            "Name",
            "Latitude",
            "Longitude",
            "PixelX",
            "PixelY",
            "Date(yyyy-MM-dd)",
            "Time(HH_mm_ss)"
    };

    private static final DateFormat DATE_FORMAT = ProductData.UTC.createDateFormat("yyyy-MM-dd\tHH:mm:ss");
    protected RasterNamesFactory rasterNamesFactory;
    protected String expression;
    protected int windowSize;
    protected boolean exportExpressionResult;
    protected final boolean includeExpressionInTable;

    protected AbstractFormatStrategy(RasterNamesFactory rasterNamesFactory, String expression, int windowSize,
                                     boolean exportExpressionResult) {
        this.rasterNamesFactory = rasterNamesFactory;
        this.expression = expression;
        this.windowSize = windowSize;
        this.exportExpressionResult = exportExpressionResult;
        includeExpressionInTable = expression != null && exportExpressionResult;
    }

    protected void writeStandardHeader(PrintWriter writer) {
        writer.printf("# SNAP pixel extraction export table%n");
        writer.printf("#%n");
        writer.printf(Locale.ENGLISH, "# Window size: %d%n", windowSize);
        if (expression != null) {
            writer.printf("# Expression: %s%n", expression);
        }

        final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");
        writer.printf(Locale.ENGLISH, "# Created on:\t%s%n%n", dateFormat.format(new Date()));
    }

    protected void writeWavelengthLine(PrintWriter writer, Product product) {
        final String[] rasterNames = rasterNamesFactory.getRasterNames(product);
        if (product != null) {
            ArrayList<Float> wavelengthList = new ArrayList<Float>();
            for (String rasterName : rasterNames) {
                RasterDataNode rasterDataNode = product.getRasterDataNode(rasterName);
                if (rasterDataNode instanceof Band) {
                    Band band = (Band) rasterDataNode;
                    wavelengthList.add(band.getSpectralWavelength());
                } else {
                    wavelengthList.add(0.0F);
                }
            }
            if (!wavelengthList.isEmpty()) {
                Float[] wavelengthArray = wavelengthList.toArray(new Float[wavelengthList.size()]);
                String patternStart = "# Wavelength:";
                int attributeCount = getAttributeCount();
                String patternPadding = "";
                for (int i = 0; i < attributeCount; i++) {
                    patternPadding += "\t ";
                }
                if (includeExpressionInTable) {
                    patternPadding += "\t ";
                }
                patternPadding = patternPadding.substring(0, patternPadding.length() - 1);
                writer.printf(Locale.ENGLISH, patternStart + patternPadding + "%s%n",
                              StringUtils.arrayToString(wavelengthArray, "\t"));
            }
        }
    }

    protected void writeStandardColumnNames(PrintWriter writer) {
        if (includeExpressionInTable) {
            writer.print("Expression result\t");
        }
        for (int i = 0; i < STANDARD_COLUMN_NAMES.length; i++) {
            writer.print(STANDARD_COLUMN_NAMES[i]);
            if (i < STANDARD_COLUMN_NAMES.length - 1) {
                writer.print("\t");
            }
        }
    }

    protected void writeRasterNames(PrintWriter writer, Product product) {
        final String[] rasterNames = rasterNamesFactory.getRasterNames(product);
        for (String name : rasterNames) {
            writer.printf(Locale.ENGLISH, "\t%s", name);
        }
    }

    protected int getAttributeCount() {
        return STANDARD_COLUMN_NAMES.length;
    }

    protected void writeLine(PrintWriter writer, Measurement measurement, boolean withExpression) {
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
    }

    private void writeValues(PrintWriter writer, Object[] values) {
        for (int i = 0; i < values.length; i++) {
            final Object value = values[i];
            writeValue(writer, value);
            if (i != values.length - 1) {
                writer.print("\t");
            }
        }
    }

    protected void writeValue(PrintWriter writer, Object value) {
        if (value instanceof Number) {
            if (Double.isNaN(((Number) value).doubleValue())) {
                writer.printf(Locale.ENGLISH, "%s", "");
            } else {
                writer.printf(Locale.ENGLISH, "%s", value);
            }
        } else if (value == null) {
            writer.printf(Locale.ENGLISH, "");
        } else {
            writer.printf(Locale.ENGLISH, value.toString());
        }
    }

    @Override
    public void finish() {
    }

}
