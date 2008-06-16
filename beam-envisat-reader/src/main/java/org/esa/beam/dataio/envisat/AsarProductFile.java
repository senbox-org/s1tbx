/*
 * $Id: AsarProductFile.java,v 1.3 2007/02/09 09:55:12 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.envisat;

import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.StringUtils;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;


/**
 * The <code>AsarProductFile</code> is a specialization of the abstract <code>ProductFile</code> class for ENVISAT
 * ASAR data products.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see org.esa.beam.dataio.envisat.ProductFile
 */
public class AsarProductFile extends ProductFile {

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
        VERSION_UNKNOWN, ASAR_3K, ASAR_4A, ASAR_4B
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
     * @see org.esa.beam.dataio.envisat.ProductFile#getSceneRasterWidth()
     */
    @Override
    public int getSceneRasterWidth() {
        return sceneRasterWidth;
    }

    /**
     * Overrides the base class method.
     *
     * @see org.esa.beam.dataio.envisat.ProductFile#getSceneRasterHeight()
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
     * @see org.esa.beam.dataio.envisat.ProductFile#getTiePointSubSamplingX(int)
     */
    @Override
    public float getTiePointSubSamplingX(int gridWidth) {
        return locTiePointSubSamplingX;
    }

    /**
     * Overrides the base class method.
     *
     * @param gridWidth for AATSR products, this is the number of tie points in a tie point ADSR
     * @see org.esa.beam.dataio.envisat.ProductFile#getTiePointSubSamplingY(int)
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
     *         GADS.
     */
    @Override
    public String getGADSName() {
        // @todo 1 nf/tb - check: are there really no GADS for any ASAR? If so, add to API doc. why null is returned
        return null;
    }

    /**
     * This method is called after the main product header has been read in successfully.
     * <p/>
     * <p> Sub-classes should set product specific parameters in the <code>parameters</code> argument. The parameters
     * can be referenced in DDDB in order to implement dynamic field length, such as 'LINE_WIDTH'.
     * <p/>
     * <p> When this method is called, the <code>getMPH()</code> method returns a non-null value.
     * <p/>
     * <p> The default implementation is empty.
     *
     * @param parameters product specific parameters (possibly referenced within in the DDDB
     * @throws java.io.IOException if a header format error was detected or if an I/O error occurs
     */
    protected void postProcessMPH(Map parameters) throws IOException {
        _ioddVersion = IODD.VERSION_UNKNOWN;
    }

    /**
     * This method is called after the main product header has been read in successfully.
     * <p/>
     * <p> Sub-classes should set product specific parameters in the <code>parameters</code> argument. The parameters
     * can be referenced in DDDB in order to implement dynamic field length, such as 'LINE_WIDTH'.
     * <p/>
     * <p> When this method is called, the <code>getMPH()</code> method returns a non-null value.
     * <p/>
     * <p> The default implementation is empty.
     *
     * @param parameters product specific parameters (possibly referenced within in the DDDB
     */
    @Override
    protected void postProcessSPH(Map parameters) throws IOException {

        DSD[] mdsDsds = getValidDSDs(EnvisatConstants.DS_TYPE_MEASUREMENT);
        if (mdsDsds.length == 0) {
            throw new ProductIOException("no valid measurements datasets found in this ASAR product");
        }

        setIODDVersion();

        DSD dsdGeoLocationAds = getDSD("GEOLOCATION_GRID_ADS");
        if (dsdGeoLocationAds == null) {
            throw new ProductIOException("invalid product: missing DSD for dataset 'GEOLOCATION_GRID_ADS'"); /*I18N*/
        }

        sceneRasterHeight = mdsDsds[0].getNumRecords();
        if (getProductType().equals("ASA_WVI_1P") || getProductType().equals("ASA_WVW_1P") ||
                getProductType().equals("ASA_WVS_2P"))
            sceneRasterWidth = -1;
        else
            sceneRasterWidth = getSPH().getParamInt("LINE_LENGTH");

        if (sceneRasterWidth < 0) {                              // handle WSS where LINE_LENGTH is -1
            int maxHeight = mdsDsds[0].getNumRecords();
            int maxWidth = mdsDsds[0].getRecordSize();
            for (DSD mdsDsd : mdsDsds) {

                if (mdsDsd.getNumRecords() > maxHeight)
                    maxHeight = mdsDsd.getNumRecords();
                if (mdsDsd.getRecordSize() > maxWidth)
                    maxWidth = mdsDsd.getRecordSize();
            }
            sceneRasterHeight = maxHeight;
            sceneRasterWidth = maxWidth / 5;
        }

        int locTiePointGridWidth = EnvisatConstants.ASAR_LOC_TIE_POINT_GRID_WIDTH;
        int locTiePointGridHeight = dsdGeoLocationAds.getNumRecords();

        locTiePointGridOffsetX = EnvisatConstants.ASAR_LOC_TIE_POINT_OFFSET_X;
        locTiePointGridOffsetY = EnvisatConstants.ASAR_LOC_TIE_POINT_OFFSET_Y;
        locTiePointSubSamplingX = (float) sceneRasterWidth / ((float) EnvisatConstants.ASAR_LOC_TIE_POINT_GRID_WIDTH - 1f);
        locTiePointSubSamplingY = (float) sceneRasterHeight / (float) dsdGeoLocationAds.getNumRecords();
        // @todo 1 nf/** - ASAR

        //locTiePointSubSamplingY = (float) sceneRasterHeight / ((float) dsdGeoLocationAds.getNumRecords() - 1f);

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

        String prod_type = getSPH().getParamString("SPH_DESCRIPTOR");
        if (prod_type != null) {
            chronologicalOrder = prod_type.indexOf("Geocoded") == -1;
        }

        String firstMDSName = mdsDsds[0].getDatasetName();
        if (!isValidDatasetName(firstMDSName)) {
            firstMDSName = firstMDSName.replace(' ', '_');
        }
        sceneRasterStartTime = getRecordTime(firstMDSName, "zero_doppler_time", 0);
        sceneRasterStopTime = getRecordTime(firstMDSName, "zero_doppler_time", sceneRasterHeight - 1);
    }

    // @todo tb/** check with IODD if this is correct behaivoiur - right now implements the current global behaviour.
    void setInvalidPixelExpression(Band band) {
        if (band.getName().startsWith("reflec_")) {
            band.setNoDataValueUsed(true);
            band.setNoDataValue(0);
        } else {
            band.setNoDataValueUsed(false);
            band.setNoDataValue(0);
        }
    }

    IODD getIODDVersion() {
        if (_ioddVersion == IODD.VERSION_UNKNOWN) {
            setIODDVersion();
        }
        return _ioddVersion;
    }

    /**
     * Sets the IODD version which is an indicator for the product format.
     * <p/>
     * REF_DOC from version 3H on end with 3H, 3K, 4A, 4B
     * Software can be at least ASAR, NORUT, KSPT_L1B
     * <p/>
     * 3H ASAR/3.05, ASAR/3.06, ASAR/3.08
     * 4A ASAR/4.01, ASAR/4.02, ASAR/4.04
     * 4B ASAR/4.05
     */
    private void setIODDVersion() {

        Header mph = getMPH();
        try {

            String refDoc = mph.getParamString("REF_DOC").toUpperCase().trim();
            if (refDoc.endsWith("4B") || refDoc.endsWith("4/B")) {
                _ioddVersion = IODD.ASAR_4B;
            } else if (refDoc.endsWith("4A") || refDoc.endsWith("4/A")) {
                _ioddVersion = IODD.ASAR_4A;
            } else if (refDoc.endsWith("3K") || refDoc.endsWith("3/K")) {
                _ioddVersion = IODD.ASAR_3K;
            } else {
                char issueCh = refDoc.charAt(refDoc.length() - 2);
                if (Character.isDigit(issueCh)) {
                    int issue = Character.getNumericValue(issueCh);
                    if (issue >= 4) {
                        _ioddVersion = IODD.ASAR_4B;                             // catch future versions
                    }
                }
            }

            // if version not found from doc_ref then look at the software version
            if (_ioddVersion == IODD.VERSION_UNKNOWN) {

                String softwareVersion = mph.getParamString("SOFTWARE_VER").toUpperCase().trim();
                if (softwareVersion.startsWith("ASAR/3.")) {
                    String versionStr = softwareVersion.substring(5);
                    if (StringUtils.isNumeric(versionStr, Float.class)) {
                        float versionNum = Float.parseFloat(versionStr);
                        if (versionNum > 3.08)
                            _ioddVersion = IODD.ASAR_3K;
                    }
                } else if (softwareVersion.startsWith("ASAR/4.05")) {
                    _ioddVersion = IODD.ASAR_4B;
                } else if (softwareVersion.startsWith("ASAR/4.00") || softwareVersion.startsWith("ASAR/4.01") ||
                        softwareVersion.startsWith("ASAR/4.02") || softwareVersion.startsWith("ASAR/4.03") ||
                        softwareVersion.startsWith("ASAR/4.04")) {
                    _ioddVersion = IODD.ASAR_4A;
                } else {
                    char versionCh = softwareVersion.charAt(6);
                    if (Character.isDigit(versionCh)) {
                        int versionNum = Character.getNumericValue(versionCh);
                        if (versionNum >= 4)
                            _ioddVersion = IODD.ASAR_4B;
                        else
                            _ioddVersion = IODD.VERSION_UNKNOWN;
                    } else
                        _ioddVersion = IODD.VERSION_UNKNOWN;
                }
            }
        } catch (HeaderEntryNotFoundException e) {
            _ioddVersion = IODD.VERSION_UNKNOWN;
        }
    }

    /**
     * Gets the product type string as used within the DDDB, e.g. "MER_FR__1P_IODD5". This implementation considers
     * format changes in IODD 6.
     *
     * @return the product type string
     */
    protected String getDddbProductType() {
        // Debug.trace("MerisProductFile.getDddbProductType: IODD version still unknown");
        final String productType = getDddbProductTypeReplacement(getProductType(), getIODDVersion());
        return productType != null ? productType : super.getDddbProductType();
    }

    static String getDddbProductTypeReplacement(final String productType, final IODD ioddVersion) {
        String resource = productType;
        if (ioddVersion == IODD.ASAR_3K) {
            if (productDDExists(productType + IODD3K_SUFFIX))
                resource = productType + IODD3K_SUFFIX;
        } else if (ioddVersion == IODD.ASAR_4A) {
            if (productDDExists(productType + IODD4A_SUFFIX))
                resource = productType + IODD4A_SUFFIX;
            else if (productDDExists(productType + IODD3K_SUFFIX))
                resource = productType + IODD3K_SUFFIX;
        } else if (ioddVersion == IODD.ASAR_4B) {
            if (productDDExists(productType + IODD4B_SUFFIX))
                resource = productType + IODD4B_SUFFIX;
            else if (productDDExists(productType + IODD4A_SUFFIX))
                resource = productType + IODD4A_SUFFIX;
            else if (productDDExists(productType + IODD3K_SUFFIX))
                resource = productType + IODD3K_SUFFIX;
        } else if (ioddVersion == IODD.VERSION_UNKNOWN) {
            return productType;

        }
        return resource;
    }

    private static boolean productDDExists(String productType) {
        String productInfoFilePath = "products/" + productType + ".dd";
        return DDDB.databaseResourceExists(productInfoFilePath);
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
     * Returns a new default set of bitmask definitions for this product file.
     *
     * @param dsName the name of the flag dataset
     * @return a new default set, an empty array if no default set is given for this product type, never
     *         <code>null</code>.
     */
    @Override
    public BitmaskDef[] createDefaultBitmaskDefs(String dsName) {
        return new BitmaskDef[0];
    }


    /**
     * This method just delegates to
     * {@link BandInfo#BandInfo(String, int, int, int, int, float, float, String, FlagCoding, String, String, int, int)} to
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
                                   float scalingOffset,
                                   float scalingFactor,
                                   String validExpression,
                                   FlagCoding flagCoding,
                                   String physicalUnit,
                                   String description,
                                   String dataSetName) {

        int rasterHeight = sceneRasterHeight;
        int rasterWidth = sceneRasterWidth;

        if (getProductType().equals("ASA_WSS_1P")) {

            DSD[] mdsDsds = getValidDSDs(EnvisatConstants.DS_TYPE_MEASUREMENT);
            for (DSD mdsDsd : mdsDsds) {
                if (mdsDsd.getDatasetName().equals(dataSetName)) {
                    rasterHeight = mdsDsd.getNumRecords();
                    //rasterWidth = mdsDsd.getRecordSize() / 5;
                    break;
                }
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

}
