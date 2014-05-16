package org.esa.beam.dataio.avhrr.noaa;

/**
 * @author Ralf Quast
 */
interface FormatDetector {

    boolean canDecode();

    void dispose();
}
