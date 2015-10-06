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
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Debug;

import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Map;


/**
 * The <code>MerisProductFile</code> is a specialization of the abstract <code>ProductFile</code> class for ENVISAT
 * MERIS data products.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see org.esa.snap.dataio.envisat.AatsrProductFile
 */
public class MerisProductFile extends ProductFile {

    /**
     * Product type prefix for MERIS RR L1b
     */
    public static final String RR__1_PREFIX = "MER_RR__1";
    /**
     * Product type prefix for MERIS RR L2
     */
    public static final String RR__2_PREFIX = "MER_RR__2";
    /**
     * Product type prefix for MERIS FR L1b
     */
    public static final String FR__1_PREFIX = "MER_FR__1";
    /**
     * Product type prefix for MERIS FR L2
     */
    public static final String FR__2_PREFIX = "MER_FR__2";
    /**
     * Product type prefix for MERIS FR full swath L1b
     */
    public static final String FRS_1_PREFIX = "MER_FRS_1";
    /**
     * Product type prefix for MERIS FR full swath L2
     */
    public static final String FRS_2_PREFIX = "MER_FRS_2";
    /**
     * Product type prefix for MERIS FSG full resolution full swath geo/ortho-corrected L1b
     */
    public static final String FSG_1_PREFIX = "MER_FSG_1";

    /**
     * Product type suffix for IODD-5 backward compatibility
     */
    public static final String IODD5_SUFFIX = "_IODD5";

    /**
     * Product type suffix for IODD-6 backward compatibility
     */
    public static final String IODD6_SUFFIX = "_IODD6";

    /**
     * Product type suffix for IODD-7 backward compatibility
     */
    public static final String IODD7_SUFFIX = "_IODD7";


    public static final String BITMASKDEF_NAME_LAND = "land";
    public static final String BITMASKDEF_NAME_WATER = "water";
    public static final String BITMASKDEF_NAME_COASTLINE = "coastline";
    public static final String BITMASKDEF_NAME_COSMETIC = "cosmetic";
    public static final String BITMASKDEF_NAME_DUPLICATED = "duplicated";
    public static final String BITMASKDEF_NAME_GLINT_RISK = "glint_risk";
    public static final String BITMASKDEF_NAME_SUSPECT = "suspect";
    public static final String BITMASKDEF_NAME_BRIGHT = "bright";
    public static final String BITMASKDEF_NAME_INVALID = "invalid";
    public static final String BITMASKDEF_NAME_CLOUD = "cloud";
    public static final String BITMASKDEF_NAME_INVALID_REFLECTANCES = "invalid_reflectances";
    public static final String BITMASKDEF_NAME_INVALID_WATER_VAPOUR = "invalid_water_vapour";
    public static final String BITMASKDEF_NAME_INVALID_ALGAL_1 = "invalid_algal_1";
    public static final String BITMASKDEF_NAME_INVALID_ALGAL2_TSM_YS = "invalid_algal2_tsm_ys";
    public static final String BITMASKDEF_NAME_INVALID_PHOTOSYN_RAD = "invalid_photosyn_rad";
    public static final String BITMASKDEF_NAME_INVALID_TOA_VEG = "invalid_toa_veg";
    public static final String BITMASKDEF_NAME_INVALID_BOA_VEG = "invalid_boa_veg";
    public static final String BITMASKDEF_NAME_INVALID_RECT_REFL = "invalid_rect_refl";
    public static final String BITMASKDEF_NAME_INVALID_SURF_PRESS = "invalid_surf_press";
    public static final String BITMASKDEF_NAME_INVALID_AERO_PRODUCTS = "invalid_aero_products";
    public static final String BITMASKDEF_NAME_INVALID_CLOUD_ALBEDO = "invalid_cloud_albedo";
    public static final String BITMASKDEF_NAME_INVALID_CLOUD_OPT_THICK_AND_TYPE = "invalid_cloud_opt_thick_and_type";
    public static final String BITMASKDEF_NAME_INVALID_CLOUD_TOP_PRESS = "invalid_cloud_top_press";
    public static final String BITMASKDEF_NAME_HIGH_GLINT = "high_glint";
    public static final String BITMASKDEF_NAME_MEDIUM_GLINT = "medium_glint";
    public static final String BITMASKDEF_NAME_ICE_HAZE = "ice_haze";
    public static final String BITMASKDEF_NAME_ABSOA_CONT = "absoa_cont";
    public static final String BITMASKDEF_NAME_ABSOA_DUST = "absoa_dust";
    public static final String BITMASKDEF_NAME_SNOW_ICE = "snow_ice";
    public static final String BITMASKDEF_NAME_CASE2_S = "case2_s";
    public static final String BITMASKDEF_NAME_CASE2_ANOM = "case2_anom";
    public static final String BITMASKDEF_NAME_CASE2_Y = "case2_y";
    public static final String BITMASKDEF_NAME_DARK_VEGETATION = "dark_vegetation";
    public static final String BITMASKDEF_NAME_UNCERTAIN_AEROSOL_MODEL = "uncertain_aerosol_model";
    public static final String BITMASKDEF_NAME_TOAVI_BRIGHT = "toavi_bright";
    public static final String BITMASKDEF_NAME_TOAVI_BAD = "toavi_bad";
    public static final String BITMASKDEF_NAME_TOAVI_CSI = "toavi_csi";
    public static final String BITMASKDEF_NAME_TOAVI_WS = "toavi_ws";
    public static final String BITMASKDEF_NAME_TOAVI_INVAL_REC = "toavi_inval_rec";
    public static final String BITMASKDEF_NAME_P_CONFIDENCE = "p_confidence";
    public static final String BITMASKDEF_NAME_LOW_PRESSURE = "low_pressure";
    public static final String BITMASKDEF_NAME_WHITE_SCATTERER = "white_scatterer";
    public static final String BITMASKDEF_NAME_PCD_1_13 = "pcd_1_13";
    public static final String BITMASKDEF_NAME_PCD_14 = "pcd_14";
    public static final String BITMASKDEF_NAME_PCD_15 = "pcd_15";
    public static final String BITMASKDEF_NAME_PCD_16 = "pcd_16";
    public static final String BITMASKDEF_NAME_PCD_17 = "pcd_17";
    public static final String BITMASKDEF_NAME_PCD_18 = "pcd_18";
    public static final String BITMASKDEF_NAME_PCD_19 = "pcd_19";
    public static final String BITMASKDEF_NAME_LOW_SUN = "low_sun";
    public static final String BITMASKDEF_NAME_BLUE_AERO = "blue_aero";
    public static final String BITMASKDEF_NAME_LAND_AEROSOL_ON = "land_aerosol_on";
    public static final String BITMASKDEF_NAME_DENSE_DARK_VEG = "dense_dark_veg";
    public static final String BITMASKDEF_NAME_BPAC_ON = "bpac_on";

    /**
     * iodd version constants
     */
    public static final int IODD_VERSION_UNKNOWN = -1;
    public static final int IODD_VERSION_5 = 5;
    public static final int IODD_VERSION_6 = 6;
    public static final int IODD_VERSION_7 = 7;
    public static final int IODD_VERSION_8 = 8;


    /**
     * The UTC time of the scene's first scan line
     */
    private ProductData.UTC _sceneRasterStartTime;

    /**
     * The UTC time of the scene's last scan line
     */
    private ProductData.UTC _sceneRasterStopTime;

    /**
     * Number of pixels in across-track direction
     */
    private int _sceneRasterWidth;

    /**
     * Number of pixels in along-track direction
     */
    private int _sceneRasterHeight;

    /**
     * Number of columns per tie-point in across-track direction
     */
    private int _tiePointSubSamplingX;

    /**
     * Number of lines per tie-point in along-track direction
     */
    private int _tiePointSubSamplingY;

    /**
     * The IODD version number.
     */
    private int _ioddVersion;


    /**
     * Constructs a <code>MerisProductFile</code> for the given seekable data input stream.
     *
     * @param file            the abstract file path representation.
     * @param dataInputStream the seekable data input stream which will be used to read data from the product file.
     * @param lineInterleaved if true the Envisat file is expected to be in line interleaved storage format
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    protected MerisProductFile(File file, ImageInputStream dataInputStream, boolean lineInterleaved) throws
                                                                                                     IOException {
        super(file, dataInputStream, lineInterleaved);
    }

    /**
     * Gets the (sensing) start time associated with the first raster data line.
     *
     * @return the sensing start time, can be null e.g. for non-swath products
     */
    @Override
    public ProductData.UTC getSceneRasterStartTime() {
        return _sceneRasterStartTime;
    }


    /**
     * Gets the (sensing) stop time associated with the first raster data line.
     *
     * @return the sensing stop time, can be null e.g. for non-swath products
     */
    @Override
    public ProductData.UTC getSceneRasterStopTime() {
        return _sceneRasterStopTime;
    }

    /**
     * Overrides the base class method.
     *
     * @see org.esa.snap.dataio.envisat.ProductFile#getSceneRasterWidth()
     */
    @Override
    public int getSceneRasterWidth() {
        return _sceneRasterWidth;
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
        return EnvisatConstants.MERIS_TIE_POINT_OFFSET_X;
    }

    /**
     * Overrides the base class method.
     */
    @Override
    public float getTiePointGridOffsetY(int gridWidth) {
        return EnvisatConstants.MERIS_TIE_POINT_OFFSET_Y;
    }

    /**
     * Overrides the base class method.
     *
     * @param gridWidth this parameter is ignored for MERIS products
     */
    @Override
    public float getTiePointSubSamplingX(int gridWidth) {
        return _tiePointSubSamplingX;
    }

    /**
     * Overrides the base class method.
     *
     * @param gridWidth this parameter is ignored for MERIS products
     */
    @Override
    public float getTiePointSubSamplingY(int gridWidth) {
        return _tiePointSubSamplingY;
    }

    /**
     * Returns the name of the GADS for this ENVISAT product file.
     *
     * @return the GADS name "Scaling_Factor_GADS"
     */
    @Override
    public String getGADSName() {
        return EnvisatConstants.MERIS_GADS_NAME;
    }

    /**
     * Determines whether the scan lines in this product data file have to be flipped before in "normal" view (pixel
     * numbers increase from west to east). <p>For MERIS products the method always returns true.
     */
    @Override
    public boolean storesPixelsInChronologicalOrder() {
        return true;
    }

    @Override
    public float[] getSpectralBandWavelengths() {
        Field field = getSPH().getParam("BAND_WAVELEN");
        return createFloatArray(field, 1.0E-3F);
    }

    @Override
    public float[] getSpectralBandBandwidths() {
        Field field = getSPH().getParam("BANDWIDTH");
        return createFloatArray(field, 1.0E-3F);
    }

    @Override
    public float[] getSpectralBandSolarFluxes() {
        Field field = (getGADS() != null) ? getGADS().getField("sun_spec_flux") : null;
        return createFloatArray(field, 1.0F);
    }

    /**
     * This method is called after the main product header has been read in successfully.
     * <p> Sub-classes should set product specific parameters in the <code>parameters</code> argument. The parameters
     * can be referenced in DDDB in order to implement dynamic field length, such as 'LINE_WIDTH'.
     * <p> When this method is called, the <code>getMPH()</code> method returns a non-null value.
     * <p> The default implementation is empty.
     *
     * @param parameters product specific parameters (possibly referenced within in the DDDB
     *
     * @throws java.io.IOException if a header format error was detected or if an I/O error occurs
     */
    @Override
    protected void postProcessMPH(Map parameters) throws IOException {
        _ioddVersion = IODD_VERSION_UNKNOWN;
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
    protected void postProcessSPH(final Map parameters) throws IOException {
        final String[] validDatasetNames = getValidDatasetNames(EnvisatConstants.DS_TYPE_MEASUREMENT);
        if (validDatasetNames.length == 0) {
            throw new IllegalFileFormatException("no valid datasets found in this MERIS product");
        }

        final String firstMDSName = validDatasetNames[0];
        final DSD dsdFirstMDS = getDSD(firstMDSName);

        _sceneRasterHeight = dsdFirstMDS.getNumRecords();
        _sceneRasterWidth = getSPH().getParamInt("LINE_LENGTH");

        _tiePointSubSamplingX = getSPH().getParamInt("SAMPLES_PER_TIE_PT");
        _tiePointSubSamplingY = getSPH().getParamInt("LINES_PER_TIE_PT");
        int tiePointGridWidth = 1 + _sceneRasterWidth / _tiePointSubSamplingX;
        int tiePointGridHeight = 1 + _sceneRasterHeight / _tiePointSubSamplingY;

        final DSD dsdTiePointsADS = getDSD("Tie_points_ADS");
        if (dsdTiePointsADS == null) {
            throw new IllegalFileFormatException("invalid product: missing DSD for dataset 'Tie_points_ADS'");
        }

        final int numTiePointLinesFound = dsdTiePointsADS.getNumRecords();
        if (numTiePointLinesFound > tiePointGridHeight) {
            Debug.trace("WARNING: found more tie point records than required:");
            Debug.trace("WARNING: product: " + getProductId());
            tiePointGridHeight = numTiePointLinesFound;
        } else if (numTiePointLinesFound < tiePointGridHeight) {
            Debug.trace("WARNING: found less tie point records than required:");
            Debug.trace("WARNING: product: " + getProductId());
            tiePointGridHeight = numTiePointLinesFound;
        }

        parameters.put("sceneRasterWidth", _sceneRasterWidth);
        parameters.put("sceneRasterHeight", _sceneRasterHeight);
        parameters.put("tiePointGridWidth", tiePointGridWidth);
        parameters.put("tiePointGridHeight", tiePointGridHeight);
        parameters.put("tiePointSubSamplingX", _tiePointSubSamplingX);
        parameters.put("tiePointSubSamplingY", _tiePointSubSamplingY);

        // (nf) 22.01.2003: the property priorToIodd6 has been introduced to provide backward compatibility
        // for MERIS L1b RR and FR product formats prior to IODD 6
        //
        setIODDVersion();

        _sceneRasterStartTime = getRecordTime(firstMDSName, "dsr_time", 0);
        _sceneRasterStopTime = getRecordTime(firstMDSName, "dsr_time", _sceneRasterHeight - 1);
    }

    /**
     * Gets the product type string as used within the DDDB, e.g. "MER_FR__1P_IODD5". This implementation considers
     * format changes in IODD 6.
     *
     * @return the product type string
     */
    @Override
    protected String getDddbProductType() {
        // Debug.trace("MerisProductFile.getDddbProductType: IODD version still unknown");
        final String productType = getDddbProductTypeReplacement(getProductType(), getIODDVersion());
        return productType != null ? productType : super.getDddbProductType();
    }

    static String getDddbProductTypeReplacement(final String productType, final int ioddVersion) {
        if (ioddVersion == IODD_VERSION_5) {
            if (productType.startsWith(RR__1_PREFIX) || productType.startsWith(FR__1_PREFIX)) {
                return productType + IODD5_SUFFIX;
            }
            // level 2 of IODD 5 is compliant with level 2 of IODD 6
            if (productType.startsWith(RR__2_PREFIX) || productType.startsWith(FR__2_PREFIX)) {
                return productType + IODD6_SUFFIX;
            }
        } else if (ioddVersion == IODD_VERSION_6) {
            // level 1 of IODD 7 is compliant with level 1 of IODD 6, so no action needed here
            if (productType.startsWith(RR__2_PREFIX) || productType.startsWith(FR__2_PREFIX)) {
                return productType + IODD6_SUFFIX;
            }
            // FR full swath are compliant with "nominal" FR
            if (productType.startsWith(FRS_1_PREFIX)) {
                return FR__1_PREFIX + productType.substring(FR__1_PREFIX.length()) + IODD6_SUFFIX;
            }
            if (productType.startsWith(FRS_2_PREFIX)) {
                return FR__2_PREFIX + productType.substring(FR__2_PREFIX.length()) + IODD6_SUFFIX;
            }
        } else if (ioddVersion >= IODD_VERSION_7 || ioddVersion == IODD_VERSION_UNKNOWN) {
            // level 1 of IODD 7,8 are compliant with level 1 of IODD 6, so no action needed here
            // FR full swath are compliant with "nominal" FR
            if (productType.startsWith(FRS_1_PREFIX)) {
                return FR__1_PREFIX + productType.substring(FR__1_PREFIX.length());
            }

            if (ioddVersion == IODD_VERSION_7) {
                if (productType.startsWith(RR__2_PREFIX) || productType.startsWith(FR__2_PREFIX)) {
                    return productType + IODD7_SUFFIX;
                }
                if (productType.startsWith(FRS_2_PREFIX)) {
                    return FR__2_PREFIX + productType.substring(FR__2_PREFIX.length()) + IODD7_SUFFIX;
                }
            } else {
                if (productType.startsWith(FRS_2_PREFIX)) {
                    return FR__2_PREFIX + productType.substring(FR__2_PREFIX.length());
                }
            }

        }
        return null;
    }

    public int getIODDVersion() {
        return _ioddVersion;
    }

    /**
     * * This method just delegates to
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
     *
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
        if (getIODDVersion() >= IODD_VERSION_7) {
            if (scalingMethod == BandInfo.SCALE_LOG10 &&
                isOldLinearYellowSubstanceScaling(bandName,
                                                  scalingOffset,
                                                  scalingFactor)) {
                getLogger().info("Out-of-date MERIS L2 format detected: band '" +
                                 EnvisatConstants.MERIS_L2_YELLOW_SUBST_BAND_NAME +
                                 "': changing scaling from LOG to LINEAR");
                scalingMethod = BandInfo.SCALE_LINEAR;
            }
        }
        return super.createBandInfo(bandName,
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
                                    dataSetName);
    }

    /**
     * Sets the IODD version which is an indicator for the product format.
     */
    private void setIODDVersion() {
        _ioddVersion = IODD_VERSION_8;

        // check for version 7 IODD
        final Field softwareVerField = getMPH().getParam("SOFTWARE_VER");
        if (softwareVerField != null) {
            final String softwareVersion = softwareVerField.getAsString();
            if (softwareVersion.startsWith("MERIS") ||
                softwareVersion.startsWith("MEGS-PC/7")) {
                _ioddVersion = IODD_VERSION_7;
            }
        }

        // check for version 5 IODD
        final DSD flagsDSD = getDSD("Flags");
        if (flagsDSD != null) {
            if (getProductType().startsWith(RR__1_PREFIX)
                && flagsDSD.getRecordSize() < 3376) {
                _ioddVersion = IODD_VERSION_5;
            }
            if (getProductType().startsWith(FR__1_PREFIX)) {
                final Field sphDescParam = getSPH().getParam("SPH_DESCRIPTOR");
                final String sphDescriptor = sphDescParam.getAsString();
                final boolean isImagette = sphDescriptor.startsWith("MER_FR_IM");
                if ((isImagette && flagsDSD.getRecordSize() < 3472)
                    || (!isImagette && flagsDSD.getRecordSize() < 6736)) {
                    _ioddVersion = IODD_VERSION_5;
                }
            }
        }

        if (_ioddVersion == IODD_VERSION_5) {
            getLogger().warning("old product format: IODD version less than 6, 'detector_index' is not available");
        }

        if (_ioddVersion != IODD_VERSION_5) {
            // retrieve the dataset descriptors read from the file
            DSD[] dsds = getDsds();

            // find the one with the old dataset name
            String oldMdsName = "Epsilon, OPT";
            for (DSD dsd : dsds) {
                if (dsd != null) {
                    if (dsd.getDatasetName().contains(oldMdsName)) {
                        _ioddVersion = IODD_VERSION_6;
                        getLogger().warning(
                                "old product format: IODD version less than 7, 'aero_alpha' is not available");
                        break;
                    }
                }
            }
        }
    }

    @Override
    public String getAutoGroupingPattern() {
        if (getProductType().contains("_1")) {
            return "radiance";
        } else {
            return "reflec";
        }
    }

    @Override
    void setInvalidPixelExpression(Band band) {
        if (band.getName().startsWith("reflec_")) {
            band.setNoDataValueUsed(true);
            band.setNoDataValue(0);
        } else {
            band.setNoDataValueUsed(false);
            band.setNoDataValue(0);
        }
    }

    /**
     * Returns the names of all default bitmasks to be used for the band with the given name.
     *
     * @param bandName the band's name
     *
     * @return the array of bitmask names or null if no bitmasks are applicable
     */
    @Override
    public String[] getDefaultBitmaskNames(String bandName) {
        if (bandName.startsWith("refl")) {
            return new String[]{
                    BITMASKDEF_NAME_COASTLINE,
                    BITMASKDEF_NAME_INVALID_REFLECTANCES
            };
        } else if (bandName.equals("water_vapour")) {
            return new String[]{
                    BITMASKDEF_NAME_COASTLINE,
                    BITMASKDEF_NAME_INVALID_WATER_VAPOUR
            };
        } else if (bandName.equals("algal_1")) {
            return new String[]{
                    BITMASKDEF_NAME_LAND,
                    BITMASKDEF_NAME_CLOUD,
                    BITMASKDEF_NAME_COASTLINE,
                    BITMASKDEF_NAME_INVALID_ALGAL_1
            };
        } else if (bandName.equals("algal_2") ||
                   bandName.equals("yellow_subs") ||
                   bandName.equals("total_susp")) {
            return new String[]{
                    BITMASKDEF_NAME_LAND,
                    BITMASKDEF_NAME_CLOUD,
                    BITMASKDEF_NAME_COASTLINE,
                    BITMASKDEF_NAME_INVALID_ALGAL2_TSM_YS
            };
        } else if (bandName.equals("photosyn_rad")) {
            return new String[]{
                    BITMASKDEF_NAME_LAND,
                    BITMASKDEF_NAME_CLOUD,
                    BITMASKDEF_NAME_COASTLINE,
                    BITMASKDEF_NAME_INVALID_PHOTOSYN_RAD
            };
        } else if (bandName.equals("toa_veg")) {
            return new String[]{
                    BITMASKDEF_NAME_WATER,
                    BITMASKDEF_NAME_CLOUD,
                    BITMASKDEF_NAME_COASTLINE,
                    BITMASKDEF_NAME_INVALID_TOA_VEG
            };
        } else if (bandName.equals("boa_veg")) {
            return new String[]{
                    BITMASKDEF_NAME_WATER,
                    BITMASKDEF_NAME_CLOUD,
                    BITMASKDEF_NAME_COASTLINE,
                    BITMASKDEF_NAME_INVALID_BOA_VEG
            };
        } else if (bandName.equals("rect_refl_nir") ||
                   bandName.equals("rect_refl_red")) {
            return new String[]{
                    BITMASKDEF_NAME_WATER,
                    BITMASKDEF_NAME_CLOUD,
                    BITMASKDEF_NAME_COASTLINE,
                    BITMASKDEF_NAME_INVALID_RECT_REFL
            };
        } else if (bandName.equals("surf_press")) {
            return new String[]{
                    BITMASKDEF_NAME_WATER,
                    BITMASKDEF_NAME_CLOUD,
                    BITMASKDEF_NAME_COASTLINE,
                    BITMASKDEF_NAME_INVALID_SURF_PRESS
            };
        } else if (bandName.equals("aero_alpha") ||
                   bandName.equals("aero_epsilon") ||
                   bandName.startsWith("aero_opt_thick")) {
            return new String[]{
                    BITMASKDEF_NAME_CLOUD,
                    BITMASKDEF_NAME_COASTLINE,
                    BITMASKDEF_NAME_INVALID_AERO_PRODUCTS
            };
        } else if (bandName.equals("cloud_albedo")) {
            return new String[]{
                    BITMASKDEF_NAME_LAND,
                    BITMASKDEF_NAME_WATER,
                    BITMASKDEF_NAME_COASTLINE,
                    BITMASKDEF_NAME_INVALID_CLOUD_ALBEDO
            };
        } else if (bandName.equals("cloud_opt_thick") ||
                   bandName.equals("cloud_type")) {
            return new String[]{
                    BITMASKDEF_NAME_LAND,
                    BITMASKDEF_NAME_WATER,
                    BITMASKDEF_NAME_COASTLINE,
                    BITMASKDEF_NAME_INVALID_CLOUD_OPT_THICK_AND_TYPE
            };
        } else if (bandName.equals("cloud_top_press")) {
            return new String[]{
                    BITMASKDEF_NAME_LAND,
                    BITMASKDEF_NAME_WATER,
                    BITMASKDEF_NAME_COASTLINE,
                    BITMASKDEF_NAME_INVALID_CLOUD_TOP_PRESS
            };
        } else if (bandName.equals("l2_flags")) {
            return new String[]{
                    BITMASKDEF_NAME_COASTLINE
            };
        } else {
            return null;
        }
    }

    /**
     * Returns a new default set of mask definitions for this product file.
     *
     * @param flagDsName the name of the flag dataset
     *
     * @return a new default set, an empty array if no default set is given for this product type, never
     *         <code>null</code>.
     */
    @Override
    public Mask[] createDefaultMasks(String flagDsName) {
        if (getProductType().endsWith("1P")) {
            return new Mask[]{
                    mask(BITMASKDEF_NAME_COASTLINE, null, "l1_flags.COASTLINE", Color.green, 0.0F),
                    mask(BITMASKDEF_NAME_LAND, null, "l1_flags.LAND_OCEAN", new Color(51, 153, 0), 0.75F),
                    mask(BITMASKDEF_NAME_WATER, null, "NOT l1_flags.LAND_OCEAN", new Color(153, 153, 255), 0.75F),
                    mask(BITMASKDEF_NAME_COSMETIC, null, "l1_flags.COSMETIC", new Color(204, 153, 255), 0.5F),
                    mask(BITMASKDEF_NAME_DUPLICATED, null, "l1_flags.DUPLICATED", Color.orange, 0.5F),
                    mask(BITMASKDEF_NAME_GLINT_RISK, null, "l1_flags.GLINT_RISK", Color.magenta, 0.5F),
                    mask(BITMASKDEF_NAME_SUSPECT, null, "l1_flags.SUSPECT", new Color(204, 102, 255), 0.5F),
                    mask(BITMASKDEF_NAME_BRIGHT, null, "l1_flags.BRIGHT", Color.yellow, 0.5F),
                    mask(BITMASKDEF_NAME_INVALID, null, "l1_flags.INVALID", Color.red, 0.0F)
            };
        } else if (getProductType().endsWith("2P")) {
            if ((getIODDVersion() == IODD_VERSION_6) || (getIODDVersion() == IODD_VERSION_5)) {
                return new Mask[]{
                        // Pixel Types
                        mask(BITMASKDEF_NAME_COASTLINE, null, "l2_flags.COASTLINE", Color.green, 0.0F),
                        mask(BITMASKDEF_NAME_LAND, null, "l2_flags.LAND", new Color(102, 102, 102), 0.0F),
                        mask(BITMASKDEF_NAME_CLOUD, null, "l2_flags.CLOUD", new Color(255, 255, 255), 0.0F),
                        mask(BITMASKDEF_NAME_WATER, null, "l2_flags.WATER", new Color(0, 0, 0), 0.0F),

                        // Combined quality flags in red
                        mask(BITMASKDEF_NAME_INVALID_REFLECTANCES, "pixels flagged for invalid reflectances", "l2_flags.PCD_1_13 AND (l2_flags.LAND OR l2_flags.WATER OR l2_flags.CLOUD)", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_WATER_VAPOUR, "pixels flagged for invalid water vapour", "l2_flags.PCD_14 AND (l2_flags.LAND OR l2_flags.WATER OR l2_flags.CLOUD)", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_ALGAL_1, "water pixels flagged for invalid algal1", "l2_flags.WATER AND l2_flags.PCD_15", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_ALGAL2_TSM_YS, "water pixels flagged for invalid algal2 and yellow_subs and total_susp", "l2_flags.WATER AND (l2_flags.PCD_16 OR l2_flags.PCD_17)", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_PHOTOSYN_RAD, "water pixels flagged for invalid PAR", "l2_flags.WATER AND l2_flags.PCD_18", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_TOA_VEG, "land pixels flagged for invalid toa_veg", "l2_flags.LAND AND l2_flags.PCD_15", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_BOA_VEG, "land pixels flagged for invalid boa_veg", "l2_flags.LAND AND l2_flags.PCD_17", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_RECT_REFL, "land pixels flagged for invalid rectified reflectances", "l2_flags.LAND AND l2_flags.PCD_16", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_SURF_PRESS, "land pixels flagged for invalid surf_press", "l2_flags.LAND AND l2_flags.PCD_18", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_AERO_PRODUCTS, "water pixels flagged for invalid aero_epsilon and aero_opt_thick_(i)", "l2_flags.PCD_19 AND (l2_flags.LAND OR l2_flags.WATER)", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_CLOUD_ALBEDO, "cloud pixels flagged for invalid cloud_albedo", "l2_flags.CLOUD AND l2_flags.PCD_18", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_CLOUD_OPT_THICK_AND_TYPE, "cloud pixels flagged for invalid cloud_opt_thick and cloud_type", "l2_flags.CLOUD AND l2_flags.PCD_19", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_CLOUD_TOP_PRESS, "cloud pixels flagged for invalid cloud_top_press", "l2_flags.CLOUD AND l2_flags.PCD_15", Color.black, 0.0F),

                        // the conditions that limitate algorithms in pink
                        mask(BITMASKDEF_NAME_HIGH_GLINT, null, "l2_flags.HIGH_GLINT", new Color(204, 0, 204), 0.5F),
                        mask(BITMASKDEF_NAME_MEDIUM_GLINT, null, "l2_flags.MEDIUM_GLINT", new Color(255, 51, 255), 0.5F),
                        mask(BITMASKDEF_NAME_ICE_HAZE, null, "l2_flags.ICE_HAZE", Color.yellow, 0.5F),

                        // the flags for atmospheric correction in blue
                        mask(BITMASKDEF_NAME_ABSOA_CONT, null, "l2_flags.ABSOA_CONT", new Color(0, 102, 255), 0.5F),
                        mask(BITMASKDEF_NAME_ABSOA_DUST, null, "l2_flags.ABSOA_DUST", new Color(0, 204, 255), 0.5F),

                        // Case2 water flags in ochre
                        mask(BITMASKDEF_NAME_CASE2_S, null, "l2_flags.CASE2_S", new Color(255, 255, 153), 0.5F),
                        mask(BITMASKDEF_NAME_CASE2_ANOM, null, "l2_flags.CASE2_ANOM", new Color(153, 153, 0), 0.5F),
                        mask(BITMASKDEF_NAME_CASE2_Y, null, "l2_flags.CASE2_Y", new Color(204, 204, 0), 0.5F),

                        // Land product flags
                        mask(BITMASKDEF_NAME_DARK_VEGETATION, null, "l2_flags.DDV", new Color(0, 204, 0), 0.5F),
                        mask(BITMASKDEF_NAME_TOAVI_BRIGHT, null, "l2_flags.TOAVI_BRIGHT", new Color(255, 204, 204), 0.5F),
                        mask(BITMASKDEF_NAME_TOAVI_BAD, null, "l2_flags.TOAVI_BAD", new Color(255, 153, 102), 0.5F),
                        mask(BITMASKDEF_NAME_TOAVI_CSI, null, "l2_flags.TOAVI_CSI", new Color(255, 153, 0), 0.5F),
                        mask(BITMASKDEF_NAME_TOAVI_WS, null, "l2_flags.TOAVI_WS", new Color(204, 102, 0), 0.5F),
                        mask(BITMASKDEF_NAME_TOAVI_INVAL_REC, null, "l2_flags.TOAVI_INVAL_REC", new Color(153, 51, 0), 0.5F),
                        mask(BITMASKDEF_NAME_P_CONFIDENCE, null, "l2_flags.P_CONFIDENCE", new Color(255, 204, 255), 0.5F),
                        mask(BITMASKDEF_NAME_LOW_PRESSURE, null, "l2_flags.LOW_PRESSURE", new Color(204, 204, 255), 0.5F),

                        // L1b copied flags in magenta
                        mask(BITMASKDEF_NAME_COSMETIC, null, "l2_flags.COSMETIC", new Color(204, 153, 255), 0.5F),
                        mask(BITMASKDEF_NAME_SUSPECT, null, "l2_flags.SUSPECT", new Color(204, 102, 255), 0.5F),

                        // Product Confidence Flags
                        mask(BITMASKDEF_NAME_PCD_1_13, null, "l2_flags.PCD_1_13", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_14, null, "l2_flags.PCD_14", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_15, null, "l2_flags.PCD_15", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_16, null, "l2_flags.PCD_16", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_17, null, "l2_flags.PCD_17", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_18, null, "l2_flags.PCD_18", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_19, null, "l2_flags.PCD_19", Color.red, 0.0F),
                };
            } else if (getIODDVersion() == IODD_VERSION_7) {
                return new Mask[]{
                        // Pixel Types
                        mask(BITMASKDEF_NAME_COASTLINE, null, "l2_flags.COASTLINE", Color.green, 0.0F),
                        mask(BITMASKDEF_NAME_LAND, null, "l2_flags.LAND", new Color(102, 102, 102), 0.0F),
                        mask(BITMASKDEF_NAME_CLOUD, null, "l2_flags.CLOUD", new Color(255, 255, 255), 0.0F),
                        mask(BITMASKDEF_NAME_WATER, null, "l2_flags.WATER", new Color(0, 0, 0), 0.0F),

                        // Combined quality flags in red
                        mask(BITMASKDEF_NAME_INVALID_REFLECTANCES, "Pixels flagged for invalid reflectances", "l2_flags.PCD_1_13 AND (l2_flags.LAND OR l2_flags.WATER OR l2_flags.CLOUD)", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_WATER_VAPOUR, "Pixels flagged for invalid water vapour", "l2_flags.PCD_14 AND (l2_flags.LAND OR l2_flags.WATER OR l2_flags.CLOUD)", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_ALGAL_1, "Water pixels flagged for invalid algal1", "l2_flags.WATER AND l2_flags.PCD_15", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_ALGAL2_TSM_YS, "Water pixels flagged for invalid algal2 and yellow_subs and total_susp", "l2_flags.WATER AND (l2_flags.PCD_16 OR l2_flags.PCD_17)", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_PHOTOSYN_RAD, "Water pixels flagged for invalid PAR", "l2_flags.WATER AND l2_flags.PCD_18", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_TOA_VEG, "Land pixels flagged for invalid toa_veg", "l2_flags.LAND AND l2_flags.PCD_15", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_BOA_VEG, "Land pixels flagged for invalid boa_veg", "l2_flags.LAND AND l2_flags.PCD_17", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_RECT_REFL, "Land pixels flagged for invalid rectified reflectances", "l2_flags.LAND AND l2_flags.PCD_16", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_SURF_PRESS, "Land pixels flagged for invalid surf_press", "l2_flags.LAND AND l2_flags.PCD_18", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_AERO_PRODUCTS, "Land and water pixels flagged for invalid aero_alpha and aero_opt_thick_(i)", "l2_flags.PCD_19 AND (l2_flags.LAND OR l2_flags.WATER)", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_CLOUD_ALBEDO, "Cloud pixels flagged for invalid cloud_albedo", "l2_flags.CLOUD AND l2_flags.PCD_18", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_CLOUD_OPT_THICK_AND_TYPE, "Cloud pixels flagged for invalid cloud_opt_thick and cloud_type", "l2_flags.CLOUD AND l2_flags.PCD_19", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_CLOUD_TOP_PRESS, "Cloud pixels flagged for invalid cloud_top_press", "l2_flags.CLOUD AND l2_flags.PCD_15", Color.black, 0.0F),

                        // the conditions that limitate algorithms in pink
                        mask(BITMASKDEF_NAME_LOW_SUN, null, "l2_flags.LOW_SUN", new Color(153, 0, 153), 0.5F),
                        mask(BITMASKDEF_NAME_HIGH_GLINT, null, "l2_flags.HIGH_GLINT", new Color(204, 0, 204), 0.5F),
                        mask(BITMASKDEF_NAME_MEDIUM_GLINT, null, "l2_flags.MEDIUM_GLINT", new Color(255, 51, 255), 0.5F),
                        mask(BITMASKDEF_NAME_ICE_HAZE, null, "l2_flags.ICE_HAZE", Color.yellow, 0.5F),

                        // the flags for atmospheric correction in blue
                        mask(BITMASKDEF_NAME_LAND_AEROSOL_ON, null, "l2_flags.LARS_ON", new Color(51, 51, 255), 0.25F),
                        mask(BITMASKDEF_NAME_ABSOA_DUST, null, "l2_flags.ABSOA_DUST", new Color(0, 204, 255), 0.5F),
                        mask(BITMASKDEF_NAME_BPAC_ON, null, "l2_flags.BPAC_ON", new Color(153, 255, 204), 0.5F),

                        // Case2 water flags in ochre
                        mask(BITMASKDEF_NAME_CASE2_S, null, "l2_flags.CASE2_S", new Color(255, 255, 153), 0.5F),
                        mask(BITMASKDEF_NAME_CASE2_ANOM, null, "l2_flags.CASE2_ANOM", new Color(153, 153, 0), 0.5F),
                        mask(BITMASKDEF_NAME_CASE2_Y, null, "l2_flags.CASE2_Y", new Color(204, 204, 0), 0.5F),

                        // Land product flags
                        mask(BITMASKDEF_NAME_UNCERTAIN_AEROSOL_MODEL, null, "l2_flags.OOADB", new Color(0, 204, 0), 0.5F),
                        mask(BITMASKDEF_NAME_TOAVI_BRIGHT, null, "l2_flags.TOAVI_BRIGHT", new Color(255, 204, 204), 0.5F),
                        mask(BITMASKDEF_NAME_TOAVI_BAD, null, "l2_flags.TOAVI_BAD", new Color(255, 153, 102), 0.5F),
                        mask(BITMASKDEF_NAME_TOAVI_CSI, null, "l2_flags.TOAVI_CSI", new Color(255, 153, 0), 0.5F),
                        mask(BITMASKDEF_NAME_TOAVI_WS, null, "l2_flags.TOAVI_WS", new Color(204, 102, 0), 0.5F),
                        mask(BITMASKDEF_NAME_TOAVI_INVAL_REC, null, "l2_flags.TOAVI_INVAL_REC", new Color(153, 51, 0), 0.5F),
                        mask(BITMASKDEF_NAME_LOW_PRESSURE, null, "l2_flags.LOW_PRESSURE", new Color(204, 204, 255), 0.5F),
                        mask(BITMASKDEF_NAME_WHITE_SCATTERER, null, "l2_flags.WHITE_SCATTERER", new Color(204, 204, 255), 0.5F),

                        // L1b copied flags in magenta
                        mask(BITMASKDEF_NAME_COSMETIC, null, "l2_flags.COSMETIC", new Color(204, 153, 255), 0.5F),
                        mask(BITMASKDEF_NAME_SUSPECT, null, "l2_flags.SUSPECT", new Color(204, 102, 255), 0.5F),

                        // Product Confidence Flags
                        mask(BITMASKDEF_NAME_PCD_1_13, null, "l2_flags.PCD_1_13", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_14, null, "l2_flags.PCD_14", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_15, null, "l2_flags.PCD_15", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_16, null, "l2_flags.PCD_16", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_17, null, "l2_flags.PCD_17", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_18, null, "l2_flags.PCD_18", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_19, null, "l2_flags.PCD_19", Color.red, 0.0F),
                };
            } else if (getIODDVersion() == IODD_VERSION_8) {
                return new Mask[]{
                        // Pixel Types
                        mask(BITMASKDEF_NAME_COASTLINE, null, "l2_flags.COASTLINE", Color.green, 0.0F),
                        mask(BITMASKDEF_NAME_LAND, null, "l2_flags.LAND", new Color(102, 102, 102), 0.0F),
                        mask(BITMASKDEF_NAME_CLOUD, null, "l2_flags.CLOUD", new Color(255, 255, 255), 0.0F),
                        mask(BITMASKDEF_NAME_WATER, null, "l2_flags.WATER", new Color(0, 0, 0), 0.0F),

                        // Combined quality flags in red
                        mask(BITMASKDEF_NAME_INVALID_REFLECTANCES, "Pixels flagged for invalid reflectances", "l2_flags.PCD_1_13 AND (l2_flags.LAND OR l2_flags.WATER OR l2_flags.CLOUD)", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_WATER_VAPOUR, "Pixels flagged for invalid water vapour", "l2_flags.PCD_14 AND (l2_flags.LAND OR l2_flags.WATER OR l2_flags.CLOUD)", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_ALGAL_1, "Water pixels flagged for invalid algal1", "l2_flags.WATER AND l2_flags.PCD_15", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_ALGAL2_TSM_YS, "Water pixels flagged for invalid algal2 and yellow_subs and total_susp", "l2_flags.WATER AND (l2_flags.PCD_16 OR l2_flags.PCD_17)", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_PHOTOSYN_RAD, "Water pixels flagged for invalid PAR", "l2_flags.WATER AND l2_flags.PCD_18", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_TOA_VEG, "Land pixels flagged for invalid toa_veg", "l2_flags.LAND AND l2_flags.PCD_15", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_BOA_VEG, "Land pixels flagged for invalid boa_veg", "l2_flags.LAND AND l2_flags.PCD_17", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_RECT_REFL, "Land pixels flagged for invalid rectified reflectances", "l2_flags.LAND AND l2_flags.PCD_16", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_SURF_PRESS, "Land pixels flagged for invalid surf_press", "l2_flags.LAND AND l2_flags.PCD_18", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_AERO_PRODUCTS, "Land and water pixels flagged for invalid aero_alpha and aero_opt_thick_(i)", "l2_flags.PCD_19 AND (l2_flags.LAND OR l2_flags.WATER)", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_CLOUD_ALBEDO, "Cloud pixels flagged for invalid cloud_albedo", "l2_flags.CLOUD AND l2_flags.PCD_18", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_CLOUD_OPT_THICK_AND_TYPE, "Cloud pixels flagged for invalid cloud_opt_thick and cloud_type", "l2_flags.CLOUD AND l2_flags.PCD_19", Color.black, 0.0F),
                        mask(BITMASKDEF_NAME_INVALID_CLOUD_TOP_PRESS, "Cloud pixels flagged for invalid cloud_top_press", "l2_flags.CLOUD AND l2_flags.PCD_15", Color.black, 0.0F),

                        // the conditions that limitate algorithms in pink
                        mask(BITMASKDEF_NAME_LOW_SUN, null, "l2_flags.LOW_SUN", new Color(153, 0, 153), 0.5F),
                        mask(BITMASKDEF_NAME_HIGH_GLINT, null, "l2_flags.HIGH_GLINT", new Color(204, 0, 204), 0.5F),
                        mask(BITMASKDEF_NAME_MEDIUM_GLINT, null, "l2_flags.MEDIUM_GLINT", new Color(255, 51, 255), 0.5F),
                        mask(BITMASKDEF_NAME_ICE_HAZE, null, "l2_flags.ICE_HAZE", Color.yellow, 0.5F),

                        // the flags for atmospheric correction in blue
                        mask(BITMASKDEF_NAME_ABSOA_DUST, null, "l2_flags.ABSOA_DUST", new Color(0, 204, 255), 0.5F),
                        mask(BITMASKDEF_NAME_BPAC_ON, null, "l2_flags.BPAC_ON", new Color(153, 255, 204), 0.5F),

                        // Case2 water flags in ochre
                        mask(BITMASKDEF_NAME_CASE2_S, null, "l2_flags.CASE2_S", new Color(255, 255, 153), 0.5F),
                        mask(BITMASKDEF_NAME_CASE2_ANOM, null, "l2_flags.CASE2_ANOM", new Color(153, 153, 0), 0.5F),
                        mask(BITMASKDEF_NAME_CASE2_Y, null, "l2_flags.CASE2_Y", new Color(204, 204, 0), 0.5F),

                        // Land product flags
                        mask(BITMASKDEF_NAME_SNOW_ICE, null, "l2_flags.SNOW_ICE", new Color(255, 255, 153), 0.5F),
                        mask(BITMASKDEF_NAME_DENSE_DARK_VEG, null, "l2_flags.DDV", new Color(51, 51, 255), 0.25F),
                        mask(BITMASKDEF_NAME_UNCERTAIN_AEROSOL_MODEL, null, "l2_flags.OOADB", new Color(0, 204, 0), 0.5F),
                        mask(BITMASKDEF_NAME_TOAVI_BRIGHT, null, "l2_flags.TOAVI_BRIGHT", new Color(255, 204, 204), 0.5F),
                        mask(BITMASKDEF_NAME_TOAVI_BAD, null, "l2_flags.TOAVI_BAD", new Color(255, 153, 102), 0.5F),
                        mask(BITMASKDEF_NAME_TOAVI_CSI, null, "l2_flags.TOAVI_CSI", new Color(255, 153, 0), 0.5F),
                        mask(BITMASKDEF_NAME_TOAVI_WS, null, "l2_flags.TOAVI_WS", new Color(204, 102, 0), 0.5F),
                        mask(BITMASKDEF_NAME_TOAVI_INVAL_REC, null, "l2_flags.TOAVI_INVAL_REC", new Color(153, 51, 0), 0.5F),
                        mask(BITMASKDEF_NAME_WHITE_SCATTERER, null, "l2_flags.WHITE_SCATTERER", new Color(204, 204, 255), 0.5F),

                        // L1b copied flags in magenta
                        mask(BITMASKDEF_NAME_COSMETIC, null, "l2_flags.COSMETIC", new Color(204, 153, 255), 0.5F),
                        mask(BITMASKDEF_NAME_SUSPECT, null, "l2_flags.SUSPECT", new Color(204, 102, 255), 0.5F),

                        // Product Confidence Flags
                        mask(BITMASKDEF_NAME_PCD_1_13, null, "l2_flags.PCD_1_13", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_14, null, "l2_flags.PCD_14", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_15, null, "l2_flags.PCD_15", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_16, null, "l2_flags.PCD_16", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_17, null, "l2_flags.PCD_17", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_18, null, "l2_flags.PCD_18", Color.red, 0.0F),
                        mask(BITMASKDEF_NAME_PCD_19, null, "l2_flags.PCD_19", Color.red, 0.0F),
                };
            } else {
                return new Mask[0];
            }
        } else {
            return new Mask[0];
        }
    }

    private float[] createFloatArray(Field field, float scale) {
        if (field != null) {
            float[] values = new float[field.getNumElems()];
            for (int i = 0; i < values.length; i++) {
                values[i] = field.getElemFloat(i) * scale;
            }
            return values;
        }
        return null;
    }

    private static boolean isOldLinearYellowSubstanceScaling(String bandName,
                                                             double scalingOffset,
                                                             double scalingFactor) {
        int rawValue = 1;
        double scaledValue = scalingOffset + rawValue * scalingFactor;
        return bandName.equals(EnvisatConstants.MERIS_L2_YELLOW_SUBST_BAND_NAME) && scaledValue >= 0.0;
    }

}

