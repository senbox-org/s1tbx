package org.esa.beam.binning.operator;

import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.TemporalBin;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This interface can be implemented to write binned data.
 *
 * @author Marco Peters
 */
public interface BinWriter {

    void write(Map<String, String> metadataProperties, List<TemporalBin> temporalBins) throws IOException;

    void setBinningContext(BinningContext binningContext);

    void setTargetFileTemplatePath(String targetFileTemplatePath);

    String getTargetFilePath();

    void setLogger(Logger logger);
}
