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

import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.util.StringUtils;

import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;


/**
 * The {@code AatsrProductFile} is a specialization of the abstract {@code ProductFile} class for ENVISAT
 * AATSR data products.
 *
 * @author Norman Fomferra
 * @see org.esa.snap.dataio.envisat.MerisProductFile
 */
public class AatsrProductFile extends ProductFile {

    private static final String FIRST_LINE_TIME = "FIRST_LINE_TIME";
    private static final String LAST_LINE_TIME = "LAST_LINE_TIME";

    /**
     * Number of pixels in along-track direction
     */
    private int _sceneRasterHeight;

    /**
     * X-offset in pixels of the first localisation tie point in the grid
     */
    private float _locTiePointGridOffsetX;

    /**
     * Y-offset in pixels of the first localisation tie point in the grid
     */
    private float _locTiePointGridOffsetY;

    /**
     * Number of columns per localisation tie-point in across-track direction
     */
    private int _locTiePointSubSamplingX;

    /**
     * Number of lines per localisation tie-point in along-track direction
     */
    private int _locTiePointSubSamplingY;

    /**
     * Number of solar angles tie-points in across-track direction
     */
    private int _solTiePointGridWidth;

    /**
     * X-offset in pixels of the first solar angles tie point in the grid
     */
    private float _solTiePointGridOffsetX;

    /**
     * Y-offset in pixels of the first solar angles tie point in the grid
     */
    private float _solTiePointGridOffsetY;

    /**
     * Number of columns per solar angles tie-point in across-track direction
     */
    private int _solTiePointSubSamplingX;

    /**
     * Number of lines per solar angles tie-point in along-track direction
     */
    private int _solTiePointSubSamplingY;
    private int[] mdsMapIndex;


    /**
     * Constructs a {@code MerisProductFile} for the given seekable data input stream.
     *
     * @param file            the abstract file path representation.
     * @param dataInputStream the seekable data input stream which will be used to read data from the product file.
     * @throws java.io.IOException if an I/O error occurs
     */
    protected AatsrProductFile(File file, ImageInputStream dataInputStream) throws IOException {
        super(file, dataInputStream);
    }

    /**
     * Gets the (sensing) start time associated with the first raster data line.
     *
     * @return the sensing start time, can be null e.g. for non-swath products
     */
    @Override
    public ProductData.UTC getSceneRasterStartTime() {
        try {
            return getSPH().getParamUTC(FIRST_LINE_TIME);
        } catch (HeaderParseException | HeaderEntryNotFoundException e) {
            getLogger().warning(
                    MessageFormat.format("failed to parse specific header parameter ''{0}'': {1}", FIRST_LINE_TIME,
                                         e.getMessage()));
            return null;
        }
    }

    /**
     * Gets the (sensing) stop time associated with the first raster data line.
     *
     * @return the sensing stop time, can be null e.g. for non-swath products
     */
    @Override
    public ProductData.UTC getSceneRasterStopTime() {
        try {
            return getSPH().getParamUTC(LAST_LINE_TIME);
        } catch (HeaderParseException | HeaderEntryNotFoundException e) {
            getLogger().warning(
                    MessageFormat.format("failed to parse specific header parameter ''{0}'': {1}", LAST_LINE_TIME,
                                         e.getMessage()));
            return null;
        }
    }

    /**
     * Overrides the base class method.
     *
     * @see org.esa.snap.dataio.envisat.ProductFile#getSceneRasterWidth()
     */
    @Override
    public int getSceneRasterWidth() {
        return EnvisatConstants.AATSR_SCENE_RASTER_WIDTH;
    }

    /**
     * Overrides the base class method.
     *
     * @see org.esa.snap.dataio.envisat.ProductFile#getSceneRasterHeight()
     */
    @Override
    public int getSceneRasterHeight() {
        return _sceneRasterHeight;
    }

    /**
     * Overrides the base class method.
     */
    @Override
    public float getTiePointGridOffsetX(int gridWidth) {
        return gridWidth == _solTiePointGridWidth ? _solTiePointGridOffsetX : _locTiePointGridOffsetX;
    }

    /**
     * Overrides the base class method.
     */
    @Override
    public float getTiePointGridOffsetY(int gridWidth) {
        return gridWidth == _solTiePointGridWidth ? _solTiePointGridOffsetY : _locTiePointGridOffsetY;
    }

    /**
     * Overrides the base class method.
     *
     * @param gridWidth for AATSR products, this is the number of tie points in a tie point ADSR
     * @see org.esa.snap.dataio.envisat.ProductFile#getTiePointSubSamplingX(int)
     */
    @Override
    public float getTiePointSubSamplingX(int gridWidth) {
        return gridWidth == _solTiePointGridWidth ? _solTiePointSubSamplingX : _locTiePointSubSamplingX;
    }

    /**
     * Overrides the base class method.
     *
     * @param gridWidth for AATSR products, this is the number of tie points in a tie point ADSR
     * @see org.esa.snap.dataio.envisat.ProductFile#getTiePointSubSamplingY(int)
     */
    @Override
    public float getTiePointSubSamplingY(int gridWidth) {
        return gridWidth == _solTiePointGridWidth ? _solTiePointSubSamplingY : _locTiePointSubSamplingY;
    }

    /**
     * Determines whether the scan lines in this product data file have to be flipped before in "normal" view (pixel
     * numbers increase from west to east). <p>For MERIS products the method always returns true.
     */
    @Override
    public boolean storesPixelsInChronologicalOrder() {
        return true;
    }

    /**
     * Returns the name of the GADS for this ENVISAT product file.
     *
     * @return the GADS name "VISIBLE_CALIB_COEFS_GADS", or {@code null} if this product file does not have a
     *         GADS.
     */
    @Override
    public String getGADSName() {
        return getProductType().equalsIgnoreCase(EnvisatConstants.AATSR_L1B_TOA_PRODUCT_TYPE_NAME)
                ? EnvisatConstants.AATSR_L1B_GADS_NAME
                : null;
    }

    /**
     * This method is called after the main product header has been read in successfully.
     * <p> Sub-classes should set product specific parameters in the {@code parameters} argument. The parameters
     * can be referenced in DDDB in order to implement dynamic field length, such as 'LINE_WIDTH'.
     * <p> When this method is called, the {@code getMPH()} method returns a non-null value.
     * <p> The default implementation is empty.
     *
     * @param parameters product specific parameters (possibly referenced within in the DDDB
     */
    @Override
    protected void postProcessSPH(Map parameters) throws IOException {
        DSD[] mdsDsds = getValidDSDs(EnvisatConstants.DS_TYPE_MEASUREMENT);
        int numMDSR;
        if (mdsDsds.length == 0) {
            //throw new ProductIOException("no valid measurements datasets found in this AATSR product"); /*I18N*/
            numMDSR = 0;
        } else {
            numMDSR = mdsDsds[0].getNumRecords();
        }
        DSD dsdGeoLocationAds = getDSD("GEOLOCATION_ADS");
        if (dsdGeoLocationAds == null) {
            throw new IllegalFileFormatException("invalid product: missing DSD for dataset 'GEOLOCATION_ADS'"); /*I18N*/
        }
        DSD dsdNadirViewSolarAnglesAds = getDSD("NADIR_VIEW_SOLAR_ANGLES_ADS");
        if (dsdNadirViewSolarAnglesAds == null) {
            throw new IllegalFileFormatException(
                    "invalid product: missing DSD for dataset 'NADIR_VIEW_SOLAR_ANGLES_ADS'"); /*I18N*/
        }

        _sceneRasterHeight = calculateSceneRasterHeight(dsdGeoLocationAds, numMDSR);
        int sceneRasterWidth = EnvisatConstants.AATSR_SCENE_RASTER_WIDTH;

        int locTiePointGridWidth = EnvisatConstants.AATSR_LOC_TIE_POINT_GRID_WIDTH;
        int locTiePointGridHeight = dsdGeoLocationAds.getNumRecords();
        _locTiePointGridOffsetX = EnvisatConstants.AATSR_LOC_TIE_POINT_OFFSET_X;
        _locTiePointGridOffsetY = EnvisatConstants.AATSR_TIE_POINT_OFFSET_Y;
        _locTiePointSubSamplingX = EnvisatConstants.AATSR_LOC_TIE_POINT_SUBSAMPLING_X;
        _locTiePointSubSamplingY = EnvisatConstants.AATSR_LOC_TIE_POINT_SUBSAMPLING_Y;

        _solTiePointGridWidth = EnvisatConstants.AATSR_SOL_TIE_POINT_GRID_WIDTH;
        int solTiePointGridHeight = dsdNadirViewSolarAnglesAds.getNumRecords();
        _solTiePointGridOffsetX = EnvisatConstants.AATSR_SOL_TIE_POINT_OFFSET_X;
        _solTiePointGridOffsetY = EnvisatConstants.AATSR_TIE_POINT_OFFSET_Y;
        _solTiePointSubSamplingX = EnvisatConstants.AATSR_SOL_TIE_POINT_SUBSAMPLING_X;
        _solTiePointSubSamplingY = EnvisatConstants.AATSR_SOL_TIE_POINT_SUBSAMPLING_Y;

//        _locTiePointSubSamplingX = sceneRasterWidth / (locTiePointGridWidth - 1);
//        _locTiePointSubSamplingY = sceneRasterHeight / (locTiePointGridHeight - 1);
//        _solTiePointSubSamplingX = sceneRasterWidth / (_solTiePointGridWidth - 1);
//        _solTiePointSubSamplingY = sceneRasterHeight / (solTiePointGridHeight - 1);

        // Note: the following parameters are NOT used in the DDDB anymore
        // They are provided here for debugging purposes only.
        //
        parameters.put("sceneRasterWidth", sceneRasterWidth);
        parameters.put("sceneRasterHeight", _sceneRasterHeight);
        parameters.put("locTiePointGridWidth", locTiePointGridWidth);
        parameters.put("locTiePointGridHeight", locTiePointGridHeight);
        parameters.put("locTiePointGridOffsetX", _locTiePointGridOffsetX);
        parameters.put("locTiePointGridOffsetY", _locTiePointGridOffsetY);
        parameters.put("locTiePointSubSamplingX", _locTiePointSubSamplingX);
        parameters.put("locTiePointSubSamplingY", _locTiePointSubSamplingY);
        parameters.put("solTiePointGridWidth", _solTiePointGridWidth);
        parameters.put("solTiePointGridHeight", solTiePointGridHeight);
        parameters.put("solTiePointGridOffsetX", _solTiePointGridOffsetX);
        parameters.put("solTiePointGridOffsetY", _solTiePointGridOffsetY);
        parameters.put("solTiePointSubSamplingX", _solTiePointSubSamplingX);
        parameters.put("solTiePointSubSamplingY", _solTiePointSubSamplingY);


        if (_sceneRasterHeight > numMDSR) {
            mdsMapIndex = new int[getSceneRasterHeight()];
            final RecordReader recordReader = getRecordReader("GEOLOCATION_ADS");
            final int records = recordReader.getNumRecords() - 1;
            int mdsIndex = 0;
            for (int geoADSIndex = 0; geoADSIndex < records; geoADSIndex++) {
                final Record record = recordReader.readRecord(geoADSIndex);
                final int attachFlag = record.getField("attach_flag").getElemInt(0);
                for (int line = 0; line < 32; line++) {
                    final int i = geoADSIndex * 32 + line;
                    if (attachFlag == 0) {
                        mdsMapIndex[i] = mdsIndex;
                        ++mdsIndex;
                    } else {
                        mdsMapIndex[i] = -1;
                    }
                }
            }
        }
    }

    @Override
    int getMappedMDSRIndex(int lineIndex) {
        if (mdsMapIndex == null) {
            return lineIndex;
        }
        return mdsMapIndex[lineIndex];
    }

    @Override
    double getMissingMDSRPixelValue() {
        return -2.0;
    }

    @Override
    void setInvalidPixelExpression(Band band) {
        if (band.isFlagBand()) {
            band.setNoDataValueUsed(false);
        } else {
            band.setNoDataValueUsed(true);
            band.setNoDataValue(-2);
        }
    }

    @Override
    public String getAutoGroupingPattern() {
        return "nadir:fward";
    }

    /**
     * Override because of the AATSR attachment flag treating. Valis dsds might have zero MDSR's attached because the
     * attachment_flag is rised for the whole (subset) product.
     *
     * @param datasetType the desired dataset type
     * @return all valis dsds conforming to the dataset type
     */
    @Override
    public DSD[] getValidDSDs(char datasetType) {
        ArrayList<DSD> dsdList = new ArrayList<>();
        for (int i = 0; i < getNumDSDs(); i++) {
            final DSD dsd = getDSDAt(i);
            if (dsd.getDatasetType() == datasetType && !StringUtils.isNullOrEmpty(dsd.getDatasetName())) {
                dsdList.add(dsd);
            }
        }

        return dsdList.toArray(new DSD[dsdList.size()]);
    }

    static int calculateSceneRasterHeight(DSD dsdGeoLocationAds, int numMDSR) {
        final int linesFromADS = (dsdGeoLocationAds.getNumRecords() - 1) * EnvisatConstants.AATSR_LOC_TIE_POINT_SUBSAMPLING_Y;
        if (numMDSR > linesFromADS) {
            return numMDSR;
        }
        return linesFromADS;
    }

    /**
     * Returns an array containing the center wavelengths for all bands in the AATSR product (in nm).
     */
    @Override
    public float[] getSpectralBandWavelengths() {
        return EnvisatConstants.AATSR_WAVELENGTHS;
    }

    /**
     * Returns an array containing the bandwidth for each band in nm.
     */
    @Override
    public float[] getSpectralBandBandwidths() {
        return EnvisatConstants.AATSR_BANDWIDTHS;
    }

    /**
     * Returns an array containing the solar spectral flux for each band.
     */
    @Override
    public float[] getSpectralBandSolarFluxes() {
        return EnvisatConstants.AATSR_SOLAR_FLUXES;
    }

    @Override
    protected void addCustomMetadata(Product product) throws IOException {
        // add bitmasks for ATSR active fires, see http://dup.esrin.esa.it/ionia/wfa/algorithm.asp
        final String nadirBand = EnvisatConstants.AATSR_L1B_BTEMP_NADIR_0370_BAND_NAME;
        final String fwardBand = EnvisatConstants.AATSR_L1B_BTEMP_FWARD_0370_BAND_NAME;

        ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
        if (product.containsBand(nadirBand)) {
            maskGroup.add(mask("fire_nadir_1", "ATSR active fire (ALGO1)", nadirBand + " > 312.0", Color.RED, 0.5f));
            maskGroup.add(mask("fire_nadir_2", "ATSR active fire (ALGO2)", nadirBand + " > 308.0", Color.RED.darker(), 0.5f));
        }
        if (product.containsBand(fwardBand)) {
            maskGroup.add(mask("fire_fward_1", "ATSR active fire (ALGO1)", fwardBand + " > 312.0", Color.RED, 0.5f));
            maskGroup.add(mask("fire_fward_2", "ATSR active fire (ALGO2)", fwardBand + " > 308.0", Color.RED.darker(), 0.5f));
        }
    }

    /**
     * Returns a new default set of masks definitions for this product file.
     *
     * @param dsName the name of the flag dataset
     * @return a new default set, an empty array if no default set is given for this product type, never
     *         {@code null}.
     */
    @Override
    public Mask[] createDefaultMasks(String dsName) {
        if (getProductType().endsWith("1P")) {
            if ("confid_flags_nadir".equalsIgnoreCase(dsName) || "confid_flags_fward".equalsIgnoreCase(dsName)) {
                String prefix = "qln_";
                if ("confid_flags_fward".equalsIgnoreCase(dsName)) {
                    prefix = "qlf_";
                }
                return createL1bConfidBMDs(prefix, dsName);
            } else if ("cloud_flags_nadir".equalsIgnoreCase(dsName) || "cloud_flags_fward".equalsIgnoreCase(dsName)) {
                String prefix = "clf_";
                if ("cloud_flags_nadir".equalsIgnoreCase(dsName)) {
                    prefix = "cln_";
                }
                return createL1bCloudBMDs(prefix, dsName);
            }
        } else if (getProductType().endsWith("2P")) {
            if ("flags".equalsIgnoreCase(dsName)) {
                return createL2BMDs();
            }
        }
        return new Mask[0];
    }


    private Mask[] createL1bCloudBMDs(String prefix, String dsName) {
        return new Mask[]{
                mask(mkBMDNm(prefix, "LAND"), null, dsName + ".LAND", Color.green, 0.5F),
                mask(mkBMDNm(prefix, "CLOUDY"), null, dsName + ".CLOUDY", Color.cyan, 0.5F),
                mask(mkBMDNm(prefix, "SUN_GLINT"), null, dsName + ".SUN_GLINT", Color.yellow, 0.5F),
                mask(mkBMDNm(prefix, "CLOUDY_REFL_HIST"), null, dsName + ".CLOUDY_REFL_HIST", Color.orange, 0.5F),
                mask(mkBMDNm(prefix, "CLOUDY_SPAT_COHER_16"), null, dsName + ".CLOUDY_SPAT_COHER_16", Color.red, 0.5F),
                mask(mkBMDNm(prefix, "CLOUDY_SPAT_COHER_11"), null, dsName + ".CLOUDY_SPAT_COHER_11", Color.blue, 0.5F),
                mask(mkBMDNm(prefix, "CLOUDY_GROSS_12"), null, dsName + ".CLOUDY_GROSS_12", Color.magenta, 0.5F),
                mask(mkBMDNm(prefix, "CLOUDY_CIRRUS_11_12"), null, dsName + ".CLOUDY_CIRRUS_11_12", Color.pink, 0.5F),
                mask(mkBMDNm(prefix, "CLOUDY_MED_HI_LEVEL_37_12"), null, dsName + ".CLOUDY_MED_HI_LEVEL_37_12", Color.yellow, 0.5F),
                mask(mkBMDNm(prefix, "CLOUDY_FOG_LOW_STRATUS_11_37"), null, dsName + ".CLOUDY_FOG_LOW_STRATUS_11_37", Color.orange, 0.5F),
                mask(mkBMDNm(prefix, "CLOUDY_VW_DIFF_11_12"), null, dsName + ".CLOUDY_VW_DIFF_11_12", Color.red, 0.5F),
                mask(mkBMDNm(prefix, "CLOUDY_VW_DIFF_37_11"), null, dsName + ".CLOUDY_VW_DIFF_37_11", Color.green, 0.5F),
                mask(mkBMDNm(prefix, "CLOUDY_THERM_HIST_11_12"), null, dsName + ".CLOUDY_THERM_HIST_11_12", Color.blue, 0.5F),
                mask(mkBMDNm(prefix, "CLOUDY_VIS_CHANNEL"), null, dsName + ".CLOUDY_VIS_CHANNEL", Color.yellow.darker(), 0.5F),
                mask(mkBMDNm(prefix, "CLOUDY_NDSI"), null, dsName + ".CLOUDY_NDSI", Color.gray, 0.5F)
        };
    }

    private Mask[] createL1bConfidBMDs(String prefix, String dsName) {
        return new Mask[]{
                mask(mkBMDNm(prefix, "BLANKING"), null, dsName + ".BLANKING", Color.red, 0.5F),
                mask(mkBMDNm(prefix, "COSMETIC"), null, dsName + ".COSMETIC", Color.yellow, 0.5F),
                mask(mkBMDNm(prefix, "SCAN_ABSENT"), null, dsName + ".SCAN_ABSENT", Color.orange, 0.5F),
                mask(mkBMDNm(prefix, "ABSENT"), null, dsName + ".ABSENT", Color.green, 0.5F),
                mask(mkBMDNm(prefix, "NOT_DECOMPR"), null, dsName + ".NOT_DECOMPR", Color.blue, 0.5F),
                mask(mkBMDNm(prefix, "NO_SIGNAL"), null, dsName + ".NO_SIGNAL", Color.magenta, 0.5F),
                mask(mkBMDNm(prefix, "SATURATION"), null, dsName + ".SATURATION", Color.green, 0.5F),
                mask(mkBMDNm(prefix, "OUT_OF_RANGE"), null, dsName + ".OUT_OF_RANGE", Color.red, 0.5F),
                mask(mkBMDNm(prefix, "NO_CALIB_PARAM"), null, dsName + ".NO_CALIB_PARAM", Color.cyan, 0.5F),
                mask(mkBMDNm(prefix, "UNFILLED"), null, dsName + ".UNFILLED", Color.yellow, 0.5F)
        };
    }

    private Mask[] createL2BMDs() {
        return new Mask[]{
                mask(mkBMDNm("LAND"), null, "flags.LAND", Color.green, 0.5F),
                mask(mkBMDNm("WATER"), null, "!flags.LAND AND !(flags.NADIR_CLOUD OR flags.FWARD_CLOUD)", Color.blue, 0.5F),
                mask(mkBMDNm("CLOUD"), null, "flags.NADIR_CLOUD OR flags.FWARD_CLOUD", Color.cyan, 0.5F),
                mask(mkBMDNm("NADIR_CLOUD"), null, "flags.NADIR_CLOUD", Color.cyan, 0.5F),
                mask(mkBMDNm("NADIR_BLANKING"), null, "flags.NADIR_BLANKING", Color.orange, 0.5F),
                mask(mkBMDNm("NADIR_COSMETIC"), null, "flags.NADIR_COSMETIC", Color.red, 0.5F),
                mask(mkBMDNm("FWARD_CLOUD"), null, "flags.FWARD_CLOUD", Color.cyan, 0.5F),
                mask(mkBMDNm("FWARD_BLANKING"), null, "flags.FWARD_BLANKING", Color.orange, 0.5F),
                mask(mkBMDNm("FWARD_COSMETIC"), null, "flags.FWARD_COSMETIC", Color.red, 0.5F),
                mask(mkBMDNm("CLOUDY_16_MY"), null, "flags.CLOUDY_16_MY", Color.cyan, 0.5F),
                mask(mkBMDNm("CLOUDY_11_12_MY"), null, "flags.CLOUDY_11_12_MY", Color.cyan, 0.5F),
                mask(mkBMDNm("CLOUDY_HISTO"), null, "flags.CLOUDY_HISTO", Color.cyan, 0.5F),
                mask(mkBMDNm("NADIR_SST_ONLY_VALID"), null, "flags.NADIR_SST_ONLY_VALID", Color.red, 0.5F),
                mask(mkBMDNm("NADIR_SST_ONLY_37_MY_VALID"), null, "flags.NADIR_SST_ONLY_37_MY_VALID", Color.orange, 0.5F),
                mask(mkBMDNm("DUAL_SST_VALID"), null, "flags.DUAL_SST_VALID", Color.red, 0.5F),
                mask(mkBMDNm("DUAL_SST_VALID_37_MY"), null, "flags.DUAL_SST_VALID_37_MY", Color.orange, 0.5F)
        };
    }

    private String mkBMDNm(String base) {
        return mkBMDNm("", base);
    }

    private String mkBMDNm(String prefix, String base) {
        return (prefix + base).toLowerCase();
    }
}
