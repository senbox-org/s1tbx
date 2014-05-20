package org.esa.beam.dataio.avhrr.noaa.pod;

import java.io.IOException;

/**
 * @author Ralf Quast
 */
interface Validator {

    boolean isValid(int i) throws IOException;
}
