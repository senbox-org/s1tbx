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
import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The <code>ProductFile</code> class represents a single ENVISAT product file. Instances of this class represent
 * product files which have been opened for <i>read-only</i> access.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public abstract class ProductFile {

    /**
     * The key string for the sensing start value
     */
    public static final String KEY_SENSING_START = "SENSING_START";

    /**
     * The key string for the sensing stop value
     */
    public static final String KEY_SENSING_STOP = "SENSING_STOP";

    /**
     * The abstract file path representation.
     */
    private final File file;

    /**
     * The image seekable data input stream used to read data from ENVISAT products
     */
    private final ImageInputStream dataInputStream;

    /**
     * The logger
     */
    private final Logger logger;

    /**
     * The ENVISAT product's main product header (MPH)
     */
    private Header mph;

    /**
     * The ENVISAT product's specific product header (SPH)
     */
    private Header sph;

    /**
     * The array of an ENVISAT product's dataset descriptors (DSD)
     */
    private DSD[] dsdArray;

    /**
     * The ENVISAT GADS record.
     */
    private Record gads;

    /**
     * Product id as found in the MPH parameter "PRODUCT"
     */
    private String productId;

    /**
     * Product type. (first 10 characters of productId)
     */
    private String productType;

    /**
     * The total product size
     */
    private int productSize;

    /**
     * The total size of the SPH
     */
    private int sphSize;

    /**
     * The total size of a single DSD
     */
    private int dsdSize;

    /**
     * The number of DSDs
     */
    private int numDSDs;

    /**
     * The number of datasets
     */
    private int numDatasets;

    /**
     * The sensing start time
     */
    private Date sensingStart;

    /**
     * The sensing stop time
     */
    private Date sensingStop;

    /**
     * A cache for all record readers used so far
     */
    private final Map<String, RecordReader> recordReaderCache = new java.util.Hashtable<String, RecordReader>();

    /**
     * The parameter table for all product specific variables which have to be considered in the record infos read from
     * the DDDB.
     */
    private final Map parameters = new java.util.Hashtable();

    /**
     * An array containing all geophysical band readers for this product.
     */
    private BandLineReader[] bandLineReaders;

    /**
     * A bandName --> bandReader map for the geophysical bands contained in this product.
     */
    private Map<Band, BandLineReader> bandLineReaderMap;
    private String _productDescription;

    private final boolean lineInterleaved;

    /**
     * Constructs a <code>ProductFile</code> for the given seekable data input stream.
     *
     * @param file            the abstract file path representation.
     * @param dataInputStream the seekable data input stream which will be used to read data from the product file.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    protected ProductFile(File file, ImageInputStream dataInputStream) throws IOException {
        this(file, dataInputStream, false);
    }

    /**
     * Constructs a <code>ProductFile</code> for the given seekable data input stream.
     *
     * @param file            the abstract file path representation.
     * @param dataInputStream the seekable data input stream which will be used to read data from the product file.
     * @param lineInterleaved if true the Envisat file is expected to be in line interleaved storage format
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    protected ProductFile(File file, ImageInputStream dataInputStream, boolean lineInterleaved) throws IOException {
        Debug.assertTrue(dataInputStream != null);

        this.file = file;
        this.dataInputStream = dataInputStream;
        this.lineInterleaved = lineInterleaved;
        this.logger = SystemUtils.LOG;
        init();
    }

    /*
     * Opens an ENVISAT product file with the given file path and returns an
     * object representing the opened product file.
     *
     * @param filePath the path of the product file to be opened
     * @return an object representing the opened ENVISAT product file, never
     *         <code>null</code>
     * @exception IOException if an I/O error occurs
     */
    public static ProductFile open(String filePath) throws IOException {
        return open(new File(filePath));
    }

    /**
     * Opens an ENVISAT product file with the given file path and returns an object representing the opened product
     * file.
     *
     * @param file a path representation of the product file to be opened
     *
     * @return an object representing the opened ENVISAT product file, never <code>null</code>
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public static ProductFile open(File file) throws IOException {
        return open(file, new FileImageInputStream(file));
    }

    /**
     * Opens an ENVISAT product file for the given seekable data input stream and returns an object representing the
     * opened product file.
     * <p> This factory method automatically detects the ENVISAT product type and returns - if applicable - an
     * appropriate <code>ProductFile</code> subclass instance for it, otherwise a default <code>ProductFile</code>
     * instance is created.
     *
     * @param dataInputStream the seekable data input stream which will be used to read data from the product file.
     *
     * @return an object representing the opened ENVISAT product file, never <code>null</code>
     *
     * @throws java.io.IOException if an I/O error occurs
     * @throws java.lang.IllegalArgumentException
     *                             if an I/O error occurs
     */
    public static ProductFile open(ImageInputStream dataInputStream) throws IOException {
        return open(null, dataInputStream);
    }

    /**
     * @return the abstract file path representation.
     */
    public File getFile() {
        return file;
    }

    /**
     * Tests if the given file is an ENVISAT data product file.
     *
     * @param file the file to test
     *
     * @return <code>true</code> if so, <code>false</code> otherwise
     */
    public static boolean isEnvisatFile(File file) {
        return getProductType(file) != null;
    }

    /**
     * Gets the product identifier string as contained in the main product header (MPH).
     *
     * @return the file's product identifier, never or <code>null</code>
     */
    public String getProductId() {
        return productId;
    }

    /**
     * Gets the product type of the given ENVISAT product file. If the given file does not represent a path to a 'true'
     * ENVISAT product or if an I/O error occurs, the method returns <code>null</code>.
     *
     * @param file the ENVISAT product file
     *
     * @return the file's product type or <code>null</code> if the product type could not be retrieved.
     */
    public static String getProductType(File file) {
        ImageInputStream dataInputStream = null;
        String productType = null;

        if (!file.exists() || !file.isFile()) {
            return null;
        }

        try {
            dataInputStream = new FileImageInputStream(file);
            productType = getProductType(dataInputStream);
        } catch (IOException e) {
            Debug.trace(e);
        }

        try {
            if (dataInputStream != null) {
                dataInputStream.close();
            }
        } catch (IOException e) {
            Debug.trace(e);
        }

        return productType;
    }

    /**
     * Gets the product type of the given ENVISAT product file. If the given file does not represent a path to a 'true'
     * ENVISAT product or if an I/O error occurs, the method returns <code>null</code>.
     *
     * @param dataInputStream the data input stream
     *
     * @return the file's product type or <code>null</code> if the product type could not be retrieved.
     */
    public static String getProductType(ImageInputStream dataInputStream) {
        String productType = null;

        try {
            productType = readProductType(dataInputStream);
        } catch (IOException e) {
            // ignore
        }

        return productType;
    }

    /**
     * Gets the product type string, e.g. "MER_FR__1P".
     *
     * @return the product type string
     */
    public String getProductType() {
        return productType;
    }

    /**
     * Gets the  textual description for the product in this product file.
     *
     * @return a textual description for the product in this product file.
     */
    public String getProductDescription() {
        return _productDescription;
    }

    /**
     * Gets the product type string as used within the DDDB, e.g. "MER_FR__1P_iodd5". The default implementation simply
     * returns <code>getProductType()</code>
     *
     * @return the product type string
     */
    protected String getDddbProductType() {
        return getProductType();
    }

    /**
     * Gets the (sensing) start time associated with the first raster data line.
     *
     * @return the sensing start time, can be null e.g. for non-swath products
     */
    public abstract ProductData.UTC getSceneRasterStartTime();

    /**
     * Gets the (sensing) stop time associated with the first raster data line.
     *
     * @return the sensing stop time, can be null e.g. for non-swath products
     */
    public abstract ProductData.UTC getSceneRasterStopTime();

    /**
     * Gets the number of columns contained in the measuremet datasets (MDS) contained in this product. The value
     * returned is measured in pixels.
     * <p> Since the retrieval of the number of columns is product type dependend this method is abstract in order to
     * let a sub-class implement the retrieval.
     *
     * @return the number of columns measured in pixels.
     */
    public abstract int getSceneRasterWidth();

    /**
     * Gets the number of lines contained in the measuremet datasets (MDS) contained in this product. The value returned
     * is measured in pixels.
     * <p> Since the retrieval of the number of lines is product type dependend this method is abstract in order to let
     * a sub-class implement the retrieval.
     * <p> Another way to get the number of lines for a particular scene is to call the <code>getNumRecords</code>
     * method of the related <code>DSD</code> or <code>RecordReader</code>,
     *
     * @return the total number of lines contained in this data product
     *
     * @see org.esa.snap.dataio.envisat.DSD#getNumRecords()
     * @see org.esa.snap.dataio.envisat.RecordReader#getNumRecords()
     */
    public abstract int getSceneRasterHeight();

    /**
     * Gets the X-offset of the first (upper left) tie-point in the grid in the product's scene pixel co-ordinates.
     * <p> Since the retrieval of the X-offset of the tie-point grid is product type dependend this method is abstract
     * in order to let a sub-class implement the retrieval.
     *
     * @param gridWidth the tie-point grid width, ignored by other than AATSR products
     *
     * @return the total number of columns associated with one tie point measured in pixels
     */
    public abstract float getTiePointGridOffsetX(int gridWidth);

    /**
     * Gets the Y-offset of the first (upper left) tie-point in the grid in the product's scene pixel co-ordinates.
     * <p> Since the retrieval of the Y-offset of the tie-point grid is product type dependend this method is abstract
     * in order to let a sub-class implement the retrieval.
     *
     * @param gridWidth the tie-point grid width, ignored by other than AATSR products
     *
     * @return the total number of columns associated with one tie point measured in pixels
     */
    public abstract float getTiePointGridOffsetY(int gridWidth);

    /**
     * Gets the number of columns associated with one tie point. The value returned is measured in pixels. The method
     * returns the value <code>1</code> for data products not having tie point datasets.
     * <p> Since the retrieval of the number of columns associated with one tie point is product type dependend this
     * method is abstract in order to let a sub-class implement the retrieval.
     * <p> Valid ENVISAT data products must at least provide a number of
     * <pre> 1 + getRasterWidth() / getTiePointSubSamplingX() </pre>
     * tie points per annotation dataset record (ADSR).
     *
     * @param gridWidth the tie-point grid width, ignored by other than AATSR products
     *
     * @return the total number of columns associated with one tie point measured in pixels
     */
    public abstract float getTiePointSubSamplingX(int gridWidth);

    /**
     * Gets the number of lines (i.e. samples in vertical direction) associated with one tie point. The value returned
     * is measured in pixels. The method returns the value <code>1</code> for data products not having tie point
     * datasets.
     * <p> Since the retrieval of the number of lines contained in a data product is differs from pruct type to type
     * this method is abstract in order to let a sub-class implement the retrieval.
     * <p> Valid ENVISAT data products must at least provide a number of
     * <pre> 1 + getRasterHeight() / getTiePointSubSamplingY() </pre>
     * tie points per annotation dataset record (ADSR).
     *
     * @param gridWidth the tie-point grid width, ignored by other than AATSR products
     *
     * @return the total number of lines associated with one tie point measured in pixels
     */
    public abstract float getTiePointSubSamplingY(int gridWidth);

    /**
     * Determines whether the pixels of a the scan lines in this product data file are stored in chronological order have
     * to be flipped before they appear in "natural" way such that the first pixel of the first line is the most
     * north-west pixel.
     *
     * @return true, if so
     */
    public abstract boolean storesPixelsInChronologicalOrder();

    /**
     * Returns a new default set of mask definitions for this product file.
     *
     * @param flagDsName the name of the flag dataset
     *
     * @return a new default set, an empty array if no default set is given for this product type, never
     *         <code>null</code>.
     */
    public abstract Mask[] createDefaultMasks(String flagDsName);

    protected Mask mask(String name, String description, String expression, Color color, float transparency) {
        return Mask.BandMathsType.create(name, description, getSceneRasterWidth(), getSceneRasterHeight(), expression, color, transparency);
    }

    /**
     * Gets the seekable data input stream used to read data from the product file.
     *
     * @return the seekable data input stream
     */
    public ImageInputStream getDataInputStream() {
        return dataInputStream;
    }

    /**
     * @return the sensing-start time as a <code>Date</code> object.
     */
    public Date getSensingStart() {
        return sensingStart;
    }

    /**
     * @return the sensing-stop time as a <code>Date</code> object.
     */
    public Date getSensingStop() {
        return sensingStop;
    }

    /**
     * @return the total product size in bytes.
     */
    public int getProductSize() {
        return productSize;
    }

    /**
     * Gets the ENVISAT main product header (MPH).
     *
     * @return the ENVISAT main product header (MPH)
     */
    public Header getMPH() {
        return mph;
    }

    /**
     * Gets the ENVISAT specific product header (SPH).
     *
     * @return the ENVISAT specific product header (SPH)
     */
    public Header getSPH() {
        return sph;
    }

    /**
     * Gets the number of DSDs found in the product.
     *
     * @return the number of DSDs found in the product
     */
    public int getNumDSDs() {
        // Note: dsdArray.length is NOT returned here because it can be greater
        // than numDSDs!
        return numDSDs;
    }

    /**
     * Gets the DSD with the specified index.
     *
     * @param index the DSD index
     *
     * @return the DSD with the given index
     *
     * @throws java.lang.ArrayIndexOutOfBoundsException
     *          if the index is out of bounds
     */
    public DSD getDSDAt(int index) throws ArrayIndexOutOfBoundsException {
        return dsdArray[index];
    }

    /**
     * Gets the DSD for the dataset with the specified name.
     *
     * @param datasetName the dataset name
     *
     * @return the DSD for the dataset with the specified name or <code>null</code> if the DSD does not exist
     */
    public DSD getDSD(String datasetName) {
        final String dsdName = DDDB.getInstance().getDSDName(getDddbProductType(), datasetName);

        for (DSD theDSD : dsdArray) {
            if (theDSD.getDatasetName().equalsIgnoreCase(dsdName)) {
                return theDSD;
            }
        }
        return null;
    }

    /**
     * Gets the index of the DSD for the dataset with the specified name. The method performs a case-insensitive
     * search.
     *
     * @param datasetName tha dataset name
     *
     * @return the DSD index, <code>-1</code> if a DSD with the given name could not be found
     */
    public int getDSDIndex(String datasetName) {

        // Get the true DSD name from DDDB, this mapping is required
        // because, the given dataset name must not necessarily be equal
        // to the product-internal DSD name
        final String dsdName = DDDB.getInstance().getDSDName(getDddbProductType(), datasetName);

        final int numDSDs = getNumDSDs();
        for (int i = 0; i < numDSDs; i++) {
            if (getDSDAt(i).getDatasetName().equalsIgnoreCase(dsdName)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Gets all valid DSDs which have the given dataset type and whose related datasets are not empty in this product.
     *
     * @param datasetType the dataset type, must be one of the several <code>DS_TYPE_</code>X constants defined in the
     *                    <code>EnvisatConstant</code> interface.
     *
     * @return the array of adequate DSDs
     */
    public DSD[] getValidDSDs(char datasetType) {
        DSD[] dsds = null;
        for (int j = 0; j < 2; j++) {
            int n = 0;
            for (int i = 0; i < getNumDSDs(); i++) {
                DSD dsd = getDSDAt(i);
                if (dsd != null && dsd.getDatasetType() == datasetType && !dsd.isDatasetEmpty()) {
                    if (dsds != null) {
                        dsds[n] = dsd;
                    }
                    n++;
                }
            }
            if (j == 0) {
                dsds = new DSD[n];
            }
        }
        return dsds;
    }

    /**
     * Gets the ENVISAT global annotation dataset GADS (always a single record).
     *
     * @return the ENVISAT GADS record, or <code>null</code> if this product file does not have a GADS
     */
    public Record getGADS() {
        return gads;
    }

    /**
     * Gets the names of all valid (binary) datasets contained in this product file.
     *
     * @return the names of the datasets contained in this product file, never <code>null</code>
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public String[] getValidDatasetNames() throws IOException {
        return getValidDatasetNames(-1);
    }

    /**
     * Gets the names of all valid (binary) datasets contained in this product file.
     *
     * @param datasetType the dataset type, use -1 for all datasets otherwise see {@link EnvisatConstants}.DS_TYPE_X
     *
     * @return the names of the datasets contained in this product file, never <code>null</code>
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public String[] getValidDatasetNames(int datasetType) throws IOException {
        String[] datasetNames = DDDB.getInstance().getDatasetNames(getDddbProductType());
        ArrayList<String> nameList = new ArrayList<String>();
        for (String datasetName : datasetNames) {
            DSD dsd = getDSD(datasetName);
            if (dsd != null && !dsd.isDatasetEmpty()) {
                if (datasetType == -1 || datasetType == dsd.getDatasetType()) {
                    nameList.add(datasetName);
                }
            }
        }
        String[] validDatasetNames = new String[nameList.size()];
        nameList.toArray(validDatasetNames);
        return validDatasetNames;
    }

    /**
     * Tests if the given dataset name is a valid dataset name for this product file.
     *
     * @param name the dataset name
     *
     * @return <code>true</code>, if so
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public boolean isValidDatasetName(String name) throws IOException {
        String[] datasetNames = getValidDatasetNames();
        return StringUtils.containsIgnoreCase(datasetNames, name);
    }

    /**
     * Gets a record reader for the dataset with the given dataset name.
     * <p> Although the method does not access the product file at this moment, an I/O error can occur, since the method
     * accesses the internal dataset descriptor database (DDDB).
     *
     * @param datasetName the dataset name
     *
     * @return the record reader
     *
     * @throws java.io.IOException if an appropriate DSD could not be found in the product file
     * @throws org.esa.snap.dataio.envisat.DDDBException
     *                             if a database I/O error occurs
     */
    public RecordReader getRecordReader(String datasetName) throws IOException, DDDBException {

        // See if we have the record reader in the cache
        String datasetNameUC = datasetName.toUpperCase();
        RecordReader recordReader = recordReaderCache.get(datasetNameUC);
        if (recordReader != null) {
            return recordReader;
        }

        // Find appropriate DSD for dataset, note the DSDs come from the
        // product file itself
        DSD dsd = getDSD(datasetName);
        // Not found
        if (dsd == null) {
            throw new IllegalFileFormatException("no DSD found for dataset with name '" + datasetName + "'");
        }

        // Now get the record info for the dataset from the DDDB
        RecordInfo recordInfo = readRecordInfo(datasetName);
        if (recordInfo == null) {
            throw new DDDBException("no record info found for dataset with name '" + datasetName + "'");
        }

        // Create a new record reader...
        if (lineInterleaved && dsd.getDatasetType() == 'M') {
            recordReader = new LineInterleavedRecordReader(this, dsd, recordInfo);
        } else {
            recordReader = new RecordReader(this, dsd, recordInfo);
        }
        // ...and put it into the cache
        recordReaderCache.put(datasetNameUC, recordReader);

        return recordReader;
    }

    protected RecordInfo readRecordInfo(String datasetName) {
        return DDDB.getInstance().readRecordInfo(getDddbProductType(), datasetName, parameters);
    }

    public ProductData.UTC getRecordTime(final String datasetName,
                                         final String timeFieldName,
                                         int recordIndex) throws IOException {
        final RecordReader recordReader = getRecordReader(datasetName);
        final Record record = recordReader.readRecord(recordIndex);
        final Field timeField = record.getField(timeFieldName);
        final ProductData data = timeField.getData();
        return (data instanceof ProductData.UTC) ? (ProductData.UTC) data : null;
    }

    public ProductData.UTC[] getAllRecordTimes() throws IOException {
        String[] datasetNames = getValidDatasetNames(EnvisatConstants.DS_TYPE_MEASUREMENT);
        if(datasetNames.length < 1) {
            return new ProductData.UTC[0];
        }
        RecordReader recordReader = getRecordReader(datasetNames[0]);

        int fieldIndex = getDSRTimeInfoFieldIndex(recordReader);

        long fieldOffset = recordReader.getRecordInfo().getFieldOffset(fieldIndex);
        FieldInfo dsrTime = recordReader.getRecordInfo().getFieldInfoAt(fieldIndex);

        int numRecords = recordReader.getNumRecords();
        ProductData.UTC[] recordTimes = new ProductData.UTC[numRecords];
        for (int i = 0; i < recordTimes.length; i++) {
            Field field = dsrTime.createField();
            recordReader.readFieldSegment(i, fieldOffset, 1, 0, 2, field);
            recordTimes[i] = (ProductData.UTC) field.getData();
        }

        return recordTimes;
    }

    /**
     * @param recordReader the record reader to provide the time info field.
     * @return the index of the time info field of the dataset record.
     */
    protected int getDSRTimeInfoFieldIndex(RecordReader recordReader) {
        return recordReader.getRecordInfo().getFieldInfoIndex("dsr_time");
    }


    /**
     * Gets the names of all bands contained in this data product file.
     *
     * @return an array of band names, never <code>null</code>. An empty array is returned, if this product doesn't
     *         support reading band data.
     */
    public String[] getBandNames() {
        BandLineReader[] bandLineReaders = getBandLineReaders();
        String[] bandNames = new String[bandLineReaders.length];
        for (int i = 0; i < bandNames.length; i++) {
            bandNames[i] = bandLineReaders[i].getBandName();
        }
        return bandNames;
    }

    /**
     * Gets the auto-grouping applicable to the data sets contained in this product file.
     * A given {@code pattern} parameter is a textual representation of the auto-grouping.
     * The syntax for the pattern is:
     * <pre>
     * pattern    :=  &lt;groupPath&gt; {':' &lt;groupPath&gt;} | "" (empty string)
     * groupPath  :=  &lt;groupName&gt; {'/' &lt;groupName&gt;}
     * groupName  :=  any non-empty string without characters ':' and '/'
     * </pre>
     * The default implementation returns the empty string.
     *
     * @return The auto-grouping pattern.
     *
     * @since BEAM 4.8
     */
    public String getAutoGroupingPattern() {
        return "";
    }

    /**
     * Gets an array containing all geophysical band readers for this product.
     *
     * @return an array of geophysical band readers, never <code>null</code>. An empty array is returned, if this
     *         product doesn't support reading band data
     */
    public BandLineReader[] getBandLineReaders() {
        if (bandLineReaders == null) {
            bandLineReaders = createBandLineReaders();
        }
        return bandLineReaders;
    }

    protected BandLineReader[] createBandLineReaders() {
        return DDDB.getInstance().getBandLineReaders(this);
    }

    /**
     * Gets a reader for the geophysical band.
     *
     * @param band the band
     * @return the geophysical band reader, or <code>null</code> if this product doesn't support reading band data or if
     *         the a band with the given name was not found
     */
    public synchronized BandLineReader getBandLineReader(final Band band) {
        if (bandLineReaderMap == null) {
            bandLineReaderMap = new java.util.Hashtable<>();
            BandLineReader[] bandLineReaders = getBandLineReaders();
            final Product product = band.getProduct();
            for (BandLineReader bandLineReader : bandLineReaders) {
                final String bandName = bandLineReader.getBandName();
                final Band key = product.getBand(bandName);
                if (key != null) {
                    bandLineReaderMap.put(key, bandLineReader);
                }
            }
        }
        return bandLineReaderMap.get(band);
    }

    /**
     * Closes this product file by closing the seekable data input stream used to read data from the product file.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public void close() throws IOException {
        synchronized (dataInputStream) {
            dataInputStream.close();
        }
    }

    /**
     * Gets the current logger.
     *
     * @return the logger, never null.
     */
    public Logger getLogger() {
        return logger;
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
    protected void postProcessMPH(Map parameters) throws IOException {
    }

    /**
     * This method is called after the specific product header has been read in successfully.
     * <p> Sub-classes should set product specific parameters in the <code>parameters</code> argument. The parameters
     * can be referenced in DDDB in order to implement dynamic field length, such as 'LINE_WIDTH'.
     * <p> When this method is called, the <code>getMPH()</code> and <code>getSPH()</code> methods return a non-null
     * value.
     * <p> The default implementation is empty.
     *
     * @param parameters product specific parameters as used in the DDDB
     *
     * @throws java.io.IOException if a header format error was detected or if an I/O error occurs
     */
    protected void postProcessSPH(Map parameters) throws IOException {
    }

    /**
     * Retrieves the file based MDSR index for the image line.
     * Default implementation returns always the line number requested, if different behaviour is required, please overwrite.
     *
     * @param lineIndex line (raster-y) index, zero based
     *
     * @return the mapped index, a mapped index < 0 means that the record is not present
     */
    int getMappedMDSRIndex(int lineIndex) {
        return lineIndex;
    }

    /**
     * Retrieves the raw pixel value to be filled in when a missing MDSR is supplied.
     *
     * @return the missing pixel value
     */
    double getMissingMDSRPixelValue() {
        return 0;
    }

    abstract void setInvalidPixelExpression(Band band);

    protected DSD[] getDsds() {
        return dsdArray;
    }

    /**
     * Returns the name of the GADS for this ENVISAT product file.
     *
     * @return the GADS name, or <code>null</code> if this product file does not have a GADS
     */
    public abstract String getGADSName();

    public abstract float[] getSpectralBandWavelengths();

    public abstract float[] getSpectralBandBandwidths();

    public abstract float[] getSpectralBandSolarFluxes();

    /**
     * Returns the names of all default bitmasks to be used for the band with the given name.
     *
     * @param bandName the band's name
     *
     * @return the array of bitmask names or null if no bitmasks are applicable
     */
    public String[] getDefaultBitmaskNames(String bandName) {
        return null;
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
     * @param scalingMethod     the scaling method.
     * @param scalingOffset     the scaling offset.
     * @param scalingFactor     the scaling factor.
     * @param validExpression   the valid expression.
     * @param flagCoding        the flag coding.
     * @param physicalUnit      the physical unit.
     * @param description       the description.
     * @param dataSetName       the name of the dataset.
     *
     * @return a newly created <code>BandInfo</code> object.
     */
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
                            getSceneRasterWidth(),
                            getSceneRasterHeight());
    }


    /**
     * Modifies the expression of a band if for example a band is renamed
     *
     * @param expression virtual band expression
     *
     * @return the new expression
     */
    public String updateExpression(String expression) {
        return expression;
    }

    //////////////////////////////////////////////////////////////////////////
    // Implementation helpers.
    //////////////////////////////////////////////////////////////////////////

    /**
     * Opens an ENVISAT product file for the given seekable data input stream and returns an object representing the
     * opened product file.
     * <p> This factory method automatically detects the ENVISAT product type and returns - if applicable - an
     * appropriate <code>ProductFile</code> subclass instance for it, otherwise a default <code>ProductFile</code>
     * instance is created.
     *
     * @param file            the Envisat product file
     * @param dataInputStream the seekable data input stream which will be used to read data from the product file.
     *
     * @return an object representing the opened ENVISAT product file, never <code>null</code>
     *
     * @throws java.io.IOException if an I/O error occurs
     * @throws java.lang.IllegalArgumentException
     *                             if an I/O error occurs
     */
    public static ProductFile open(File file, ImageInputStream dataInputStream) throws IOException {
        return open(file, dataInputStream, false);
    }

    /**
     * Opens an ENVISAT product file for the given seekable data input stream and returns an object representing the
     * opened product file.
     * <p> This factory method automatically detects the ENVISAT product type and returns - if applicable - an
     * appropriate <code>ProductFile</code> subclass instance for it, otherwise a default <code>ProductFile</code>
     * instance is created.
     *
     * @param file            the Envisat product file
     * @param dataInputStream the seekable data input stream which will be used to read data from the product file.
     * @param lineInterleaved if true the Envisat file is expected to be in line interleaved storage format
     *
     * @return an object representing the opened ENVISAT product file, never <code>null</code>
     *
     * @throws java.io.IOException if an I/O error occurs
     * @throws java.lang.IllegalArgumentException
     *                             if an I/O error occurs
     */
    public static ProductFile open(File file, ImageInputStream dataInputStream, boolean lineInterleaved) throws IOException {
        Guardian.assertNotNull("dataInputStream", dataInputStream);

        String productType = readProductType(dataInputStream);
        if (productType == null) {
            throw new ProductIOException("not an ENVISAT product or ENVISAT product type not supported");
        }
        // We use only the first 9 characters for comparision, since the 10th can be either 'P' or 'C'
        // and for auxiliary data it is 'A'
        String productTypeUC = productType.toUpperCase().substring(0, 9);

        ProductFile productFile = null;

        if (productTypeUC.startsWith("ME")) {
            productFile = new MerisProductFile(file, dataInputStream, lineInterleaved);
        } else if (productTypeUC.startsWith("AT")) {
            if (productTypeUC.startsWith("ATS_NL__0")) {
                productFile = new AatsrL0ProductFile(file, dataInputStream);
            }else if (productTypeUC.matches("ATS_..._A")) {
                productFile = new AatsrAuxProductFile(file, dataInputStream);
            }else {
                productFile = new AatsrProductFile(file, dataInputStream);
            }
        } else if (productTypeUC.startsWith("AS") || productTypeUC.startsWith("SA")) {
            if (productTypeUC.startsWith("ASA_XCA")) {
                productFile = new AsarXCAProductFile(file, dataInputStream);
            } else {
                productFile = new AsarProductFile(file, dataInputStream);
            }
        } else if (productTypeUC.startsWith("DOR")) {
            productFile = new DorisOrbitProductFile(file, dataInputStream);
        } else if (productTypeUC.startsWith("AUX")) {
            productFile = new AuxProductFile(file, dataInputStream);
        }

        if (productFile == null) {
            throw new ProductIOException("ENVISAT '" + productType + "' products are not supported");
        }

        return productFile;
    }


    /**
     * Initializes a product file object from the seekable data input stream.
     * <p> The method then calls the private <code>readMPH</code> and <code>readSPH</code> in sequence.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    private void init() throws IOException {

        synchronized (dataInputStream) {
            readMPH();
            postProcessMPH(parameters);

            readSPH();
            postProcessSPH(parameters);
        }

        traceDddbFieldSizeParameters();

        readGADS();

        _productDescription = DDDB.getInstance().getProductDescription(getDddbProductType());
    }

    /**
     * Implementation helper.
     * <p> Reads the MPH. The MPH is expected to have the size defined in the <code>EnvisatConstants.MPH_SIZE</code>
     * constant. <p> Internally the MPH is stored in the <code>mph</code> member.
     * <p>
     * The following MPH parameters have are used internally by this reader: <ld> <li><code>PRODUCT</code></li>
     * <li><code>TOT_SIZE</code></li> <li><code>SPH_SIZE</code></li> <li><code>DSD_SIZE</code></li>
     * <li><code>NUM_DSD</code></li> <li><code>NUM_DATA_SETS</code></li> <li><code>SENSING_START</code></li>
     * <li><code>SENSING_STOP</code></li> </ld>
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    private void readMPH() throws IOException {

        Debug.assertTrue(mph == null);

        byte[] mphBytes = new byte[EnvisatConstants.MPH_SIZE];
        dataInputStream.readFully(mphBytes);

        try {
            mph = HeaderParser.getInstance().parseHeader("MPH", mphBytes);
            Debug.trace("ProductFile: " + mph);
            productId = mph.getParamString("PRODUCT");
            productSize = mph.getParamInt("TOT_SIZE");
            sphSize = mph.getParamInt("SPH_SIZE");
            dsdSize = mph.getParamInt("DSD_SIZE");
            numDSDs = mph.getParamInt("NUM_DSD");
            numDatasets = mph.getParamInt("NUM_DATA_SETS");
            try {
                sensingStart = mph.getParamDate(KEY_SENSING_START);
            } catch (HeaderParseException e) {
                getLogger().warning("failed to parse header parameter 'SENSING_START': " + e.getMessage()+file.getAbsolutePath());
            }
            try {
                sensingStop = mph.getParamDate(KEY_SENSING_STOP);
            } catch (HeaderParseException e) {
                getLogger().warning("failed to parse header parameter 'SENSING_STOP': " + e.getMessage());
            }
        } catch (HeaderParseException e) {
            throw new IllegalFileFormatException(e.getMessage());
        }

        productType = productId.substring(0, EnvisatConstants.PRODUCT_TYPE_STRLEN).toUpperCase();
        if(productType != null && productType.endsWith("C")) {
            productType = productType.substring(0, productType.length()-1) + "P";
        }
        if (!productType.endsWith("P") && !productType.endsWith("X")) {
            final String newType = productType.substring(0, 9) + "P";
            getLogger().warning("mapping to regular product type '" + newType +
                                "' due to missing specification for products of type '" + productType + "'");
            productType = newType;
        }    
    }

    /**
     * Implementation helper.
     * <p>
     * Reads the SPH and all DSDs contained in it. <p> Internally the SPH is stored in the <code>sph</code> member
     * while each of the DSDs read in are stored in the internal <code>dsdArray</code> member.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    private void readSPH() throws IOException {
        Debug.assertTrue(sph == null);

        byte[] sphBytesAll = new byte[sphSize];
        dataInputStream.readFully(sphBytesAll);

        int sphSizeActual = getFirstDSDPos(sphBytesAll);
        byte[] sphBytes = new byte[sphSizeActual];
        System.arraycopy(sphBytesAll, 0, sphBytes, 0, sphSizeActual);

        int dsdCountValid;

        try {
            sph = HeaderParser.getInstance().parseHeader("SPH", sphBytes);
            Debug.trace("ProductFile: " + sph);

            dsdCountValid = 0;
            byte[] dsdBytes = new byte[dsdSize];
            dsdArray = new DSD[numDSDs];

            if (sphSizeActual + numDSDs * dsdSize > sphSize) {
                return;
            }

            for (int i = 0; i < numDSDs; i++) {
                System.arraycopy(sphBytesAll, sphSizeActual + i * dsdSize, dsdBytes, 0, dsdSize);
                String dsdKey = "DSD(" + (i + 1) + ")";
                Header dsd = HeaderParser.getInstance().parseHeader(dsdKey, dsdBytes);
                String dsName = dsd.hasParam("DS_NAME") ? dsd.getParamString("DS_NAME") : "";
                String dsType = dsd.hasParam("DS_TYPE") ? dsd.getParamString("DS_TYPE") + "?" : "?";
                String filename = dsd.hasParam("FILENAME") ? dsd.getParamString("FILENAME") : "";
                long dsOffset = dsd.hasParam("DS_OFFSET") ? dsd.getParamUInt("DS_OFFSET") : 0L;
                long dsSize = dsd.hasParam("DS_SIZE") ? dsd.getParamUInt("DS_SIZE") : 0L;
                int numDsr = dsd.hasParam("NUM_DSR") ? dsd.getParamInt("NUM_DSR") : 0;
                int dsrSize = dsd.hasParam("DSR_SIZE") ? dsd.getParamInt("DSR_SIZE") : 0;
                dsdArray[i] = new DSD(i,
                                      dsName.trim(),
                                      dsType.charAt(0),
                                      filename.trim(),
                                      dsOffset,
                                      dsSize,
                                      numDsr,
                                      dsrSize);
                Debug.trace("ProductFile: " + dsdArray[i]);
                dsdCountValid++;
            }
        } catch (HeaderParseException e) {
            throw new IllegalFileFormatException(e.getMessage());
        }

        numDSDs = dsdCountValid;

        if (numDSDs < numDatasets) {
            StringBuffer message = new StringBuffer();
            message.append("unsufficient number of DSDs found: minimum should be ");/*I18N*/
            message.append(numDatasets);
            message.append(", but found only "); /*I18N*/
            message.append(numDSDs);
            getLogger().warning(message.toString());
        }
    }


    /**
     * Implementation helper.
     * <p>
     * Reads the single, product type specific GADS record.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    private void readGADS() throws IOException {
        String gadsName = getGADSName();
        if (gadsName != null && isValidDatasetName(gadsName)) {
            gads = getRecordReader(gadsName).readRecord();
        } else {
            gads = null;
        }
        Debug.trace("ProductFile: GADS = " + gads);
    }

    /**
     * Reads the product type string from the product file.
     *
     * @param dataInputStream the seekable data input stream which will be used to read data from the product file.
     *
     * @return the product type string or <code>null</code> if the input stream was not opened for an ENVISAT product
     *         (file magic 'PRODUCT=' not found).
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    static String readProductType(final ImageInputStream dataInputStream)
            throws IOException {

        final byte[] magicBytes = new byte[EnvisatConstants.MAGIC_STRING.length() +
                                           EnvisatConstants.PRODUCT_TYPE_STRLEN];
        String productType = null;

        //if (dataInputStream.length() >= magicBytes.length) {
        dataInputStream.seek(0);
        dataInputStream.mark();
        dataInputStream.readFully(magicBytes);
        dataInputStream.reset();

        final String magicString = new String(magicBytes).toUpperCase();
        if (magicString.startsWith(EnvisatConstants.MAGIC_STRING)) {
            productType = magicString.substring(EnvisatConstants.MAGIC_STRING.length());
        }
        //}

        if(productType != null && productType.endsWith("C")) {
            productType = productType.substring(0, productType.length()-1) + "P";
        }

        return productType;
    }


    private static int getFirstDSDPos(final byte[] sphBytes) {
        return new String(sphBytes).toUpperCase().indexOf("DS_NAME=");
    }

    private void traceDddbFieldSizeParameters() {
        Iterator it = parameters.keySet().iterator();
        Debug.trace("ProductFile: DDDB field size parameters = {");
        while (it.hasNext()) {
            Object name = it.next().toString();
            Object value = parameters.get(name);
            Debug.trace("  " + name + " = " + value);
        }
        Debug.trace("}");
    }

    /**
     * Allow the productFile to add any other metadata not defined in dddb
     *
     * @param product the product
     *
     * @throws IOException if reading from files
     */
    protected void addCustomMetadata(Product product) throws IOException {

    }

}




