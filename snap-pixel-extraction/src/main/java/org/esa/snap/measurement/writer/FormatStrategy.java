package org.esa.snap.measurement.writer;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.measurement.Measurement;

import java.io.PrintWriter;

public interface FormatStrategy {

    void writeHeader(PrintWriter writer, Product product);

    void writeMeasurements(Product product, PrintWriter writer, Measurement[] measurements);

    void finish();
}
