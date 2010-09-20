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

package org.esa.beam;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.processor.smile.SmileAuxData;
import org.esa.beam.processor.smile.SmileConstants;
import org.esa.beam.util.ResourceInstaller;
import org.esa.beam.util.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class SmileAlgorithm {

    private final File auxdataDir;
    private final SmileAuxData auxData;

    SmileAlgorithm(String productType) throws IOException {
        auxdataDir = installAuxdata();
        auxData = loadAuxdata(productType);
    }

    private SmileAuxData loadAuxdata(String productType) throws IOException {
        if (productType.startsWith("MER_F")) {
            return SmileAuxData.loadFRAuxData(auxdataDir);
        } else if (productType.startsWith("MER_R")) {
            return SmileAuxData.loadRRAuxData(auxdataDir);
        } else {
            throw new IOException(String.format("No auxillary data found for input product of type '%s'", productType));
        }
    }

    private File installAuxdata() throws IOException {
        File defaultAuxdataInstallDir = new File(SystemUtils.getApplicationDataDir(), "beam-meris-smile/auxdata");
        String auxdataDirPath = System.getProperty(SmileConstants.AUXDATA_DIR_PROPERTY,
                                                   defaultAuxdataInstallDir.getAbsolutePath());
        File auxdataDir = new File(auxdataDirPath);

        URL sourceUrl = ResourceInstaller.getSourceUrl(SmileAuxData.class);
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceUrl, "auxdata/", auxdataDir);
        resourceInstaller.install(".*", ProgressMonitor.NULL);
        return auxdataDir;
    }

    int getMaxDetectorIndex() {
        return auxData.getDetectorWavelengths().length;
    }

    public double correct(int x, int y, int bandIndex, int detectorIndex, Tile[] radianceTiles, boolean isLand) {
        boolean[] shouldCorrect;
        int[] lowerIndexes;
        int[] upperIndexes;
        if (isLand) {
            shouldCorrect = auxData.getRadCorrFlagsLand();
            lowerIndexes = auxData.getLowerBandIndexesLand();
            upperIndexes = auxData.getUpperBandIndexesLand();
        } else {
            shouldCorrect = auxData.getRadCorrFlagsWater();
            lowerIndexes = auxData.getLowerBandIndexesWater();
            upperIndexes = auxData.getUpperBandIndexesWater();
        }
        double[] detectorE0s = auxData.getDetectorSunSpectralFluxes()[detectorIndex];
        double[] detectorWLs = auxData.getDetectorWavelengths()[detectorIndex];
        double[] theoretWLs = auxData.getTheoreticalWavelengths();
        double[] theoretE0s = auxData.getTheoreticalSunSpectralFluxes();

        // perform irradiance correction
        double r0 = radianceTiles[bandIndex].getSampleDouble(x, y) / detectorE0s[bandIndex];
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

    int[] computeRequiredBandIndexes(int bandIndex) {
        final boolean[] landShouldCorrect = auxData.getRadCorrFlagsLand();
        final int[] landIndexes1 = auxData.getLowerBandIndexesLand();
        final int[] landIndexes2 = auxData.getUpperBandIndexesLand();
        final boolean[] waterShouldCorrect = auxData.getRadCorrFlagsWater();
        final int[] waterIndexes1 = auxData.getLowerBandIndexesWater();
        final int[] waterIndexes2 = auxData.getUpperBandIndexesWater();

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
