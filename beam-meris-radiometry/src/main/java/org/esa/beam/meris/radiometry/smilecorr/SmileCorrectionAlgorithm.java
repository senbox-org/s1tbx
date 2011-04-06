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

package org.esa.beam.meris.radiometry.smilecorr;

/**
 * Applies a SMILE correction on the given MERIS L1b sample.
 */
public class SmileCorrectionAlgorithm {

    private final SmileCorrectionAuxdata auxdata;

    /**
     * Creates an instance of this class with the given auxiliary data.
     *
     * @param auxdata the auxiliary data
     */
    public SmileCorrectionAlgorithm(SmileCorrectionAuxdata auxdata) {
        this.auxdata = auxdata;
    }

    /**
     * Corrects the sample at a given index of the provided spectra.
     *
     * @param bandIndex       the index specifying the sample in the given spectra
     * @param detectorIndex   the detector index at the pixel location of the spectra.
     * @param radianceSamples the spectra
     * @param isLand          whether the spectra shall be treated as land or as water
     *
     * @return the corrected value
     */
    public double correct(int bandIndex, int detectorIndex, double[] radianceSamples, boolean isLand) {
        double originalValue = radianceSamples[bandIndex];
        if (detectorIndex < 0 || detectorIndex >= auxdata.getDetectorWavelengths().length) {
            return originalValue;
        }

        boolean[] shouldCorrect;
        int[] lowerIndexes;
        int[] upperIndexes;
        if (isLand) {
            shouldCorrect = auxdata.getRadCorrFlagsLand();
            lowerIndexes = auxdata.getLowerBandIndexesLand();
            upperIndexes = auxdata.getUpperBandIndexesLand();
        } else {
            shouldCorrect = auxdata.getRadCorrFlagsWater();
            lowerIndexes = auxdata.getLowerBandIndexesWater();
            upperIndexes = auxdata.getUpperBandIndexesWater();
        }
        double[] detectorE0s = auxdata.getDetectorSunSpectralFluxes()[detectorIndex];
        double[] detectorWLs = auxdata.getDetectorWavelengths()[detectorIndex];
        double[] theoretWLs = auxdata.getTheoreticalWavelengths();
        double[] theoretE0s = auxdata.getTheoreticalSunSpectralFluxes();

        // perform irradiance correction
        double r0 = originalValue / detectorE0s[bandIndex];
        double rc = r0 * theoretE0s[bandIndex];
        if (shouldCorrect[bandIndex]) {
            // perform reflectance correction
            int lowerIndex = lowerIndexes[bandIndex];
            int upperIndex = upperIndexes[bandIndex];
            double r1 = radianceSamples[lowerIndex] / detectorE0s[lowerIndex];
            double r2 = radianceSamples[upperIndex] / detectorE0s[upperIndex];
            double dl = (theoretWLs[bandIndex] - detectorWLs[bandIndex]) / (detectorWLs[upperIndex] - detectorWLs[lowerIndex]);
            double dr = (r2 - r1) * dl * theoretE0s[bandIndex];
            rc += dr;
        }
        return rc;
    }

}
