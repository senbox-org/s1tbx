package org.esa.beam.dataio.envisat;

import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.StringUtils;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;


/**
 * The <code>AsarProductFile</code> is a specialization of the abstract <code>ProductFile</code> class for ENVISAT
 * ASAR data products.
 */
public class AsarXCAProductFile extends ProductFile {
    private final static String GADS_NAME = "auxiliary_data";

    enum IODD {
        VERSION_UNKNOWN, ASAR_4A
    }

    /**
     * The IODD version number.
     */
    private IODD _ioddVersion = IODD.VERSION_UNKNOWN;

    /**
     * Product type suffix for IODD-4A backward compatibility
     */
    private static final String IODD4A_SUFFIX = "_IODD_4A";

    /**
     * Constructs a <code>MerisProductFile</code> for the given seekable data input stream. Attaches the
     * <code>LogSink</code> passed in to the object created. The <code>LogSink</code> can might be null.
     *
     * @param file            the abstract file path representation.
     * @param dataInputStream the seekable data input stream which will be used to read data from the product file.
     * @throws java.io.IOException if an I/O error occurs
     */
    protected AsarXCAProductFile(File file, ImageInputStream dataInputStream) throws IOException {
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
        return GADS_NAME;
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
    @Override
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

        DSD[] mdsDsds = getValidDSDs(EnvisatConstants.DS_TYPE_GLOBAL_ANNOTATION);
        if (mdsDsds.length == 0) {
            throw new IllegalFileFormatException("no valid gloabal annotation datasets found in this ASAR product");
        }

        setIODDVersion();
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
            if (refDoc.endsWith("4A") || refDoc.endsWith("4/A")) {
                _ioddVersion = IODD.ASAR_4A;
            } else {
                char issueCh = refDoc.charAt(refDoc.length() - 2);
                if (Character.isDigit(issueCh)) {
                    int issue = Character.getNumericValue(issueCh);
                    if (issue >= 4) {
                        _ioddVersion = IODD.ASAR_4A;                             // catch future versions
                    }
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
    @Override
    protected String getDddbProductType() {
        final String productType = getDddbProductTypeReplacement(getProductType(), getIODDVersion());
        return productType != null ? productType : super.getDddbProductType();
    }

    static String getDddbProductTypeReplacement(final String productType, final IODD ioddVersion) {
        String resource = productType;
        if (ioddVersion == IODD.ASAR_4A) {
            if (productDDExists(productType + IODD4A_SUFFIX))
                resource = productType + IODD4A_SUFFIX;
        }
        return resource;
    }

    private static boolean productDDExists(String productType) {
        String productInfoFilePath = "products/" + productType + ".dd";
        return DDDB.databaseResourceExists(productInfoFilePath);
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
    @Override
    public ProductData.UTC getSceneRasterStartTime() {
        return null;
    }

    /**
     * Gets the (sensing) stop time associated with the first raster data line.
     *
     * @return the sensing stop time, can be null e.g. for non-swath products
     */
    @Override
    public ProductData.UTC getSceneRasterStopTime() {
        return null;
    }

    @Override
    public int getSceneRasterWidth() {
        return 0;
    }

    @Override
    public int getSceneRasterHeight() {
        return 0;
    }

    @Override
    public float getTiePointGridOffsetX(int gridWidth) {
        return 0;
    }

    @Override
    public float getTiePointGridOffsetY(int gridWidth) {
        return 0;
    }

    @Override
    public float getTiePointSubSamplingX(int gridWidth) {
        return 0;
    }

    @Override
    public float getTiePointSubSamplingY(int gridWidth) {
        return 0;
    }

    @Override
    public boolean storesPixelsInChronologicalOrder() {
        return false;
    }

    /**
     * Allow the productFile to add any other metadata not defined in dddb
     *
     * @param product the product
     * @throws IOException if reading from files
     */
    @Override
    protected void addCustomMetadata(Product product) throws IOException {

        MetadataElement root = product.getMetadataRoot();
        Record gads = getGADS();

        MetadataElement elem = EnvisatProductReader.createMetadataGroup("XCA", gads);
        root.addElement(elem);

    }
}
