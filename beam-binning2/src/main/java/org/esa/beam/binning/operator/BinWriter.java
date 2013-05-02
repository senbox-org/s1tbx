package org.esa.beam.binning.operator;

import org.esa.beam.binning.TemporalBin;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Marco Peters
 */
public interface BinWriter {

    void write(Map<String, String> metadataProperties,
               List<TemporalBin> temporalBins) throws IOException, InvalidRangeException;

    void setTargetFileTemplatePath(String targetFileTemplatePath);

    String getTargetFilePath();

    void setLogger(Logger logger);
}
