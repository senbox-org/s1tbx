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

/*
 * - In den Konstructor nur noch die Factories rein reichen.
 *     // WriterFactory
 *     // MeasurementFactory
 * - Der MeasurementWriter bestimmt dann nur noch das WIE geschrieben wird
 *     // Die WriterFactory bestimmt das WOHIN
 *     // Die MeasurementFactory das WAS
 * - MeasurementWriter trennen von MeasurementFactory
 * - MeasurementWriter.write(Measurement ...)
 *     // ohne PrintWriter!
 *     // Dieser wird von der Factory geliefert
 *     // Ein Measurement hält auch das Product aus dem die Daten stammen und kann
 *     // somit auch zur Abfrage getWriter(product) an der Factory genutzt werden.
 * - WriteMeasurementRegion soll die Window size bekommen.
 *     // bzw. sobald eine MeasurementFactory existiert wird die window
 *     // size nur noch in der Factorymethode gebraucht, oder der Factory im
 *     // Konstructor mitgegeben werden.
 *
 */

package org.esa.beam.measurement.writer;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.measurement.Measurement;
import org.esa.beam.pixex.output.MeasurementFactory;

import java.awt.image.Raster;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Writes measurements into CSV text files along with a file containing the paths to the products which
 * contributed to the list of measurements.
 */
public class MeasurementWriter {

    private final MeasurementFactory measurementFactory;
    private final TargetFactory targetFactory;
    private final FormatStrategy formatStrategy;

    public MeasurementWriter(MeasurementFactory measurementFactory,
                             TargetFactory targetFactory,
                             FormatStrategy formatStrategy) {
        this.measurementFactory = measurementFactory;
        this.targetFactory = targetFactory;
        this.formatStrategy = formatStrategy;
    }

    public void writeMeasurements(int pixelX, int pixelY,
                                  int coordinateID, String coordinateName,
                                  Product product, Raster validData) throws IOException {

        final Measurement[] measurements;
        measurements = measurementFactory.createMeasurements(pixelX, pixelY, coordinateID, coordinateName, product, validData);

        final PrintWriter writer;
        final boolean containsWriter = targetFactory.containsWriterFor(pixelX, pixelY, coordinateID, coordinateName, product, validData);
        if (containsWriter) {
            writer = targetFactory.getWriterFor(pixelX, pixelY, coordinateID, coordinateName, product, validData);
        } else {
            writer = targetFactory.createWriterFor(pixelX, pixelY, coordinateID, coordinateName, product, validData);
            formatStrategy.writeHeader(writer, product);
        }

        formatStrategy.writeMeasurements(writer, measurements);

        if (writer.checkError()) {
            throw new IOException("Error occurred while writing measurement.");
        }
    }

    public void close() {
        targetFactory.close();
    }
}
