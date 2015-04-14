package org.esa.snap.csv.dataio.writer;

import java.io.IOException;

/**
 * Interface which hides different strategies to write csv output.
 *
 * @author Thomas Storm
 */
public interface WriteStrategy {

    /**
     * Writes the given output, which is expected to be complete and in the correct format.
     * @param fullOutput the output to write.
     */
    void writeCsv(String fullOutput) throws IOException;
    
}
