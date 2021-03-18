/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s1tbx.commons;

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
     * Replaced by getRadarWavelength()
     *
     * @param absRoot the AbstractMetadata
     * @return wavelength
     * @throws Exception The exceptions.
     */
    @Deprecated
    public static double getRadarFrequency(final MetadataElement absRoot) throws Exception {
        final double radarFreq = AbstractMetadata.getAttributeDouble(absRoot,
                AbstractMetadata.radar_frequency) * Constants.oneMillion; // Hz
        if (Double.compare(radarFreq, 0.0) <= 0) {
            throw new OperatorException("Invalid radar frequency: " + radarFreq);
        }
        return Constants.lightSpeed / radarFreq;
    }

    /**
     * Get radar wavelength from the abstracted metadata (in nm).
     *
     * @param absRoot the AbstractMetadata
     * @return wavelength
     * @throws Exception The exceptions.
     */
    public static double getRadarWavelength(final MetadataElement absRoot) throws Exception {
        final double radarFreq = AbstractMetadata.getAttributeDouble(absRoot,
                AbstractMetadata.radar_frequency) * Constants.oneMillion; // Hz
        if (Double.compare(radarFreq, 0.0) <= 0) {
            throw new OperatorException("Invalid radar frequency: " + radarFreq);
        }
        return Constants.lightSpeed / radarFreq;
    }
}
