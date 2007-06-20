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
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;


/**
 * The <code>AsarProductFile</code> is a specialization of the abstract <code>ProductFile</code> class for ENVISAT
 * ASAR data products.
 *
 * @author Norman Fomferra
 * @version $Revision: 1.3 $ $Date: 2007/02/09 09:55:12 $
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

    /**
     * Constructs a <code>MerisProductFile</code> for the given seekable data input stream. Attaches the
     * <code>LogSink</code> passed in to the object created. The <code>LogSink</code> can might be null.
     *
     * @param file            the abstract file path representation.
     * @param dataInputStream the seekable data input stream which will be used to read data from the product file.
     *
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
     *
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
     *
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
     */
    @Override
    protected void postProcessSPH(Map parameters) throws IOException {

        DSD[] mdsDsds = getValidDSDs(EnvisatConstants.DS_TYPE_MEASUREMENT);
        if (mdsDsds.length == 0) {
            throw new ProductIOException("no valid measurements datasets found in this ASAR product");
        }
        DSD dsdGeoLocationAds = getDSD("GEOLOCATION_GRID_ADS");
        if (dsdGeoLocationAds == null) {
            throw new ProductIOException("invalid product: missing DSD for dataset 'GEOLOCATION_GRID_ADS'"); /*I18N*/
        }

        sceneRasterHeight = mdsDsds[0].getNumRecords();
        sceneRasterWidth = getSPH().getParamInt("LINE_LENGTH");

        int locTiePointGridWidth = EnvisatConstants.ASAR_LOC_TIE_POINT_GRID_WIDTH;
        int locTiePointGridHeight = dsdGeoLocationAds.getNumRecords();

        locTiePointGridOffsetX = (float) EnvisatConstants.ASAR_LOC_TIE_POINT_OFFSET_X;
        locTiePointGridOffsetY = (float) EnvisatConstants.ASAR_LOC_TIE_POINT_OFFSET_Y;
        locTiePointSubSamplingX = (float) sceneRasterWidth / ((float) EnvisatConstants.ASAR_LOC_TIE_POINT_GRID_WIDTH - 1f);
        locTiePointSubSamplingY = (float) sceneRasterHeight / (float) dsdGeoLocationAds.getNumRecords();
        // @todo 1 nf/** - ASAR

//        locTiePointSubSamplingY = (float) sceneRasterHeight / ((float) dsdGeoLocationAds.getNumRecords() - 1f);

        // Note: the following parameters are NOT used in the DDDB anymore
        // They are provided here for debugging purposes only.
        //
        parameters.put("sceneRasterWidth", new Integer(sceneRasterWidth));
        parameters.put("sceneRasterHeight", new Integer(sceneRasterHeight));
        parameters.put("locTiePointGridWidth", new Integer(locTiePointGridWidth));
        parameters.put("locTiePointGridHeight", new Integer(locTiePointGridHeight));
        parameters.put("locTiePointGridOffsetX", new Float(locTiePointGridOffsetX));
        parameters.put("locTiePointGridOffsetY", new Float(locTiePointGridOffsetY));
        parameters.put("locTiePointSubSamplingX", new Float(locTiePointSubSamplingX));
        parameters.put("locTiePointSubSamplingY", new Float(locTiePointSubSamplingY));

        String prod_type = getSPH().getParamString("SPH_DESCRIPTOR");
        if (prod_type != null) {
            chronologicalOrder = prod_type.indexOf("Geocoded") == -1;
        }

        String firstMDSName = mdsDsds[0].getDatasetName();
        sceneRasterStartTime = getRecordTime(firstMDSName, "zero_doppler_time", 0);
        sceneRasterStopTime = getRecordTime(firstMDSName, "zero_doppler_time", sceneRasterHeight - 1);
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
     *
     * @return a new default set, an empty array if no default set is given for this product type, never
     *         <code>null</code>.
     */
    @Override
    public BitmaskDef[] createDefaultBitmaskDefs(String dsName) {
        return new BitmaskDef[0];
    }
}
