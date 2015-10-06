/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.measurement.Measurement;

import java.io.PrintWriter;

public class DefaultFormatStrategy extends AbstractFormatStrategy {

    public DefaultFormatStrategy(final RasterNamesFactory rasterNamesFactory, final int windowSize,
                                 final String expression, final boolean exportExpressionResult) {
        super(rasterNamesFactory, expression, windowSize, exportExpressionResult);
    }

    @Override
    public void writeHeader(PrintWriter writer, Product product) {

        writeStandardHeader(writer);
        writeWavelengthLine(writer, product);
        writeStandardColumnNames(writer);
        writeRasterNames(writer, product);

        writer.println();
    }

    @Override
    public void writeMeasurements(Product product, PrintWriter writer, Measurement[] measurements) {
        try {
            for (Measurement measurement : measurements) {
                write(writer, measurement);
            }
        } finally {
            writer.flush();
        }
    }

    private void write(PrintWriter writer, Measurement measurement) {
        if (expression == null || exportExpressionResult || measurement.isValid()) {
            final boolean withExpression = expression != null && exportExpressionResult;
            writeLine(writer, measurement, withExpression);
            writer.write("\n");
        }
    }

}
