/*
 * $id$
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.processor.smile;

import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.util.SystemUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
public class SmileAuxData {

    private static final String _BAND_INFO_FILENAME = "band_info.txt";
    private static final String _CENTRAL_WAVELEN_FR_FILENAME = "central_wavelen_fr.txt";
    private static final String _CENTRAL_WAVELEN_RR_FILENAME = "central_wavelen_rr.txt";
    private static final String _SUN_SPECTRAL_FLUX_FR_FILENAME = "sun_spectral_flux_fr.txt";
    private static final String _SUN_SPECTRAL_FLUX_RR_FILENAME = "sun_spectral_flux_rr.txt";
    private static final int _NUM_DETECTORS_FR = 3700;
    private static final int _NUM_DETECTORS_RR = 925;

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

    private static File getDefaultAuxdataDir() {
        String symbolicName = "beam-meris-smile"; // todo - get the symbolicName from processor
        File defaultAuxdataDir = new File(SystemUtils.getApplicationDataDir(), symbolicName + "/auxdata");
        String auxdataDirPath = System.getProperty(SmileConstants.AUXDATA_DIR_PROPERTY,
                                                   defaultAuxdataDir.getAbsolutePath());
        return new File(auxdataDirPath);
    }

    private SmileAuxData(File auxdataDir,
                         final String detectorWavelengthsFilename,
                         final String detectorSunSpectralFluxesFilename,
                         final int numRows,
                         final int numCols) throws IOException {
        this.auxdataDir = auxdataDir;
        loadBandInfos();
        loadDetectorData(detectorWavelengthsFilename, detectorSunSpectralFluxesFilename, numRows, numCols);
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

    /** @deprecated in 4.0, use {@link #loadRRAuxData(java.io.File)}
     */
    public static SmileAuxData loadRRAuxData() throws IOException {
        return loadRRAuxData(getDefaultAuxdataDir());
    }

    public static SmileAuxData loadRRAuxData(File auxdataDir) throws IOException {
        return new SmileAuxData(auxdataDir,
                                _CENTRAL_WAVELEN_RR_FILENAME,
                                _SUN_SPECTRAL_FLUX_RR_FILENAME,
                                _NUM_DETECTORS_RR,
                                EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS);
    }

    /** @deprecated in 4.0, use {@link #loadFRAuxData(java.io.File)}
     */
    public static SmileAuxData loadFRAuxData() throws IOException {
        return loadFRAuxData(getDefaultAuxdataDir());
    }

    public static SmileAuxData loadFRAuxData(File auxdataDir) throws IOException {
        return new SmileAuxData(auxdataDir,
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

    private  BufferedReader openFlatAuxDataFile(String fileName) throws IOException {
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

}
