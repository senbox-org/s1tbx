package org.esa.beam.dataio.envisat;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.StringUtils;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;


/**
 * The <code>AsarProductFile</code> is a specialization of the abstract <code>ProductFile</code> class for ENVISAT
 * ASAR data products.
 */
public class DorisOrbitProductFile extends ProductFile {

    private final static String ORBIT_RECORD_NAME = "DORIS_Orbit";

    /**
     * Constructs a <code>MerisProductFile</code> for the given seekable data input stream. Attaches the
     * <code>LogSink</code> passed in to the object created. The <code>LogSink</code> can might be null.
     *
     * @param file            the abstract file path representation.
     * @param dataInputStream the seekable data input stream which will be used to read data from the product file.
     * @throws java.io.IOException if an I/O error occurs
     */
    protected DorisOrbitProductFile(File file, ImageInputStream dataInputStream) throws IOException {
        super(file, dataInputStream);
    }

    /**
     * Returns the name of the GADS for this ENVISAT product file.
     *
     * @return the GADS name "VISIBLE_CALIB_COEFS_GADS", or <code>null</code> if this product file does not have a
     *         GADS.
     */
    @Override
    public String getGADSName() {
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

        DSD dsd = getDSDAt(0);
        if(dsd != null) {
            int num = dsd.getNumRecords();
            parameters.put("NUM_DSR", num);
        }
    }

    public Record readerOrbitData() throws IOException
    {
        return getRecordReader(ORBIT_RECORD_NAME).readRecord();
    }

    @Override
    public String getProductType() {
        return getProductId().substring(0, EnvisatConstants.PRODUCT_TYPE_STRLEN).toUpperCase();
    }

    @Override
    public boolean isValidDatasetName(String name) throws IOException {
        String[] datasetNames = getValidDatasetNames();
        return name.equalsIgnoreCase(getGADSName()) || StringUtils.containsIgnoreCase(datasetNames, name);
    }


    @Override
    public void setInvalidPixelExpression(Band band) {
        band.setNoDataValueUsed(false);
        band.setNoDataValue(0);
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
     * Gets the (sensing) start time associated with the first raster data line.
     *
     * @return the sensing start time, can be null e.g. for non-swath products
     */
    public ProductData.UTC getSceneRasterStartTime() {
        return null;
    }

    /**
     * Gets the (sensing) stop time associated with the first raster data line.
     *
     * @return the sensing stop time, can be null e.g. for non-swath products
     */
    public ProductData.UTC getSceneRasterStopTime() {
        return null;
    }

    public int getSceneRasterWidth() {
        return 0;
    }

    public int getSceneRasterHeight() {
        return 0;
    }

    public float getTiePointGridOffsetX(int gridWidth) {
        return 0;
    }

    public float getTiePointGridOffsetY(int gridWidth) {
        return 0;
    }

    public float getTiePointSubSamplingX(int gridWidth) {
        return 0;
    }

    public float getTiePointSubSamplingY(int gridWidth) {
        return 0;
    }

    public boolean storesPixelsInChronologicalOrder() {
        return false;
    }
}
