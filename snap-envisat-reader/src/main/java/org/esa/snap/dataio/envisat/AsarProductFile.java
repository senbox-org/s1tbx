/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.dataio.envisat;

import com.bc.ceres.core.runtime.RuntimeContext;
import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.StringUtils;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;


/**
 * The <code>AsarProductFile</code> is a specialization of the abstract <code>ProductFile</code> class for ENVISAT
 * ASAR data products.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see org.esa.snap.dataio.envisat.ProductFile
 */
public final class AsarProductFile extends ProductFile {

    /**
     * Number of pixels in across-track direction
     */
    private int sceneRasterWidth;

    /**
     * Number of pixels in along-track direction
     */
    private int sceneRasterHeight;

    /**
     * The UTC time of the scene's first scan line
     */
    private ProductData.UTC sceneRasterStartTime;

    /**
     * The UTC time of the scene's last scan line
     */
    private ProductData.UTC sceneRasterStopTime;

    /**
     * X-offset in pixels of the first localisation tie point in the grid
     */
    private float locTiePointGridOffsetX;

    /**
     * Y-offset in pixels of the first localisation tie point in the grid
     */
    private float locTiePointGridOffsetY;

    /**
     * Number of columns per localisation tie-point in across-track direction
     */
    private float locTiePointSubSamplingX;

    /**
     * Number of lines per localisation tie-point in along-track direction
     */
    private float locTiePointSubSamplingY;

    /**
     * Whether the samples are in chronological order or not
     */
    private boolean chronologicalOrder;

    enum IODD {
        VERSION_UNKNOWN, ASAR_3K, ASAR_4A, ASAR_4B, ASAR_4C
    }

    /**
     * The IODD version number.
     */
    private IODD _ioddVersion = IODD.VERSION_UNKNOWN;

    /**
     * Product type suffix for IODD-3KB backward compatibility
     */
    private static final String IODD3K_SUFFIX = "_IODD_3K";
    /**
     * Product type suffix for IODD-4A backward compatibility
     */
    private static final String IODD4A_SUFFIX = "_IODD_4A";
    /**
     * Product type suffix for IODD-4B backward compatibility
     */
    private static final String IODD4B_SUFFIX = "_IODD_4B";
    /**
     * Product type suffix for IODD-4B backward compatibility
     */
    private static final String IODD4C_SUFFIX = "_IODD_4C";

    /**
     * The product type plus the IODD suffix
     */
    private String fullProductType = null;
    private String versionSuffix = null;

    /**
     * Constructs a <code>MerisProductFile</code> for the given seekable data input stream. Attaches the
     * <code>LogSink</code> passed in to the object created. The <code>LogSink</code> can might be null.
     *
     * @param file            the abstract file path representation.
     * @param dataInputStream the seekable data input stream which will be used to read data from the product file.
     * @throws java.io.IOException if an I/O error occurs
     */
    protected AsarProductFile(File file, ImageInputStream dataInputStream) throws IOException {
        super(file, dataInputStream);
    }

    /**
     * Gets the (sensing) start time associated with the first raster data line.
     *
     * @return the sensing start time, can be null e.g. for non-swath products
     */
    @Override
    public ProductData.UTC getSceneRasterStartTime() {
        return sceneRasterStartTime;
    }

    /**
     * Gets the (sensing) stop time associated with the first raster data line.
     *
     * @return the sensing stop time, can be null e.g. for non-swath products
     */
    @Override
    public ProductData.UTC getSceneRasterStopTime() {
        return sceneRasterStopTime;
    }

    /**
     * Overrides the base class method.
     *
     * @see org.esa.snap.dataio.envisat.ProductFile#getSceneRasterWidth()
     */
    @Override
    public int getSceneRasterWidth() {
        return sceneRasterWidth;
    }

    /**
     * Overrides the base class method.
     *
     * @see org.esa.snap.dataio.envisat.ProductFile#getSceneRasterHeight()
     */
    @Override
    public int getSceneRasterHeight() {
        return sceneRasterHeight;
    }

    /**
     * Overrides the base class method.
     */
    @Override
    public float getTiePointGridOffsetX(int gridWidth) {
        return locTiePointGridOffsetX;
    }

    /**
     * Overrides the base class method.
     */
    @Override
    public float getTiePointGridOffsetY(int gridWidth) {
        return locTiePointGridOffsetY;
    }

    /**
     * Overrides the base class method.
     *
     * @param gridWidth for AATSR products, this is the number of tie points in a tie point ADSR
     * @see org.esa.snap.dataio.envisat.ProductFile#getTiePointSubSamplingX(int)
     */
    @Override
    public float getTiePointSubSamplingX(int gridWidth) {
        return locTiePointSubSamplingX;
    }

    /**
     * Overrides the base class method.
     *
     * @param gridWidth for AATSR products, this is the number of tie points in a tie point ADSR
     * @see org.esa.snap.dataio.envisat.ProductFile#getTiePointSubSamplingY(int)
     */
    @Override
    public float getTiePointSubSamplingY(int gridWidth) {
        return locTiePointSubSamplingY;
    }

    /**
     * Determines whether the scan lines in this product data file have to be flipped before in "normal" view (pixel
     * numbers increase from west to east). <p>For MERIS products the method always returns true.
     */
    @Override
    public boolean storesPixelsInChronologicalOrder() {
        return chronologicalOrder;
    }

    /**
     * Returns the name of the GADS for this ENVISAT product file.
     *
     * @return the GADS name "VISIBLE_CALIB_COEFS_GADS", or <code>null</code> if this product file does not have a
     * GADS.
     */
    @Override
    public String getGADSName() {
        // @todo 1 nf/tb - check: are there really no GADS for any ASAR? If so, add to API doc. why null is returned
        return null;
    }

    /**
     * This method is called after the main product header has been read in successfully.
     * <p> Sub-classes should set product specific parameters in the <code>parameters</code> argument. The parameters
     * can be referenced in DDDB in order to implement dynamic field length, such as 'LINE_WIDTH'.
     * <p> When this method is called, the <code>getMPH()</code> method returns a non-null value.
     * <p> The default implementation is empty.
     *
     * @param parameters product specific parameters (possibly referenced within in the DDDB
     * @throws java.io.IOException if a header format error was detected or if an I/O error occurs
     */
    @Override
    protected void postProcessMPH(Map parameters) throws IOException {
        _ioddVersion = IODD.VERSION_UNKNOWN;
    }

    /**
     * This method is called after the main product header has been read in successfully.
     * <p> Sub-classes should set product specific parameters in the <code>parameters</code> argument. The parameters
     * can be referenced in DDDB in order to implement dynamic field length, such as 'LINE_WIDTH'.
     * <p> When this method is called, the <code>getMPH()</code> method returns a non-null value.
     * <p> The default implementation is empty.
     *
     * @param parameters product specific parameters (possibly referenced within in the DDDB
     */
    @Override
    protected void postProcessSPH(Map parameters) throws IOException {

        final DSD[] mdsDsds = getValidDSDs(EnvisatConstants.DS_TYPE_MEASUREMENT);
        if (mdsDsds.length == 0) {
            throw new IllegalFileFormatException("no valid measurements datasets found in this ASAR product");
        }

        setIODDVersion();

        final DSD dsdGeoLocationAds = getDSD("GEOLOCATION_GRID_ADS");
        if (dsdGeoLocationAds == null) {
            throw new IllegalFileFormatException("invalid product: missing DSD for dataset 'GEOLOCATION_GRID_ADS'"); /*I18N*/
        }

        sceneRasterHeight = mdsDsds[0].getNumRecords();
        final String productType = getProductType();
        boolean waveProduct = false;
        if (productType.equals("ASA_WVS_1P") || productType.equals("ASA_WVW_2P") || productType.equals("ASA_WVI_1P")) {
            waveProduct = true;
            final int numDirBins = getSPH().getParamInt("NUM_DIR_BINS");
            int numWlBins = getSPH().getParamInt("NUM_WL_BINS");
            if (productType.equals("ASA_WVS_1P") || productType.equals("ASA_WVI_1P"))
                numWlBins /= 2;                     // only 0 to 180 needed

            sceneRasterWidth = numDirBins * numWlBins;
            parameters.put("spectraWidth", sceneRasterWidth);

            if (productType.equals("ASA_WVI_1P")) {
                for (DSD dsd : mdsDsds) {
                    if (dsd.getNumRecords() > sceneRasterHeight)
                        sceneRasterHeight = dsd.getNumRecords();
                    if (dsd.getRecordSize() > sceneRasterWidth)
                        sceneRasterWidth = dsd.getRecordSize();
                }
            }

        } else {
            sceneRasterWidth = getSPH().getParamInt("LINE_LENGTH");
        }
        if (sceneRasterWidth < 0 && !waveProduct) {                          // handle WSS where LINE_LENGTH is -1

            int maxWidth = 0;
            final RecordReader recordReader = getRecordReader("MAIN_PROCESSING_PARAMS_ADS");
            for (int i = 0; i < mdsDsds.length; ++i) {

                final Record rec = recordReader.readRecord(i);

                final Field numSamplesPerLineField = rec.getField("num_samples_per_line");
                if (numSamplesPerLineField != null) {
                    final int rasterWidth = numSamplesPerLineField.getData().getElemInt();
                    parameters.put("mdsWidth" + (i + 1), rasterWidth);
                    if (rasterWidth > maxWidth) {
                        maxWidth = rasterWidth;
                    }
                }
            }
            sceneRasterWidth = maxWidth;
        }

        final int locTiePointGridWidth = EnvisatConstants.ASAR_LOC_TIE_POINT_GRID_WIDTH;
        final int locTiePointGridHeight = dsdGeoLocationAds.getNumRecords();

        locTiePointGridOffsetX = EnvisatConstants.ASAR_LOC_TIE_POINT_OFFSET_X;
        locTiePointGridOffsetY = EnvisatConstants.ASAR_LOC_TIE_POINT_OFFSET_Y;
        if (!waveProduct) {
            locTiePointSubSamplingX = (float) getPixelsPerTiePoint();
            locTiePointSubSamplingY = (float) getLinesPerTiePoint();
        } else {
            locTiePointSubSamplingX = (float) sceneRasterWidth / ((float) EnvisatConstants.ASAR_LOC_TIE_POINT_GRID_WIDTH - 1f);
            locTiePointSubSamplingY = (float) sceneRasterHeight / ((float) dsdGeoLocationAds.getNumRecords() - 1f);
        }

        // Note: the following parameters are NOT used in the DDDB anymore
        // They are provided here for debugging purposes only.
        //
        parameters.put("sceneRasterWidth", sceneRasterWidth);
        parameters.put("sceneRasterHeight", sceneRasterHeight);
        parameters.put("locTiePointGridWidth", locTiePointGridWidth);
        parameters.put("locTiePointGridHeight", locTiePointGridHeight);
        parameters.put("locTiePointGridOffsetX", locTiePointGridOffsetX);
        parameters.put("locTiePointGridOffsetY", locTiePointGridOffsetY);
        parameters.put("locTiePointSubSamplingX", locTiePointSubSamplingX);
        parameters.put("locTiePointSubSamplingY", locTiePointSubSamplingY);

        final String prod_descriptor = getSPH().getParamString("SPH_DESCRIPTOR");
        if (prod_descriptor != null) {
            chronologicalOrder = false;
            //if(prod_descriptor.contains("Geocoded"))
            //    chronologicalOrder = true;

            // don't flip - leave in satellite geometry
            //final String pass = getSPH().getParamString("PASS").trim();
            //if (pass.equals("ASCENDING")) {
            //    chronologicalOrder = false;
            //}
            if (productType.startsWith("SAR")) {   // ERS PGS
                chronologicalOrder = false;
            }
        }

        String firstMDSName = mdsDsds[0].getDatasetName();
        if (!isValidDatasetName(firstMDSName)) {
            firstMDSName = firstMDSName.replace(' ', '_');
        }
        if (!waveProduct) {
            try {
                sceneRasterStartTime = getSPH().getParamUTC("FIRST_LINE_TIME");
                sceneRasterStopTime = getSPH().getParamUTC("LAST_LINE_TIME");
            } catch (HeaderParseException e) {
                sceneRasterStartTime = getRecordTime(firstMDSName, "zero_doppler_time", 0);
                sceneRasterStopTime = getRecordTime(firstMDSName, "zero_doppler_time", sceneRasterHeight - 1);
            }
        }
    }

    private int getPixelsPerTiePoint() throws IOException {
        final RecordReader geoRecordReader = getRecordReader("GEOLOCATION_GRID_ADS");
        if (geoRecordReader == null) {
            return 0;
        }
        final Record rec = geoRecordReader.readRecord(0);
        final Field sampNumberField = rec.getField("ASAR_Geo_Grid_ADSR.sd/first_line_tie_points.samp_numbers");
        if (sampNumberField == null) {
            return 0;
        }
        return sampNumberField.getData().getElemIntAt(1) - 1;
    }

    private int getLinesPerTiePoint() throws IOException {
        final RecordReader geoRecordReader = getRecordReader("GEOLOCATION_GRID_ADS");
        if (geoRecordReader == null) {
            return 0;
        }
        final Record rec = geoRecordReader.readRecord(0);
        final Field numLinesField = rec.getField("num_lines");
        if (numLinesField == null) {
            return 0;
        }
        return numLinesField.getData().getElemInt();
    }

    @Override
    void setInvalidPixelExpression(Band band) {
        band.setNoDataValueUsed(true);
        band.setNoDataValue(0);
    }

    IODD getIODDVersion() {
        if (_ioddVersion == IODD.VERSION_UNKNOWN) {
            setIODDVersion();
        }
        return _ioddVersion;
    }

    /**
     * Sets the IODD version which is an indicator for the product format.
     * <p>
     * REF_DOC from version 3H on end with 3H, 3K, 4A, 4B
     * Software can be at least ASAR, NORUT, KSPT_L1B
     * <p>
     * 3H ASAR/3.05, ASAR/3.06, ASAR/3.08
     * 4A ASAR/4.01, ASAR/4.02, ASAR/4.04
     * 4B ASAR/4.05
     */
    private void setIODDVersion() {

        final Header mph = getMPH();
        try {

            final String refDoc = mph.getParamString("REF_DOC").toUpperCase().trim();
            if (refDoc.endsWith("4C") || refDoc.endsWith("4/C")) {
                _ioddVersion = IODD.ASAR_4C;
            } else if (refDoc.endsWith("4B") || refDoc.endsWith("4/B")) {
                _ioddVersion = IODD.ASAR_4B;
            } else if (refDoc.endsWith("4A") || refDoc.endsWith("4/A")) {
                _ioddVersion = IODD.ASAR_4A;
            } else if (refDoc.endsWith("3K") || refDoc.endsWith("3/K")) {
                _ioddVersion = IODD.ASAR_3K;
            } else {
                final char issueCh = refDoc.charAt(refDoc.length() - 2);
                if (Character.isDigit(issueCh)) {
                    final int issue = Character.getNumericValue(issueCh);
                    if (issue >= 4) {
                        _ioddVersion = IODD.ASAR_4B;                             // catch future versions
                    }
                }
            }

            // if version not found from doc_ref then look at the software version
            if (_ioddVersion == IODD.VERSION_UNKNOWN) {

                final String softwareVersion = mph.getParamString("SOFTWARE_VER").toUpperCase().trim();
                if (softwareVersion.startsWith("ASAR/3.")) {
                    final String versionStr = softwareVersion.substring(5);
                    if (StringUtils.isNumeric(versionStr, Float.class)) {
                        final float versionNum = Float.parseFloat(versionStr);
                        if (versionNum > 3.08) {
                            _ioddVersion = IODD.ASAR_3K;
                        }
                    }
                } else if (softwareVersion.startsWith("ASAR/4.05") || softwareVersion.contains("4.05")) {
                    _ioddVersion = IODD.ASAR_4B;
                } else if (softwareVersion.startsWith("ASAR/4.00") || softwareVersion.startsWith("ASAR/4.01") ||
                        softwareVersion.startsWith("ASAR/4.02") || softwareVersion.startsWith("ASAR/4.03") ||
                        softwareVersion.startsWith("ASAR/4.04") ||
                        softwareVersion.contains("4.00") || softwareVersion.contains("4.01") ||
                        softwareVersion.contains("4.02") || softwareVersion.contains("4.03") ||
                        softwareVersion.contains("4.04")) {
                    _ioddVersion = IODD.ASAR_4A;
                } else if (softwareVersion.startsWith("ASAR/4.05") || softwareVersion.startsWith("ASAR/4.06") ||
                        softwareVersion.startsWith("ASAR/4.07") ||
                        softwareVersion.contains("4.05") || softwareVersion.contains("4.06") ||
                        softwareVersion.contains("4.07")) {
                    _ioddVersion = IODD.ASAR_4B;
                } else if (softwareVersion.length() > 6) {
                    final char versionCh = softwareVersion.charAt(6);
                    if (Character.isDigit(versionCh)) {
                        final int versionNum = Character.getNumericValue(versionCh);
                        if (versionNum >= 4) {
                            _ioddVersion = IODD.ASAR_4B;
                        } else {
                            _ioddVersion = IODD.VERSION_UNKNOWN;
                        }
                    } else {
                        _ioddVersion = IODD.VERSION_UNKNOWN;
                    }
                } else {
                    _ioddVersion = IODD.VERSION_UNKNOWN;
                }
            }
        } catch (Exception e) {
            _ioddVersion = IODD.VERSION_UNKNOWN;
        }
    }

    /**
     * Gets the product type string as used within the DDDB, e.g. "MER_FR__1P_IODD5". This implementation considers
     * format changes in IODD 6.
     *
     * @return the product type string
     */
    @Override
    protected String getDddbProductType() {
        if (fullProductType == null) {
            fullProductType = getDddbProductTypeReplacement(getProductType(), getIODDVersion());
        }
        return fullProductType != null ? fullProductType : super.getDddbProductType();
    }

    static String getDddbProductTypeReplacement(final String productType, final IODD ioddVersion) {
        return productType + createVersionSuffix(productType, ioddVersion);
    }

    static String createVersionSuffix(final String productType, final IODD ioddVersion) {
        String suffix = "";
        if (ioddVersion == IODD.ASAR_3K) {
            if (productDDExists(productType + IODD3K_SUFFIX)) {
                suffix = IODD3K_SUFFIX;
            }
        } else if (ioddVersion == IODD.ASAR_4A) {
            if (productDDExists(productType + IODD4A_SUFFIX)) {
                suffix = IODD4A_SUFFIX;
            } else if (productDDExists(productType + IODD3K_SUFFIX)) {
                suffix = IODD3K_SUFFIX;
            }
        } else if (ioddVersion == IODD.ASAR_4B) {
            if (productDDExists(productType + IODD4B_SUFFIX)) {
                suffix = IODD4B_SUFFIX;
            } else if (productDDExists(productType + IODD4A_SUFFIX)) {
                suffix = IODD4A_SUFFIX;
            } else if (productDDExists(productType + IODD3K_SUFFIX)) {
                suffix = IODD3K_SUFFIX;
            }
        } else if (ioddVersion == IODD.ASAR_4C) {
            if (productDDExists(productType + IODD4C_SUFFIX)) {
                suffix = IODD4C_SUFFIX;
            } else if (productDDExists(productType + IODD4B_SUFFIX)) {
                suffix = IODD4B_SUFFIX;
            } else if (productDDExists(productType + IODD4A_SUFFIX)) {
                suffix = IODD4A_SUFFIX;
            } else if (productDDExists(productType + IODD3K_SUFFIX)) {
                suffix = IODD3K_SUFFIX;
            }
        } else if (ioddVersion == IODD.VERSION_UNKNOWN) {
            suffix = "";
        }
        return suffix;
    }

    String getVersionSuffix(final String productType, final IODD ioddVersion) {
        if (versionSuffix == null) {
            versionSuffix = createVersionSuffix(productType, ioddVersion);
        }
        return versionSuffix;
    }

    private static boolean productDDExists(String productType) {
        return DDDB.databaseResourceExists("products/" + productType + ".dd");
    }

    /**
     * Returns an array containing the center wavelengths for all bands in the AATSR product (in nm).
     */
    @Override
    public float[] getSpectralBandWavelengths() {
        return null;
    }

    /**
     * Returns an array containing the bandwidth for each band in nm.
     */
    @Override
    public float[] getSpectralBandBandwidths() {
        return null;
    }

    /**
     * Returns an array containing the solar spectral flux for each band.
     */
    @Override
    public float[] getSpectralBandSolarFluxes() {
        return null;
    }

    /**
     * Returns a new default set of mask definitions for this product file.
     *
     * @param dsName the name of the flag dataset
     * @return a new default set, an empty array if no default set is given for this product type, never
     * <code>null</code>.
     */
    @Override
    public Mask[] createDefaultMasks(String dsName) {
        return new Mask[0];
    }

    @Override
    protected int getDSRTimeInfoFieldIndex(RecordReader recordReader) {
        return recordReader.getRecordInfo().getFieldInfoIndex("zero_doppler_time");
    }

    @Override
    protected BandLineReader[] createBandLineReaders() {

        if (getProductType().equals("ASA_WVI_1P")) {
            return createWVIImagettes();
        }
        return DDDB.getInstance().getBandLineReaders(this);
    }

    private BandLineReader[] createWVIImagettes() {
        final BandLineReader[] dddbReaderList = DDDB.getInstance().getBandLineReaders(this);
        final ArrayList<BandLineReader> readerList = new ArrayList<BandLineReader>();
        readerList.addAll(Arrays.asList(dddbReaderList));

        try {
            final int numImagettes = getSPH().getParamInt("IMAGETTES_MADE");
            int bandDataType = DDDB.getFieldType("Float");

            String numStr;
            for (int i = 0; i < numImagettes; ++i) {

                if (i < 10) {
                    numStr = "00" + i;
                } else if (i < 100) {
                    numStr = "0" + i;
                } else {
                    numStr = "" + i;
                }
                final String pixelDataRefStr = "SLC_IMAGETTE_MDS_" + numStr + ".4";
                final FieldRef fieldRef = FieldRef.parse(pixelDataRefStr);
                final String dataSetName = fieldRef.getDatasetName();
                final int pixelDataFieldIndex = fieldRef.getFieldIndex();

                final String iBandName = "i_" + (i + 1);
                final BandInfo bandInfoI = createBandInfo(iBandName, bandDataType, -1,
                        BandInfo.SMODEL_1OF2, BandInfo.SCALE_LINEAR, 0.0, 1.0,
                        null, null, "real", "", dataSetName);

                final RecordReader pixelDataReaderI = getRecordReader(dataSetName);
                final BandLineReader bandLineReaderI = new BandLineReader(bandInfoI, pixelDataReaderI,
                        pixelDataFieldIndex);
                readerList.add(bandLineReaderI);

                final String qBandName = "q_" + (i + 1);
                final BandInfo bandInfoQ = createBandInfo(qBandName, bandDataType, -1,
                        BandInfo.SMODEL_2OF2, BandInfo.SCALE_LINEAR, 0.0, 1.0,
                        null, null, "imaginary", "", dataSetName);

                final RecordReader pixelDataReaderQ = getRecordReader(dataSetName);
                final BandLineReader bandLineReaderQ = new BandLineReader(bandInfoQ, pixelDataReaderQ,
                        pixelDataFieldIndex);
                readerList.add(bandLineReaderQ);

                final BandInfo bandInfoIntensity = createBandInfo("Intensity_" + (i + 1), bandDataType, -1,
                        BandInfo.SMODEL_1OF1, BandInfo.SCALE_LINEAR, 0.0, 1.0,
                        null, null, "intensity", "", dataSetName);

                final String expression = iBandName + '*' + iBandName + '+' + qBandName + '*' + qBandName;
                final BandLineReader bandLineReaderIntensity = new BandLineReader.Virtual(bandInfoIntensity,
                        updateExpression(expression));
                readerList.add(bandLineReaderIntensity);

                final BandInfo bandInfoPhase = createBandInfo("Phase_" + (i + 1), bandDataType, -1,
                        BandInfo.SMODEL_1OF1, BandInfo.SCALE_LINEAR, 0.0, 1.0,
                        null, null, "phase", "", dataSetName);

                final String expressionPhase = "atan2(" + qBandName + "," + iBandName + ")";
                final BandLineReader bandLineReaderPhase = new BandLineReader.Virtual(bandInfoPhase, updateExpression(
                        expressionPhase));
                readerList.add(bandLineReaderPhase);
            }
        } catch (Exception e) {
            Debug.trace(e.getMessage());
            //continue
        }
        return readerList.toArray(new BandLineReader[readerList.size()]);
    }

    /**
     * This method just delegates to
     * {@link BandInfo#BandInfo(String, int, int, int, int, double, double, String, FlagCoding, String, String, int, int)} to
     * create a new <code>BandInfo</code>.
     *
     * @param bandName          the name of the band.
     * @param dataType          the type of the data.
     * @param spectralBandIndex the spectral band index.
     * @param sampleModel       the sample model.
     * @param scalingMethod     the scaling mehtod.
     * @param scalingOffset     the scaling offset.
     * @param scalingFactor     the scaling factor.
     * @param validExpression   the valid expression.
     * @param flagCoding        the flag codeing.
     * @param physicalUnit      the physical unit.
     * @param description       the description.
     * @param dataSetName       the name of the dataset
     * @return a newly created <code>BandInfo</code> object.
     */
    @Override
    public BandInfo createBandInfo(String bandName,
                                   int dataType,
                                   int spectralBandIndex,
                                   int sampleModel,
                                   int scalingMethod,
                                   double scalingOffset,
                                   double scalingFactor,
                                   String validExpression,
                                   FlagCoding flagCoding,
                                   String physicalUnit,
                                   String description,
                                   String dataSetName) {

        int rasterHeight = sceneRasterHeight;
        int rasterWidth = sceneRasterWidth;

        final String productType = getProductType();
        if (productType.equals("ASA_WSS_1P")) {

            try {
                final DSD[] mdsDsds = getValidDSDs(EnvisatConstants.DS_TYPE_MEASUREMENT);
                for (int i = 0; i < mdsDsds.length; ++i) {

                    if (mdsDsds[i].getDatasetName().equals(dataSetName)) {
                        final RecordReader recordReader = getRecordReader("MAIN_PROCESSING_PARAMS_ADS");
                        final Record rec = recordReader.readRecord(i);

                        final Field numOutputLinesField = rec.getField("num_output_lines");
                        if (numOutputLinesField != null) {
                            rasterHeight = numOutputLinesField.getData().getElemInt();
                        }

                        final Field numSamplesPerLineField = rec.getField("num_samples_per_line");
                        if (numSamplesPerLineField != null) {
                            rasterWidth = numSamplesPerLineField.getData().getElemInt();
                        }

                        break;
                    }
                }

            } catch (IOException e) {
                // use defaults
            }
        } else if (productType.equals("ASA_WVI_1P") || productType.equals("ASA_WVS_1P") || productType.equals(
                "ASA_WVW_2P")) {
            final DSD dsd = getDSD(dataSetName);
            rasterHeight = dsd.getNumRecords();

            boolean error = false;
            try {     // width for cross and wave spectra
                final int numDirBins = getSPH().getParamInt("NUM_DIR_BINS");
                int numWlBins = getSPH().getParamInt("NUM_WL_BINS");
                if (productType.equals("ASA_WVS_1P") || productType.equals("ASA_WVI_1P")) {
                    numWlBins /= 2;                     // only 0 to 180 needed
                }

                rasterWidth = numDirBins * numWlBins;
            } catch (Exception e) {
                Debug.trace(e);
                error = true;
            }

            // width for imagettes
            if (error || (productType.equals("ASA_WVI_1P") && dataSetName.contains("IMAGE"))) {

                final int headerSize = 12 + 1 + 4;
                rasterWidth = (dsd.getRecordSize() - headerSize) / 4; // for complex each band is half
            }
        } else {

            if (bandName.endsWith("_1")) {
                bandName = renameWithPolarization(bandName, "_1", "MDS1_TX_RX_POLAR");
            } else if (bandName.endsWith("_2")) {
                bandName = renameWithPolarization(bandName, "_2", "MDS2_TX_RX_POLAR");
            }
        }

        return new BandInfo(bandName,
                dataType,
                spectralBandIndex,
                sampleModel,
                scalingMethod,
                scalingOffset,
                scalingFactor,
                validExpression,
                flagCoding,
                physicalUnit,
                description,
                rasterWidth,
                rasterHeight);
    }

    /**
     * Modifies the expression of a band if for example a band is renamed
     *
     * @param expression virtual band expression
     * @return the new expression
     */
    @Override
    public String updateExpression(String expression) {
        try {
            if (expression != null && !getProductType().equals("ASA_WSS_1P")) {

                String polarization1 = getSPH().getParamString("MDS1_TX_RX_POLAR");
                if (polarization1 != null && !polarization1.isEmpty()) {
                    polarization1 = polarization1.replace("/", "");
                    expression = expression.replaceAll("_1", "_" + polarization1);
                }

                String polarization2 = getSPH().getParamString("MDS2_TX_RX_POLAR");
                if (polarization2 != null && !polarization2.isEmpty()) {
                    polarization2 = polarization2.replace("/", "");
                    expression = expression.replaceAll("_2", "_" + polarization2);
                }
            }
        } catch (HeaderEntryNotFoundException e) {
            // use defaults
        }
        return expression;
    }

    private String renameWithPolarization(String bandName, String ending, String tag) {

        try {
            String polarization = getSPH().getParamString(tag);
            if (polarization != null && !polarization.isEmpty()) {
                polarization = polarization.replace("/", "");
                bandName = bandName.substring(0, bandName.length() - ending.length()) + '_' + polarization;
            }
        } catch (HeaderEntryNotFoundException e) {
            // use defaults
        }
        return bandName;
    }

    private void processWSSImageRecordMetadata(Product product) {

        for (final Band band : product.getBands()) {

            if (band.getUnit().equals("imaginary"))
                continue;

            MetadataElement imgRecElem = product.getMetadataRoot().getElement("Image Record");
            if (imgRecElem == null) {
                imgRecElem = new MetadataElement("Image Record");
                product.getMetadataRoot().addElement(imgRecElem);
            }

            MetadataElement bandElem = imgRecElem.getElement(band.getName());
            if (bandElem == null) {
                bandElem = new MetadataElement(band.getName());
                imgRecElem.addElement(bandElem);
            }
            final MetadataElement bandElemement = bandElem;

            FieldInfo fInfo = new FieldInfo("t", ProductData.TYPE_UTC, 1, "", "");
            Field field = fInfo.createField();

            try {
                final BandLineReader bandLineReader = getBandLineReader(band);
                final RecordReader recReader = bandLineReader.getPixelDataReader();
                final ImageInputStream istream = getDataInputStream();

                final long datasetOffset = recReader.getDSD().getDatasetOffset();
                final long recordSize = recReader.getDSD().getRecordSize();

                final int height = band.getRasterHeight();
                final double[] timeData = new double[height];
                long pos = datasetOffset;
                for (int y = 0; y < height; ++y) {

                    istream.seek(pos);
                    field.readFrom(istream);
                    ProductData data = field.getData();
                    if (data.getElemIntAt(0) == 0)
                        timeData[y] = 0;
                    else
                        timeData[y] = ((ProductData.UTC) data).getMJD();
                    pos += recordSize;
                }

                final MetadataAttribute attribute = new MetadataAttribute("t", ProductData.TYPE_FLOAT64, height);
                attribute.setDataElems(timeData);
                bandElemement.addAttribute(attribute);

            } catch (IOException e) {
                System.out.println("processWSSImageRecordMetadata " + e.toString());
            }
        }
    }

    private void processWaveMetadata(Product product) throws IOException {

        final MetadataElement origRoot = product.getMetadataRoot();
        final String[] datasetNames = getValidDatasetNames();
        for (String datasetName : datasetNames) {
            if (datasetName.equalsIgnoreCase("CROSS_SPECTRA_MDS") || datasetName.equalsIgnoreCase("OCEAN_WAVE_SPECTRA_MDS")) {
                final RecordReader recordReader = getRecordReader(datasetName);
                final MetadataElement metadataTableGroup = new MetadataElement(datasetName);
                final StringBuilder sb = new StringBuilder(25);
                for (int i = 0; i < recordReader.getNumRecords(); i++) {
                    final Record record = recordReader.readRecord(i);
                    sb.setLength(0);
                    sb.append(datasetName);
                    sb.append('.');
                    sb.append(i + 1);

                    final MetadataElement elem = new MetadataElement(sb.toString());

                    for (int j = 0; j < record.getNumFields(); j++) {
                        final Field field = record.getFieldAt(j);
                        if (field.getName().equals("ocean_spectra") || field.getName().equals("real_spectra"))
                            break;

                        final String description = field.getInfo().getDescription();
                        if (description != null) {
                            if (description.equalsIgnoreCase("Spare")) {
                                continue;
                            }
                        }

                        final MetadataAttribute attribute = new MetadataAttribute(field.getName(), field.getData(), true);
                        if (field.getInfo().getPhysicalUnit() != null) {
                            attribute.setUnit(field.getInfo().getPhysicalUnit());
                        }
                        if (description != null) {
                            attribute.setDescription(field.getInfo().getDescription());
                        }
                        elem.addAttribute(attribute);
                    }
                    metadataTableGroup.addElement(elem);
                }
                origRoot.addElement(metadataTableGroup);
            }
        }
    }

    /**
     * Allow the productFile to add any other metadata not defined in dddb
     *
     * @param product the product
     * @throws IOException if reading from files
     */
    @Override
    protected void addCustomMetadata(Product product) throws IOException {

        // wss metadata preprocesing to retrieve image record times
        final String productType = getProductType();
        if (productType.equalsIgnoreCase("ASA_WSS_1P")) {
            processWSSImageRecordMetadata(product);
        } else if (productType.equals("ASA_WVI_1P") || productType.equals("ASA_WVS_1P") || productType.equals(
                "ASA_WVW_2P")) {
            processWaveMetadata(product);
        }

        // set quicklook image
        for (Band b : product.getBands()) {
            if (b.getUnit() != null && b.getUnit().contains("intensity")) {
                product.setQuicklookBandName(b.getName());
                break;
            }
        }

        // Abstracted metadata
        final MetadataElement root = product.getMetadataRoot();
        final AsarAbstractMetadata absMetadata = new AsarAbstractMetadata(getProductType(),
                getVersionSuffix(getProductType(),
                        getIODDVersion()),
                getFile());
        absMetadata.addAbstractedMetadataHeader(product, root);

        discardUnusedMetadata(product);
    }

    private static void discardUnusedMetadata(final Product product) {
        if (RuntimeContext.getModuleContext() != null) {
            final String dicardUnusedMetadata = RuntimeContext.getModuleContext().getRuntimeConfig().
                    getContextProperty("discard.unused.metadata");
            if ("true".equalsIgnoreCase(dicardUnusedMetadata)) {
                removeUnusedMetadata(product.getMetadataRoot());
            }
        }
    }

    private static String[] elemsToKeep = {"Abstracted_Metadata", "MAIN_PROCESSING_PARAMS_ADS", "DSD", "SPH"};

    private static void removeUnusedMetadata(final MetadataElement root) {
        final MetadataElement[] elems = root.getElements();
        for (MetadataElement elem : elems) {
            final String name = elem.getName();
            boolean keep = false;
            for (String toKeep : elemsToKeep) {
                if (name.equals(toKeep)) {
                    keep = true;
                    break;
                }
            }
            if (!keep) {
                root.removeElement(elem);
                elem.dispose();
            }
        }
    }
}
