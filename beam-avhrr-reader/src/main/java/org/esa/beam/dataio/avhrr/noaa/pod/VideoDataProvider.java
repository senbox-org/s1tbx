package org.esa.beam.dataio.avhrr.noaa.pod;

import com.bc.ceres.binio.SequenceData;

import java.io.IOException;

/**
 * @author Ralf Quast
 */
interface VideoDataProvider {

    SequenceData getVideoData(int recordIndex) throws IOException;
}
