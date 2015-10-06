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
package org.esa.snap.dataio.envisat;

import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.io.CsvReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * A <code>DDDB</code> instance is used read record infos from the ENVISAT data description database (DDDB).
 * <p> Note this class has public access as a side-effect of the implementation.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see org.esa.snap.dataio.envisat.RecordInfo
 */
public class DDDB {

    public static final String SMODEL_1OF1_NAME = "1OF1";
    public static final String SMODEL_1OF2_NAME = "1OF2";
    public static final String SMODEL_2OF2_NAME = "2OF2";
    public static final String SMODEL_2TOF_NAME = "2TOF";
    public static final String SMODEL_3TOI_NAME = "3TOI";

    public static final String SCALE_LINEAR_NAME = "Linear_Scale";
    public static final String SCALE_LOG_NAME = "Log_Scale";

    public static final String EXPRESSION_PREFIX = "=";

    private static final Map<String, URL> resourceMap = new HashMap<String, URL>(100);

    private static URL emptyURL;
    static {
        try {
            emptyURL = new URL("file:/empty");
        } catch(MalformedURLException e) {
            //ignore
            e.printStackTrace();
        }
    }

    /**
     * The database path.
     */
    public static final String DB_DIR_PATH = "/org/esa/snap/resources/dddb";

    /**
     * Separator for record fields is the pipe '|' character.
     */
    public static final char[] FIELD_SEPARATORS = {'|'};

    /**
     * Separator for field diemension size multiplicators is the comma ',' or the asterisk '*' character.
     */
    public static final char[] DIMSIZE_SEPARATORS = {',', '*'};

    /**
     * The null value string is the asterisk '*' character.
     */
    public static final String NULL_VALUE_STRING = "*";

    /**
     * The index of the first binary dataset in product-info files.
     */
    private final int _firstBinaryDsIndex = 3;

    /**
     * A cache containing a (productType) --> (ProductInfo instance) mapping
     */
    private final Map _productInfoCache = new java.util.Hashtable(16);

    /**
     *@link dependency
     */
    /*#RecordInfo lnkRecordInfo;*/

    /**
     *@link dependency
     */
    /*#FieldInfo lnkFieldInfo;*/

    /**
     * Gets the singleton instance of the RecordInfoDB.
     */
    public static DDDB getInstance() {
        return Holder.instance;
    }


    /**
     * Gets the names of all binary datasets expected to be contained in a product file of the given type.
     *
     * @param productType the product type identifier, e.g. "MER_FR__2P"
     *
     * @return an array of dataset names, never <code>null</code>
     *
     * @throws java.lang.IllegalArgumentException
     *          if the product type is null
     * @throws org.esa.snap.dataio.envisat.DDDBException
     *          if a database I/O error occurs
     */
    public String[] getDatasetNames(String productType)
            throws DDDBException {
        ProductInfo productInfo = getProductInfo(productType);
        Debug.assertNotNull(productInfo);
        int i0 = _firstBinaryDsIndex;
        int numBinaryDatasets = productInfo.datasetInfos.size() - i0;
        if (numBinaryDatasets < 0) {
            throw new DDDBException("illegal DDDB file content for product type '" + productType + "'"); /*I18N*/
        }
        String[] datasetNames = new String[numBinaryDatasets];
        for (int i = i0; i < productInfo.datasetInfos.size(); i++) {
            datasetNames[i - i0] = ((DatasetInfo) productInfo.datasetInfos.elementAt(i)).datasetName;
        }
        return datasetNames;
    }

    /**
     * Gets the product description string for the given product type identifier.
     *
     * @param productType the product type identifier, e.g. "MER_FR__2P"
     *
     * @return the description string for products of the given type
     *
     * @throws java.lang.IllegalArgumentException
     *          if the product type is null
     * @throws org.esa.snap.dataio.envisat.DDDBException
     *          if a database I/O error occurs
     */
    public String getProductDescription(String productType)
            throws DDDBException {
        ProductInfo productInfo = getProductInfo(productType);
        Debug.assertNotNull(productInfo);
        return productInfo.description;
    }

//    /**
//     * Gets the dataset description string for the given product type identifier
//     * and dataset name.
//     *
//     * @param productType the product type identifier, e.g. "MER_FR__2P"
//     * @param datasetName the dataset name
//     * @return the description string for datasets of the given product type and
//     *  the given dataset name, never <code>null</code>
//     * @throws java.lang.IllegalArgumentException if the product type or dataset name is null
//     * @throws org.esa.snap.dataio.envisat.DDDBException if a database I/O error occurs
//     */
//    public String getDatasetDescription(String productType, String datasetName)
//            throws DDDBException {
//        DatasetInfo datasetInfo = getDatasetInfo(productType, datasetName);
//        Debug.assertNotNull(datasetInfo);
//        return datasetInfo.description;
//    }


    /**
     * Gets the name of the DSD (as internally used in product data files) for the dataset with the given name.
     * <p>Note that the dataset name must not necessarily match the internally used DSD name.
     *
     * @param productType the product type identifier, e.g. "MER_FR__2P"
     * @param datasetName the dataset name
     *
     * @return the internal DSD name corresponding to the given dataset name, never <code>null</code>
     *
     * @throws org.esa.snap.dataio.envisat.DDDBException
     *          if a database I/O error occurs
     */
    public String getDSDName(String productType, String datasetName)
            throws DDDBException {
        DatasetInfo datasetInfo = getDatasetInfo(productType, datasetName);
        Debug.assertNotNull(datasetInfo);
        return datasetInfo.dsdName;
    }

    /**
     * Reads a record info from the database.
     * <p> The record is specified through the given product type (e.g. "MER_FR__2P") and a dataset name (e.g.
     * "Scaling_Factor_GADS"). Additional parameters are required for special product derivates such as 'child products'
     * in order to supply the true number of pixels in an image line.
     *
     * @param productType the product type id,
     * @param datasetName the dataset name
     * @param parameters  additional parameters possibly required to create the dataset info, can be <code>null</code>
     *                    for the majority of ENVISAT datasets.
     *
     * @return a new record info instance
     *
     * @throws org.esa.snap.dataio.envisat.DDDBException
     *          if a database I/O error occurs
     */
    public RecordInfo readRecordInfo(String productType, String datasetName, Map parameters) throws DDDBException {
        Guardian.assertNotNullOrEmpty("productType", productType);
        Guardian.assertNotNullOrEmpty("datasetName", datasetName);

        String recordInfoFilePath = getRecordInfoFilePath(productType, datasetName);

        return readRecordInfoPath(recordInfoFilePath, datasetName, parameters);
    }

    /**
     * Reads a record info from the database.
     * <p> The record is specified given the path to the *.dd file in the dddb resources.
     *
     * @param recordInfoFilePath a <code>String</code> holding the relative path to the *.dd file
     * @param datasetName        the dataset name
     * @param parameters         additional parameters possibly required to create the dataset info, can be
     *                           <code>null</code> for the majority of ENVISAT datasets.
     *
     * @return a new record info instance
     *
     * @throws org.esa.snap.dataio.envisat.DDDBException
     *          if a database I/O error occurs
     */
    private RecordInfo readRecordInfoPath(String recordInfoFilePath, String datasetName, Map parameters) throws
                                                                                                         DDDBException {
        URL url = getDatabaseResource(recordInfoFilePath);

        CsvReader csvReader = openCsvReader(url);

        RecordInfo recordInfo = null;

        String[] tokens;
        int lineIndex = 0;
        boolean subExpressionDetected = false;
        while ((tokens = readCsvRecord(csvReader, url)) != null) {

            if (isValidDataLine(tokens)) {
                if (lineIndex == 0) { // Header line
                    recordInfo = new RecordInfo(datasetName);
                } else {
                    String fieldName = null;
                    String dataTypeStr = null;
                    int dataType = 0;
                    int fieldSize = 0;
                    int numDataElems = 1;
                    String unit = "";
                    String description = "";
                    String token;
                    for (int columnIndex = 0; columnIndex < tokens.length; columnIndex++) {

                        token = tokens[columnIndex];                        
                        switch (columnIndex) {
                        case 0:
                            fieldName = token;
                            break;
                        case 1:
                            dataTypeStr = token;
                            if (dataTypeStr.startsWith("@+")) {
                                subExpressionDetected = true;
                            } else {
                                // standard
                                subExpressionDetected = false;
                                dataType = getFieldType(dataTypeStr);
                                if (dataType == ProductData.TYPE_UNDEFINED) {
                                    raiseSyntaxError(csvReader, url,
                                                     "undefined DDDB data type '" + token + "'"); /*I18N*/
                                }
                            }
                            break;
                        case 2:
                            unit = token;
                            break;
                        case 3:
                            // @todo 3 nf/nf - remove redundant DDDB column 4, field size is only required for type "spare" (check C-API too)
                            fieldSize = parseIntegerField(token, parameters);
                            break;
                        case 4:
                            numDataElems *= parseIntegerField(token, parameters);
                            break;
                        case 5:
                            // @todo 3 nf/nf - remove obsolete DDDB column 6, it is always "*" in all DDDB files (check C-API too)
                            numDataElems *= parseIntegerField(token, parameters);
                            break;
                        case 6:
                            description = token;
                            break;
                        }
                    }

                    if (fieldName != null) {
                        if (subExpressionDetected) {
                            // encountered subdataset defining a data structure to be loaded

                            String subDatasetPath = recordInfoFilePath.substring(0, recordInfoFilePath.lastIndexOf('/'));
                            subDatasetPath += '/' + dataTypeStr.substring(2, dataTypeStr.length());

                            RecordInfo subRecord = readRecordInfoPath(subDatasetPath, dataTypeStr, parameters);
                            String prefixStr = dataTypeStr.substring(2, dataTypeStr.length() - 3);
                            String prefix = prefixStr;
                            for (int n = 1; n <= numDataElems; n++) {

                                if (numDataElems > 1) {
                                    prefix = prefixStr + '.' + n;
                                }

                                recordInfo.add(subRecord, prefix);
                            }
                            recordInfo.updateSizeInBytes();
                        } else {
                            if (dataTypeStr.equalsIgnoreCase("Spare")) {
                                if (fieldSize > 0 && numDataElems == 1) {
                                    numDataElems = fieldSize;
                                    fieldSize = 1;
                                    Debug.trace(
                                            "DDDB: spares detected and adjusted: numDataElems = " + numDataElems); /*I18N*/
                                }
                            }
                            if (dataType != ProductData.TYPE_ASCII) {
                                if (fieldSize != FieldInfo.getDataTypeElemSize(dataType)) {
                                    closeCsvReader(csvReader, url);
                                    throw new DDDBException("DDDB integrity check failed: " /*I18N*/
                                                            + recordInfoFilePath
                                                            + ": field "
                                                            + fieldName
                                                            + ": field size mismatch: " /*I18N*/
                                                            + fieldSize
                                                            + " != "
                                                            + FieldInfo.getDataTypeElemSize(dataType));
                                }
                            } else {
                                Debug.trace("DDDB files ASCII - name: " + fieldName + " fieldSize: " + fieldSize);
                                // because of the String type handling in ASAR and AATSR
                                numDataElems = fieldSize;
                            }
                            Debug.assertTrue(recordInfo != null);
                            recordInfo.add(fieldName, dataType, numDataElems, unit, description);
                        }
                    }
                }

                lineIndex++;
            }
        }

        closeCsvReader(csvReader, url);

        if (recordInfo == null || recordInfo.getNumFieldInfos() == 0) {
            throw new DDDBException("database resource is empty: file " + recordInfoFilePath); /*I18N*/
        }

//        Debug.trace("DDDB: " + recordInfo.toString());

        return recordInfo;
    }

    /**
     * @throws org.esa.snap.dataio.envisat.DDDBException
     *          if a database I/O error occurs
     */
    public BandLineReader[] getBandLineReaders(ProductFile productFile) throws DDDBException {
        Guardian.assertNotNull("productFile", productFile);

        String gadsName = productFile.getGADSName();
        Record gadsRecord = productFile.getGADS();

        String filePath = "bands/" + productFile.getDddbProductType() + ".dd";
        URL url = getDatabaseResource(filePath);

        CsvReader csvReader = openCsvReader(url);
        List readerList = new Vector();

        String[] tokens;
        int lineIndex = 0;
        while ((tokens = readCsvRecord(csvReader, url)) != null) {

            if (isValidDataLine(tokens)) {
                if (lineIndex == 0) { // Header line
                } else {
                    if (tokens.length < 11) {
                        raiseSyntaxError(csvReader, url, "columns missing in record line"); /*I18N*/
                    }

                    String bandName = getTokenValue(tokens[0]);
                    String pixelDataRefStr = getTokenValue(tokens[1]);
                    String sampleModelName = getTokenValue(tokens[2]);
                    String bandDataTypeStr = getTokenValue(tokens[3]);
                    String spectrBandIndexStr = getTokenValue(tokens[4]);
                    String scalingMethodName = getTokenValue(tokens[5]);
                    String scalingOffsetStr = getTokenValue(tokens[6]);
                    String scalingFactorStr = getTokenValue(tokens[7]);
                    String bitmaskExpression = getTokenValue(tokens[8]);
                    String flagsFileRefStr = getTokenValue(tokens[9]);
                    String unit = getTokenValue(tokens[10]);
                    String description = getTokenValue(tokens[11]);

                    if (bandName == null || pixelDataRefStr == null || bandDataTypeStr == null) {
                        throw new DDDBException("malformed band info record in file '" + url + "'"); /*I18N*/
                    }

                    int bandDataType = getFieldType(bandDataTypeStr);
                    if (bandDataType == ProductData.TYPE_UNDEFINED) {
                        throw new DDDBException("invalid band datatype: " + bandDataTypeStr); /*I18N*/
                    }

                    int spectrBandIndex = -1;
                    if (spectrBandIndexStr != null) {
                        try {
                            spectrBandIndex = Integer.parseInt(spectrBandIndexStr);
                            spectrBandIndex--; // Def.: zero-based
                            if (spectrBandIndex < 0) {
                                throw new DDDBException("invalid spectral band index for band '" /*I18N*/
                                                        + bandName
                                                        + "'");
                            }
                        } catch (NumberFormatException e) {
                            throw new DDDBException("invalid spectral band index for band '" /*I18N*/
                                                    + bandName
                                                    + "'");
                        }
                    }

                    RecordReader pixelDataReader = null;
                    int pixelDataFieldIndex = -1;
                    String expression = null;
                    String dataSetName = null;

                    if (pixelDataRefStr.startsWith(EXPRESSION_PREFIX)) {
                        expression = pixelDataRefStr.substring(1).trim();
                    } else {
                        final FieldRef fieldRef = FieldRef.parse(pixelDataRefStr);
                        dataSetName = fieldRef.getDatasetName();
                        try {
                            pixelDataReader = productFile.getRecordReader(dataSetName);
                            pixelDataFieldIndex = fieldRef.getFieldIndex();
                        } catch (IOException e) {
                            Debug.trace("DDDB: " + e.getMessage());
                            continue;
                        }
                    }

                    int sampleModel = BandInfo.SMODEL_1OF1;
                    if (sampleModelName != null) {
                        sampleModel = getSampleModel(sampleModelName);
                        if (sampleModel == -1) {
                            throw new DDDBException("invalid sample model name: " + sampleModelName); /*I18N*/
                        }
                    }

                    double scalingOffset = 0.0;
                    double scalingFactor = 1.0;
                    int scalingMethod = BandInfo.SCALE_NONE;
                    if (scalingMethodName != null) {
                        if (scalingOffsetStr == null || scalingFactorStr == null) {
                            throw new DDDBException("malformed band scaling info in file '" + url + "'"); /*I18N*/
                        }
                        scalingMethod = getScalingMethod(scalingMethodName);
                        if (scalingMethod == -1) {
                            throw new DDDBException("invalid scaling method name: " + scalingMethodName); /*I18N*/
                        }
                        scalingOffset = resolveGadsValueDouble(scalingOffsetStr, gadsName, gadsRecord);
                        scalingFactor = resolveGadsValueDouble(scalingFactorStr, gadsName, gadsRecord);
                    }

                    FlagCoding flagsCoding = null;
                    if (flagsFileRefStr != null) {
                        if (flagsFileRefStr.startsWith("@/")) {
                            flagsFileRefStr = flagsFileRefStr.substring(2);
                        }
                        flagsCoding = readFlagsCoding(bandName, flagsFileRefStr);
                    }

                    final BandInfo bandInfo = productFile.createBandInfo(bandName,
                                                                         bandDataType,
                                                                         spectrBandIndex,
                                                                         sampleModel,
                                                                         scalingMethod,
                                                                         scalingOffset,
                                                                         scalingFactor,
                                                                         bitmaskExpression,
                                                                         flagsCoding,
                                                                         unit,
                                                                         description,
                                                                         dataSetName);

                    final BandLineReader bandLineReader;
                    if (pixelDataReader != null) {
                        bandLineReader = new BandLineReader(bandInfo, pixelDataReader, pixelDataFieldIndex);
                    } else {
                        bandLineReader = new BandLineReader.Virtual(bandInfo, productFile.updateExpression(expression));
                    }
                    readerList.add(bandLineReader);
                }

                lineIndex++;
            }
        }

        closeCsvReader(csvReader, url);

        BandLineReader[] readers = new BandLineReader[readerList.size()];
        for (int i = 0; i < readers.length; i++) {
            readers[i] = (BandLineReader) readerList.get(i);
        }

        return readers;
    }

    private static int getSampleModel(String sampleModelName) throws DDDBException {
        Guardian.assertNotNull("sampleModelName", sampleModelName);
        int sampleModel = -1;
        if (sampleModelName.equalsIgnoreCase(SMODEL_1OF1_NAME)) {
            sampleModel = BandInfo.SMODEL_1OF1;
        } else if (sampleModelName.equalsIgnoreCase(SMODEL_1OF2_NAME)) {
            sampleModel = BandInfo.SMODEL_1OF2;
        } else if (sampleModelName.equalsIgnoreCase(SMODEL_2OF2_NAME)) {
            sampleModel = BandInfo.SMODEL_2OF2;
        } else if (sampleModelName.equalsIgnoreCase(SMODEL_2TOF_NAME)) {
            sampleModel = BandInfo.SMODEL_2UB_TO_S;
        } else if (sampleModelName.equalsIgnoreCase(SMODEL_3TOI_NAME)) {
            sampleModel = BandInfo.SMODEL_3UB_TO_I;
        }
        return sampleModel;
    }

    private static int getScalingMethod(String scalingMethodName) {
        Guardian.assertNotNull("scalingMethodName", scalingMethodName);
        int scalingMethod = -1;
        if (scalingMethodName.equalsIgnoreCase(SCALE_LINEAR_NAME)) {
            scalingMethod = BandInfo.SCALE_LINEAR;
        } else if (scalingMethodName.equalsIgnoreCase(SCALE_LOG_NAME)) {
            scalingMethod = BandInfo.SCALE_LOG10;
        }
        return scalingMethod;
    }

    private static String getTokenValue(String token) {
        if (token == null || token.length() == 0 || token.equals("*")) {
            return null;
        }
        return token;
    }

    private static double resolveGadsValueDouble(String gadsRef, String gadsName, Record gadsRecord)
            throws DDDBException {
        Debug.assertNotNullOrEmpty(gadsRef);
        try {
            if (gadsName != null && gadsRef.startsWith(gadsName + ".")) {
                Debug.assertNotNull(gadsRecord);
                FieldRef fieldRef = FieldRef.parse(gadsRef);
                return gadsRecord.getFieldAt(fieldRef.getFieldIndex()).getElemDouble(fieldRef.getElemIndex());
            } else {
                return Double.parseDouble(gadsRef);
            }
        } catch (Exception e) {
            throw new DDDBException("failed to resolve GADS reference '" + gadsRef + "': " + e.getMessage()); /*I18N*/
        }
    }

    /**
     * Utility method which converts a field data type name as used in the DDDB into a unique integer identifier as
     * enumerated by the several <code>TYPE_</code>X constants in <code>ProductData</code>
     * interface.
     *
     * @param dataTypeName the data type name
     *
     * @return the data type ID as enumerated by the several <code>TYPE_</code>X constants in
     *         <code>ProductData</code> interface, can also be
     *         <code>TYPE_UNDEFINED</code> if the name is unknown
     *
     * @throws java.lang.IllegalArgumentException
     *          if the argument is <code>null</code>
     * @see #getFieldTypeName(int)
     */
    public static int getFieldType(String dataTypeName) {
        Guardian.assertNotNull("dataTypeName", dataTypeName);
        if (dataTypeName.equalsIgnoreCase("SChar")) {
            return ProductData.TYPE_INT8;
        }
        if (dataTypeName.equalsIgnoreCase("UChar")) {
            return ProductData.TYPE_UINT8;
        }
        if (dataTypeName.equalsIgnoreCase("SShort")) {
            return ProductData.TYPE_INT16;
        }
        if (dataTypeName.equalsIgnoreCase("UShort")) {
            return ProductData.TYPE_UINT16;
        }
        if (dataTypeName.equalsIgnoreCase("SLong")) {
            return ProductData.TYPE_INT32;
        }
        if (dataTypeName.equalsIgnoreCase("ULong")) {
            return ProductData.TYPE_UINT32;
        }
        if (dataTypeName.equalsIgnoreCase("Float")) {
            return ProductData.TYPE_FLOAT32;
        }
        if (dataTypeName.equalsIgnoreCase("Double")) {
            return ProductData.TYPE_FLOAT64;
        }
        if (dataTypeName.equalsIgnoreCase("String")) {
            return ProductData.TYPE_ASCII;
        }
        // @todo 3 nf/nf - possibly redefine the UTC type in DDDB (java.util.Date?)
        if (dataTypeName.equalsIgnoreCase("@/types/UTC.dd")) {
            return ProductData.TYPE_UTC;
        }
        // @todo 3 nf/nf - possibly remove this type from DDDB
        if (dataTypeName.equalsIgnoreCase("Spare")) {
            return ProductData.TYPE_INT8;
        }
        Debug.trace("DDDB: undefined datatype: " + dataTypeName);
        return ProductData.TYPE_UNDEFINED;
    }

    /**
     * Utility method which converts a data type ID to a data type name as used in the DDDB.
     *
     * @param dataType the data type name as enumerated by the several <code>TYPE_</code>X constants in
     *                 <code>ProductData</code> interface.
     *
     * @return a data type name, never <code>null</code>. If the type is <code>ProductData.TYPE_UNKNOWN</code>
     *         the string &quot;?&quot; is returned
     *
     * @throws java.lang.IllegalArgumentException
     *          if the given type is not a known type identifier
     * @see #getFieldType(java.lang.String)
     */
    public static String getFieldTypeName(int dataType) {
        switch (dataType) {
        case ProductData.TYPE_INT8:
            return "SChar";
        case ProductData.TYPE_UINT8:
            return "UChar";
        case ProductData.TYPE_INT16:
            return "SShort";
        case ProductData.TYPE_UINT16:
            return "UShort";
        case ProductData.TYPE_INT32:
            return "SLong";
        case ProductData.TYPE_UINT32:
            return "ULong";
        case ProductData.TYPE_FLOAT32:
            return "Float";
        case ProductData.TYPE_FLOAT64:
            return "Double";
        case ProductData.TYPE_UTC:
            return "@/types/UTC.dd";
        case ProductData.TYPE_ASCII:
            return "String";
        case ProductData.TYPE_UNDEFINED:
            return "?";
        default:
            throw new IllegalArgumentException("invalid data type ID: " + dataType); /*I18N*/
        }
    }

    /**
     * Gets the file path for the record info file which describes the record structure for the specified dataset.
     *
     * @param productType the product type identifier, e.g. "MER_FR__2P"
     * @param datasetName the dataset name, never <code>null</code>
     *
     * @return the file path for the record info file which describes the record structure for the specified dataset
     *
     * @throws org.esa.snap.dataio.envisat.DDDBException
     *          if a database I/O error occurs
     */
    private String getRecordInfoFilePath(String productType, String datasetName)
            throws DDDBException {
        DatasetInfo datasetInfo = getDatasetInfo(productType, datasetName);
        Debug.assertNotNull(datasetInfo);
        return datasetInfo.recordInfoFilePath;
    }

    /**
     * Gets product specific information from the database.
     *
     * @param productType the product type id
     *
     * @return a new product info instance, never <code>null</code>
     *
     * @throws java.lang.IllegalArgumentException
     *          if <code>productType</code> is null
     * @throws org.esa.snap.dataio.envisat.DDDBException
     *          if a database I/O error occurs
     */
    private ProductInfo getProductInfo(String productType) throws DDDBException {

        Guardian.assertNotNull("productType", productType);

        ProductInfo productInfo = (ProductInfo) _productInfoCache.get(productType);
        if (productInfo != null) {
            return productInfo;
        }

        productInfo = readProductInfo(productType);
        Debug.assertTrue(productInfo != null);
        _productInfoCache.put(productType, productInfo);

        return productInfo;
    }

    /**
     * Gets a dataset info from the database.
     * <p> The dataset is specified through the given product type (e.g. "MER_FR__2P") and a dataset name (e.g.
     * "Scaling_Factor_GADS").
     *
     * @param productType the product type id,
     * @param datasetName the dataset name
     *
     * @return a new dataset info instance, never <code>null</code>
     *
     * @throws java.lang.IllegalArgumentException
     *          if one of the parameters is null
     * @throws org.esa.snap.dataio.envisat.DDDBException
     *          if a database I/O error occurs
     */
    private DatasetInfo getDatasetInfo(String productType, String datasetName) throws DDDBException {

        Guardian.assertNotNull("productType", productType);
        Guardian.assertNotNull("datasetName", datasetName);

        ProductInfo productInfo = getProductInfo(productType);
        Debug.assertNotNull(productInfo);

        for (int i = 0; i < productInfo.datasetInfos.size(); i++) {
            DatasetInfo datasetInfo = (DatasetInfo) productInfo.datasetInfos.get(i);
            if (datasetInfo.datasetName.equalsIgnoreCase(datasetName)) {
                return datasetInfo;
            }
        }

        throw new DDDBException("DDDB dataset information not found: product type '" /*I18N*/
                                + productType
                                + "', dataset name '"
                                + datasetName
                                + "'");
    }

    /**
     * Reads product specific information from the database.
     *
     * @param productType the product type id
     *
     * @return a new product info instance, never <code>null</code>
     *
     * @throws org.esa.snap.dataio.envisat.DDDBException
     *          if a database I/O error occurs
     */
    private ProductInfo readProductInfo(String productType)
            throws DDDBException {

        Debug.assertNotNull(productType);

        String productInfoFilePath = "products/" + productType + ".dd";
        URL url = getDatabaseResource(productInfoFilePath);
        CsvReader csvReader = openCsvReader(url);

        String[] tokens;
        int lineIndex = 0;

        ProductInfo productInfo = null;

        while ((tokens = readCsvRecord(csvReader, url)) != null) {

            if (isValidDataLine(tokens)) {
                if (lineIndex == 0) { // Header line
                    String productName = null;
                    String description = null;
                    for (int columnIndex = 0; columnIndex < tokens.length; columnIndex++) {
                        String token = tokens[columnIndex];
                        switch (columnIndex) {
                        case 0:
                            productName = token;
                            break;
                        case 1:
                            description = token;
                            break;
                        }
                    }
                    if (productName != null) {
                        productInfo = new ProductInfo();
                        productInfo.productName = productName;
                        productInfo.description = description;
                    }
                } else {
                    String datasetName = null;
                    String recordInfoFilePath = null;
                    String dsdName = null;
                    String description = "";
                    for (int columnIndex = 0; columnIndex < tokens.length; columnIndex++) {
                        String token = tokens[columnIndex];
                        switch (columnIndex) {
                        case 0:
                            datasetName = token;
                            break;
                        case 1:
                            recordInfoFilePath = token;
                            if (recordInfoFilePath.startsWith("@/")) {
                                recordInfoFilePath = recordInfoFilePath.substring(2);
                            }
                            break;
                        case 2:
                            dsdName = token;
                            break;
                        case 3:
                            description = token;
                            break;
                        }
                    }

                    if (productInfo != null
                        && datasetName != null
                        && recordInfoFilePath != null
                        && dsdName != null) {

                        DatasetInfo datasetInfo = new DatasetInfo();
                        datasetInfo.datasetName = datasetName;
                        datasetInfo.recordInfoFilePath = recordInfoFilePath;
                        datasetInfo.dsdName = dsdName;
                        datasetInfo.description = description;

                        productInfo.datasetInfos.add(datasetInfo);
                    }
                }
            }
            lineIndex++;
        }

        closeCsvReader(csvReader, url);

        if (productInfo == null || productInfo.datasetInfos.size() == 0) {
            throw new DDDBException("DDDB resource is empty or invalid: file " + url); /*I18N*/
        }

//        Debug.trace("DDDB: " + productInfo.toString());

        return productInfo;
    }

    /**
     * Reads a flags coding DDDB table.
     *
     * @param filePath the file path (within the DDDB) of the flags file
     *
     * @return a new flags coding object
     *
     * @throws org.esa.snap.dataio.envisat.DDDBException
     *          if a database I/O error occurs
     */
    public FlagCoding readFlagsCoding(String bandName, String filePath) throws DDDBException {
        Guardian.assertNotNullOrEmpty("filePath", filePath);

        URL url = getDatabaseResource(filePath);
        CsvReader csvReader = openCsvReader(url);
        FlagCoding flagsCoding = null;

        String[] tokens;
        int lineIndex = 0;
        while ((tokens = readCsvRecord(csvReader, url)) != null) {

            if (isValidDataLine(tokens)) {
                if (lineIndex == 0) { // Header line
                    flagsCoding = new FlagCoding(bandName);
                } else {
                    String flagName = null;
                    String bitIndexStr;
                    int flagMask = 0;
                    String description = "";
                    String token;
                    for (int columnIndex = 0; columnIndex < tokens.length; columnIndex++) {
                        token = tokens[columnIndex];
                        switch (columnIndex) {
                        case 0:
                            flagName = token;
                            break;
                        case 1:
                            bitIndexStr = token;
                            flagMask = createFlagmask(bitIndexStr);
                            break;
                        case 2:
                            description = token;
                            break;
                        }
                    }

                    if (flagName != null) {
                        flagsCoding.addFlag(flagName, flagMask, description);
                    }
                }

                lineIndex++;
            }
        }

        closeCsvReader(csvReader, url);

        if (flagsCoding == null || flagsCoding.getNumAttributes() == 0) {
            throw new DDDBException("database resource is empty: file " + filePath); /*I18N*/
        }

        return flagsCoding;
    }

    private int createFlagmask(final String bitIndexStr) {
        int flagMask = 0;
        if (bitIndexStr.indexOf('&') == -1) {
            final int bitIndex = parseIntegerField(bitIndexStr, null);
            flagMask = 1 << bitIndex;
        } else {
            final StringTokenizer st = new StringTokenizer(bitIndexStr);
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token.indexOf('&') == -1) {
                    try {
                        final int bitIndex = parseIntegerField(token, null);
                        flagMask = flagMask | (1 << bitIndex);
                    } catch (NumberFormatException e) {
                        Debug.trace(e);
                    }
                }
            }
        }
        return flagMask;
    }

    public static boolean isInstalled() {
        try {
            getDatabaseResource("products/" + "MER_RR__1P.dd");
            return true;
        } catch (DDDBException e) {
            return false;
        }
    }

    static URL getDatabaseResource(String resourcePath) throws DDDBException {
        String databasePath = DB_DIR_PATH + '/' + resourcePath;
        Debug.trace("DDDB: searching for resource file '" + databasePath + "'"); /*I18N*/

        URL url = resourceMap.get(databasePath);
        if(url == null) {
            url = DDDB.class.getResource(databasePath);
            resourceMap.put(databasePath, url);
        }
        if (url == null || url == emptyURL) {
            throw new DDDBException("DDDB resource not found: missing file: " + databasePath); /*I18N*/
        }

        return url;
    }

    /**
     * Check if a resource exists
     * @param resourcePath path to resource
     * @return true if found
     */
    static boolean databaseResourceExists(String resourcePath) {
        String databasePath = DB_DIR_PATH + '/' + resourcePath;
        URL url = resourceMap.get(databasePath);
        if(url == null) {
            url = DDDB.class.getResource(databasePath);
            if(url != null)
                resourceMap.put(databasePath, url);
            else
                resourceMap.put(databasePath, emptyURL);
        }
        return url != null && url != emptyURL;
    }

    private static boolean isValidDataLine(String[] tokens) {
        Debug.assertNotNull(tokens);
        return tokens.length > 0 && !tokens[0].startsWith("#");
    }

    private static int parseIntegerField(String numFieldElemsStr, Map parameters)
            throws DDDBException {

        Debug.assertNotNull(numFieldElemsStr);

        if (numFieldElemsStr.length() == 0 || numFieldElemsStr.equals("*")) {
            return 1;
        }

        StringTokenizer st = new StringTokenizer(numFieldElemsStr, new String(DIMSIZE_SEPARATORS));
        String token;
        int numFieldElems = 1;
        int factor;
        while (st.hasMoreTokens()) {
            token = st.nextToken().trim();
            try {
                factor = Integer.parseInt(token);
            } catch (NumberFormatException e) {
                if (parameters != null) {
                    Object value = parameters.get(token);
                    if (value == null) {
                        throw new DDDBException("missing DDDB field size parameter '" + token + "'"); /*I18N*/
                    }
                    if (!(value instanceof Number)) {
                        throw new DDDBException(
                                "illegal DDDB field size parameter '" + token + "': not a number"); /*I18N*/
                    }
                    factor = ((Number) value).intValue();
                } else {
                    throw new DDDBException("invalid DDDB field size value '" + token + "'"); /*I18N*/
                }
            }
            numFieldElems *= factor;
        }

        return numFieldElems;
    }

    private static CsvReader openCsvReader(URL url) throws DDDBException {
        Debug.assertNotNull(url);
        try {
            return new CsvReader(new InputStreamReader(url.openStream()),
                                 FIELD_SEPARATORS);
        } catch (IOException e) {
            StringBuffer sb = new StringBuffer();
            sb.append("failed to open DDDB resource: "); /*I18N*/
            sb.append(e.getMessage());
            sb.append(": file ");
            sb.append(url);
            throw new DDDBException(sb.toString());
        }
    }

    private static void closeCsvReader(CsvReader csvReader, URL url) {
        Debug.assertNotNull(csvReader);
        Debug.assertNotNull(url);
        try {
            csvReader.close();
        } catch (IOException e) {
            Debug.trace("DDDB: I/O warning: failed to close DDDB file: " /*I18N*/
                        + url
                        + ": "
                        + e.getMessage());
        }
    }

    private String[] readCsvRecord(CsvReader csvReader, URL url) throws DDDBException {
        Debug.assertNotNull(csvReader);
        Debug.assertNotNull(url);
        try {
            return csvReader.readRecord();
        } catch (IOException e) {
            closeCsvReader(csvReader, url);
            StringBuffer sb = new StringBuffer();
            sb.append("failed to read from DDDB resource: "); /*I18N*/
            sb.append(e.getMessage());
            sb.append(": file ");
            sb.append(url);
            throw new DDDBException(sb.toString());
        }
    }

    private void raiseSyntaxError(CsvReader csvReader, URL url, String message) throws DDDBException {
        StringBuffer sb = new StringBuffer();
        sb.append(message);
        sb.append(": file ");
        sb.append(url);
        sb.append(", ");
        sb.append("line ");
        sb.append(csvReader.getLineNumber());
        throw new DDDBException(sb.toString());
    }

    /**
     * Constructs a new reader info database object.
     */
    private DDDB() {
    }

    /**
     * Represents a complete product info table found in a <code>./products/&lt;productName&gt;.dd<code> file of the
     * DDDB. This is an implementation data structure.
     */
    private static class ProductInfo {

        String productName;
        String description;
        Vector datasetInfos = new Vector();

        /**
         * Returns a string representation of this product-info which can be used for debugging purposes.
         */
        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("DDDB.ProductInfo[");
            sb.append("'");
            sb.append(productName);
            sb.append("','");
            sb.append(description);
            sb.append("',\n");
            for (int i = 0; i < datasetInfos.size(); i++) {
                sb.append("  ");
                sb.append(datasetInfos.get(i).toString());
                sb.append("\n");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    /**
     * Represents a single dataset info record found in a <code>./products/&lt;productName&gt;.dd<code> file of the
     * DDDB. This is an implementation data structure.
     */
    private static class DatasetInfo {

        String datasetName;
        String recordInfoFilePath;
        String dsdName;
        String description;

        /**
         * Returns a string representation of this dataset-info which can be used for debugging purposes.
         */
        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("DDDB.DatasetInfo[");
            sb.append("'");
            sb.append(datasetName);
            sb.append("','");
            sb.append(recordInfoFilePath);
            sb.append("','");
            sb.append(dsdName);
            sb.append("','");
            sb.append(description);
            sb.append("']");
            return sb.toString();
        }
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final DDDB instance = new DDDB();
    }

}

