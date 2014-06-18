package org.esa.nest.eo;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.nest.datamodel.AbstractMetadata;

/**
 * SAR specific common functions
 */
public class SARUtils {
    /**
     * Get radar frequency from the abstracted metadata (in Hz).
     *
     * @param absRoot the AbstractMetadata
     * @return wavelength
     * @throws Exception The exceptions.
     */
    public static double getRadarFrequency(final MetadataElement absRoot) throws Exception {
        final double radarFreq = AbstractMetadata.getAttributeDouble(absRoot,
                AbstractMetadata.radar_frequency) * Constants.oneMillion; // Hz
        if (Double.compare(radarFreq, 0.0) <= 0) {
            throw new OperatorException("Invalid radar frequency: " + radarFreq);
        }
        return Constants.lightSpeed / radarFreq;
    }
}
