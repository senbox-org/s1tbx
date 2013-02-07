/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.measurement.writer;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.measurement.Measurement;
import org.esa.beam.pixex.output.TargetWriterFactoryAndMap;

import java.awt.image.Raster;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Writes measurements into CSV text files along with a file containing the paths to the products which
 * contributed to the list of measurements.
 */
public class MeasurementWriter {

    private final MeasurementFactory measurementFactory;
    private final TargetWriterFactoryAndMap targetFactory;
    private final FormatStrategy formatStrategy;

    public MeasurementWriter(MeasurementFactory measurementFactory,
                             TargetWriterFactoryAndMap targetFactory,
                             FormatStrategy formatStrategy) {
        this.measurementFactory = measurementFactory;
        this.targetFactory = targetFactory;
        this.formatStrategy = formatStrategy;
    }

    public void writeMeasurements(int pixelX, int pixelY,
                                  int coordinateID, String coordinateName,
                                  Product product, Raster validData) throws IOException {

        final Measurement[] measurements;
        measurements = measurementFactory.createMeasurements(pixelX, pixelY, coordinateID, coordinateName, product,
                                                             validData);

        final PrintWriter writer;
        final boolean containsWriter = targetFactory.containsWriterFor(product);
        if (containsWriter) {
            writer = targetFactory.getWriterFor(product);
        } else {
            writer = targetFactory.createWriterFor(product);
            formatStrategy.writeHeader(writer, product);
        }

        formatStrategy.writeMeasurements(product, writer, measurements);

        if (writer.checkError()) {
            throw new IOException("Error occurred while writing measurement.");
        }
    }

    public void close() {
        measurementFactory.close();
        targetFactory.close();
    }
}
