package org.esa.s1tbx.insar.gpf.support;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.eo.Constants;

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
