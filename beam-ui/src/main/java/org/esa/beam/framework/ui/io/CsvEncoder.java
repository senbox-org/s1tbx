package org.esa.beam.framework.ui.io;

import java.io.IOException;
import java.io.Writer;

/**
 * An encoder for CSV.
 *
 * @author Norman Fomferra
 */
public interface CsvEncoder {
    void encodeCsv(Writer writer) throws IOException;
}
