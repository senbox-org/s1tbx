package org.esa.beam.measurement.writer;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.measurement.Measurement;

import java.io.PrintWriter;

public interface FormatStrategy {

    void writeHeader(PrintWriter writer, Product product);

    void writeMeasurements(Product product, PrintWriter writer, Measurement[] measurements);

    void finish();
}
