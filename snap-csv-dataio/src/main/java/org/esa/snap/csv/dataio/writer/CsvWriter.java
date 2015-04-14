package org.esa.snap.csv.dataio.writer;

import java.io.IOException;

/**
 * Interface that allows writing supported input as CSV files.
 *
 * @author Thomas Storm
 */
public interface CsvWriter {

    /**
     * Writes the input as CSV, according to the rules given in the implementation.
     * @param input The sources to be written as csv.
     * @throws IOException if sources cannot be written.
     */
    void writeCsv(Object... input) throws IOException;

    /**
     * Checks if the given input can be written.
     * @param input The sources to be written as csv.
     * @return true if the given input can be written.
     */
    boolean isValidInput(Object... input);
    
}
