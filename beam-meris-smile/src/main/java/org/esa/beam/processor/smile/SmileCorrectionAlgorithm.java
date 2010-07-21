/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.processor.smile;

/**
 * The implementation of the Smile Correction algorithm.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class SmileCorrectionAlgorithm {

    /**
     * Performs the smile correction for the reflectances of the 15 MERIS L1b bands.
     *
     * @param bandIndexes   the indexes of the bands to be corrected
     * @param shouldCorrect the flags for each band determining whether or not to perform irradiance + radiance
     *                      correction
     * @param indexes1      the indexes for each band of the lower band
     * @param indexes2      the indexes for each band of the upper band
     * @param radiances     the radiances for each band to be corrected
     * @param theoretWLs    the theoretical wavelengths for each band
     * @param theoretE0s    the theoretical sun spectral fluxes for each band
     * @param detectorWLs   the detector wavelengths for each band
     * @param detectorE0s   the sun spectral fluxes for the detector wavelengths for each band
     * @param corrRadiances the corrected radiances for each band
     */
    public static void computeSmileCorrectedRadiances(final int[/*<=15*/] bandIndexes,
                                                      final boolean[/*15*/] shouldCorrect,
                                                      final int[/*15*/] indexes1,
                                                      final int[/*15*/] indexes2,
                                                      final double[/*15*/] radiances,
                                                      final double[/*15*/] theoretWLs,
                                                      final double[/*15*/] theoretE0s,
                                                      final double[/*15*/] detectorWLs,
                                                      final double[/*15*/] detectorE0s,
                                                      double[/*15*/] corrRadiances) {
        double r0, r1, r2, rc, dl, dr;
        int i0, i1, i2;
        for (int bandIndexe : bandIndexes) {
            i0 = bandIndexe;
            // perform irradiance correction
            r0 = radiances[i0] / detectorE0s[i0];
            rc = r0 * theoretE0s[i0];
            if (shouldCorrect[i0]) {
                // perform reflectance correction
                i1 = indexes1[i0];
                i2 = indexes2[i0];
                r1 = radiances[i1] / detectorE0s[i1];
                r2 = radiances[i2] / detectorE0s[i2];
                dl = (theoretWLs[i0] - detectorWLs[i0]) / (detectorWLs[i2] - detectorWLs[i1]);
                dr = (r2 - r1) * dl * theoretE0s[i0];
                rc += dr;
            }
            corrRadiances[i0] = rc;
        }
    }
}
