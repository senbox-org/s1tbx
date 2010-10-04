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

package org.esa.beam.preprocessor.smilecorr;

import org.esa.beam.framework.gpf.Tile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SmileCorrectionAlgorithm {

    private final SmileCorrectionAuxdata auxdata;

    public SmileCorrectionAlgorithm(SmileCorrectionAuxdata auxdata) {
        this.auxdata = auxdata;
    }

    public double correct(int x, int y, int bandIndex, int detectorIndex, Tile[] radianceTiles, boolean isLand) {
        double originalValue = radianceTiles[bandIndex].getSampleDouble(x, y);
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
            double r1 = radianceTiles[lowerIndex].getSampleDouble(x, y) / detectorE0s[lowerIndex];
            double r2 = radianceTiles[upperIndex].getSampleDouble(x, y) / detectorE0s[upperIndex];
            double dl = (theoretWLs[bandIndex] - detectorWLs[bandIndex]) / (detectorWLs[upperIndex] - detectorWLs[lowerIndex]);
            double dr = (r2 - r1) * dl * theoretE0s[bandIndex];
            rc += dr;
        }
        return rc;
    }

    public int[] computeRequiredBandIndexes(int bandIndex) {
        final boolean[] landShouldCorrect = auxdata.getRadCorrFlagsLand();
        final int[] landIndexes1 = auxdata.getLowerBandIndexesLand();
        final int[] landIndexes2 = auxdata.getUpperBandIndexesLand();
        final boolean[] waterShouldCorrect = auxdata.getRadCorrFlagsWater();
        final int[] waterIndexes1 = auxdata.getLowerBandIndexesWater();
        final int[] waterIndexes2 = auxdata.getUpperBandIndexesWater();

        final Set<Integer> bandIndexSet = new HashSet<Integer>();

        bandIndexSet.add(bandIndex);
        if (landShouldCorrect[bandIndex]) {
            bandIndexSet.add(landIndexes1[bandIndex]);
            bandIndexSet.add(landIndexes2[bandIndex]);
        }
        if (waterShouldCorrect[bandIndex]) {
            bandIndexSet.add(waterIndexes1[bandIndex]);
            bandIndexSet.add(waterIndexes2[bandIndex]);
        }
        return intSetToSortedIntArray(bandIndexSet);
    }

    private static int[] intSetToSortedIntArray(final Set<Integer> set) {
        int[] bandIndexes = new int[set.size()];
        Integer[] a = new Integer[bandIndexes.length];
        a = set.toArray(a);
        for (int i = 0; i < a.length; i++) {
            bandIndexes[i] = a[i];
        }
        Arrays.sort(bandIndexes);
        return bandIndexes;
    }


}
