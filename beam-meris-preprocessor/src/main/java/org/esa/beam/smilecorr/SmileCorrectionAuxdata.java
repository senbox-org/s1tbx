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
package org.esa.beam.smilecorr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.util.ResourceInstaller;
import org.esa.beam.util.SystemUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.StringTokenizer;

/**
 * Provides auxiliary data for the Smile Correction Processor.
 * <p/>
 * This class also provides a flag for each of the 15 MERIS L1b bands determining whether or not to perform irradiance
 * plus radiance correction for each band and the lower and upper band indexes to be used for each band.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public class SmileCorrectionAuxdata {

    private static final String _BAND_INFO_FILENAME = "band_info.txt";
    private static final String _CENTRAL_WAVELEN_FR_FILENAME = "central_wavelen_fr.txt";
    private static final String _CENTRAL_WAVELEN_RR_FILENAME = "central_wavelen_rr.txt";
    private static final String _SUN_SPECTRAL_FLUX_FR_FILENAME = "sun_spectral_flux_fr.txt";
    private static final String _SUN_SPECTRAL_FLUX_RR_FILENAME = "sun_spectral_flux_rr.txt";
    private static final int _NUM_DETECTORS_FR = 3700;
    private static final int _NUM_DETECTORS_RR = 925;

    static final String AUXDATA_DIR_PROPERTY = "smile.auxdata.dir";

    private boolean[/*15*/] _radCorrFlagsLand;
    private int[/*15*/] _lowerBandIndexesLand;
    private int[/*15*/] _upperBandIndexesLand;
    private boolean[/*15*/] _radCorrFlagsWater;
    private int[/*15*/] _lowerBandIndexesWater;
    private int[/*15*/] _upperBandIndexesWater;
    private double[/*15*/] _theoreticalWavelengths;
    private double[/*15*/] _theoreticalSunSpectralFluxes;
    private double[][/*15*/] _detectorWavelengths;
    private double[][/*15*/] _detectorSunSpectralFluxes;
    private final File auxdataDir;

    private SmileCorrectionAuxdata(File auxdataDir,
                                   final String detectorWavelengthsFilename,
                                   final String detectorSunSpectralFluxesFilename,
                                   final int numRows,
                                   final int numCols) throws IOException {
        this.auxdataDir = auxdataDir;
        loadBandInfos();
        loadDetectorData(detectorWavelengthsFilename, detectorSunSpectralFluxesFilename, numRows, numCols);
    }

    public static SmileCorrectionAuxdata loadAuxdata(String productType) throws IOException {
        final File auxdataDir = installAuxdata();

        if (productType.startsWith("MER_F")) {
            return loadFRAuxdata(auxdataDir);
        } else if (productType.startsWith("MER_R")) {
            return loadRRAuxdata(auxdataDir);
        } else {
            throw new IOException(String.format("No auxillary data found for input product of type '%s'", productType));
        }
    }

    public boolean[/*15*/] getRadCorrFlagsWater() {
        return _radCorrFlagsWater;
    }

    public int[/*15*/] getLowerBandIndexesWater() {
        return _lowerBandIndexesWater;
    }

    public int[/*15*/] getUpperBandIndexesWater() {
        return _upperBandIndexesWater;
    }

    public boolean[/*15*/] getRadCorrFlagsLand() {
        return _radCorrFlagsLand;
    }

    public int[/*15*/] getLowerBandIndexesLand() {
        return _lowerBandIndexesLand;
    }

    public int[/*15*/] getUpperBandIndexesLand() {
        return _upperBandIndexesLand;
    }

    public double[] getTheoreticalWavelengths() {
        return _theoreticalWavelengths;
    }

    public double[] getTheoreticalSunSpectralFluxes() {
        return _theoreticalSunSpectralFluxes;
    }

    public double[][] getDetectorWavelengths() {
        return _detectorWavelengths;
    }

    public double[][] getDetectorSunSpectralFluxes() {
        return _detectorSunSpectralFluxes;
    }

    public static SmileCorrectionAuxdata loadRRAuxdata(File auxdataDir) throws IOException {
        return new SmileCorrectionAuxdata(auxdataDir,
                                          _CENTRAL_WAVELEN_RR_FILENAME,
                                          _SUN_SPECTRAL_FLUX_RR_FILENAME,
                                          _NUM_DETECTORS_RR,
                                          EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS);
    }

    public static SmileCorrectionAuxdata loadFRAuxdata(File auxdataDir) throws IOException {
        return new SmileCorrectionAuxdata(auxdataDir,
                                          _CENTRAL_WAVELEN_FR_FILENAME,
                                          _SUN_SPECTRAL_FLUX_FR_FILENAME,
                                          _NUM_DETECTORS_FR,
                                          EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS);
    }

    private void loadBandInfos() throws IOException {
        final double[][] table = loadFlatAuxDataFile(_BAND_INFO_FILENAME, 15, 8);
        final int n = EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS;
        _radCorrFlagsLand = new boolean[n];
        _lowerBandIndexesLand = new int[n];
        _upperBandIndexesLand = new int[n];
        _radCorrFlagsWater = new boolean[n];
        _lowerBandIndexesWater = new int[n];
        _upperBandIndexesWater = new int[n];
        _theoreticalWavelengths = new double[n];
        _theoreticalSunSpectralFluxes = new double[n];
        for (int i = 0; i < n; i++) {
            final double[] row = table[i];
            _radCorrFlagsLand[i] = row[0] != 0.0;
            _lowerBandIndexesLand[i] = (int) row[1] - 1;
            _upperBandIndexesLand[i] = (int) row[2] - 1;
            _radCorrFlagsWater[i] = row[3] != 0.0;
            _lowerBandIndexesWater[i] = (int) row[4] - 1;
            _upperBandIndexesWater[i] = (int) row[5] - 1;
            _theoreticalWavelengths[i] = row[6];
            _theoreticalSunSpectralFluxes[i] = row[7];
        }
    }

    private void loadDetectorData(final String detectorWavelengthsFilename,
                                  final String detectorSunSpectralFluxesFilename,
                                  final int numRows,
                                  final int numCols) throws IOException {
        _detectorWavelengths = loadFlatAuxDataFile(detectorWavelengthsFilename, numRows, numCols);
        _detectorSunSpectralFluxes = loadFlatAuxDataFile(detectorSunSpectralFluxesFilename, numRows, numCols);
    }

    private double[][] loadFlatAuxDataFile(final String auxFileName, final int numRows, final int numCols) throws
                                                                                                           IOException {
        BufferedReader reader = openFlatAuxDataFile(auxFileName);
        double[][] tableData = new double[numRows][numCols];
        IOException ioError = null;
        try {
            readFlatAuxDataFile(tableData, reader);
        } catch (IOException e) {
            ioError = e;
        } finally {
            reader.close();
        }
        if (ioError != null) {
            throw ioError;
        }
        return tableData;
    }

    private BufferedReader openFlatAuxDataFile(String fileName) throws IOException {
        assert fileName != null;
        assert fileName.length() > 0;
        return new BufferedReader(new FileReader(new File(auxdataDir, fileName)));
    }

    private static void readFlatAuxDataFile(double[][] xrWLs, BufferedReader reader) throws IOException {

        final int numRows = xrWLs.length;
        final int numCols = xrWLs[0].length;
        StringTokenizer st;
        String line;
        String token;
        int row = -1; // skip first row, it's always a header line
        int col;
        while ((line = reader.readLine()) != null) {
            if (row >= 0 && row < numRows) {
                st = new StringTokenizer(line, " \t", false);
                col = -1; // skip first column, it's always the band index
                while (st.hasMoreTokens()) {
                    token = st.nextToken();
                    if (col >= 0 && col < numCols) {
                        xrWLs[row][col] = Double.parseDouble(token);
                    }
                    col++;
                }
            }
            row++;
        }
    }

    private static File installAuxdata() throws IOException {
        File defaultAuxdataInstallDir = new File(SystemUtils.getApplicationDataDir(),
                                                 "beam-meris-preprocessor/smile-correction/auxdata");
        String auxdataDirPath = System.getProperty(AUXDATA_DIR_PROPERTY,
                                                   defaultAuxdataInstallDir.getAbsolutePath());
        File auxdataDirectory = new File(auxdataDirPath);

        URL sourceUrl = ResourceInstaller.getSourceUrl(SmileCorrectionAuxdata.class);
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceUrl, "auxdata/", auxdataDirectory);
        resourceInstaller.install(".*", ProgressMonitor.NULL);
        return auxdataDirectory;
    }
}
