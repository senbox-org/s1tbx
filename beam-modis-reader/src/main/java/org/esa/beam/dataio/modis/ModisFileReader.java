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
package org.esa.beam.dataio.modis;

import ncsa.hdf.hdflib.HDFConstants;
import org.esa.beam.dataio.modis.bandreader.ModisBandReader;
import org.esa.beam.dataio.modis.bandreader.ModisBandReaderFactory;
import org.esa.beam.dataio.modis.hdf.HdfAttributes;
import org.esa.beam.dataio.modis.hdf.HdfDataField;
import org.esa.beam.dataio.modis.hdf.HdfUtils;
import org.esa.beam.dataio.modis.hdf.IHDF;
import org.esa.beam.dataio.modis.hdf.lib.HDF;
import org.esa.beam.dataio.modis.productdb.*;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.util.math.Range;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

class ModisFileReader {

    private int _qcFileId;
    private Logger _logger;
    private HashMap _bandReaderMap;
    private ModisProductDb _prodDb;

    /**
     * Constructs the object with default parameters
     */
    public ModisFileReader() {
        _prodDb = ModisProductDb.getInstance();
        _logger = BeamLogManager.getSystemLogger();
        _bandReaderMap = new HashMap();
        _qcFileId = HDFConstants.FAIL;
    }

    /**
     * Adds all the bands, the tie point grids and the geocoding to the product.
     *
     * @param sdStart       the id of the HDF SDstart
     * @param globalAttribs the struct global attributes object
     * @param product       the product to be supplied with bands
     */
    public void addRastersAndGeocoding(final int sdStart, final ModisGlobalAttributes globalAttribs,
                                       final Product product) throws IOException {
        String productType = product.getProductType();
        if (globalAttribs.isImappFormat()) {
            productType += "_IMAPP";
        }
        addBandsToProduct(sdStart, productType, product);
        if (ModisConstants.EOS_TYPE_GRID.equals(globalAttribs.getEosType())) {
            addMapGeocoding(product, globalAttribs);
        } else {
            addTiePointGrids(sdStart, productType, product, globalAttribs);
            addModisTiePointGeoCoding(product);
        }
    }

    private void addMapGeocoding(final Product product,
                                 final ModisGlobalAttributes globalAttribs) {
        product.setGeoCoding(globalAttribs.createGeocoding());
    }

    /**
     * Closes the reader and releases all resources.
     */
    public void close() throws IOException {
        _bandReaderMap.clear();

        if (_qcFileId != HDFConstants.FAIL) {
            HDF.getWrap().SDend(_qcFileId);
            _qcFileId = HDFConstants.FAIL;
        }
    }

    /**
     * Retrieves the band reader with the given name. If none exists returns null.
     *
     * @param band
     * @return the band reader
     */
    public ModisBandReader getBandReader(final Band band) {
        return (ModisBandReader) _bandReaderMap.get(band);
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Closes the sds with the given sdsId
     *
     * @param sdsId
     * @throws IOException
     */
    private void closeSds(int sdsId) throws IOException {
        if (sdsId != HDFConstants.FAIL) {
            HDF.getWrap().SDendaccess(sdsId);
        }
    }

    // @todo 4 tb/tb this is stupid code duplication. we have the slight problem that for decoding more
    // information is needed than the buffer itself. See getNamedFloatAttribute
    private int[] getNamedIntAttribute(int sdsId, String name) throws IOException {
        int[] fRet;

        // shortcut for null names
        if (name == null) {
            return null;
        }

        final IHDF ihdf = HDF.getWrap();
        final int attrIdx = ihdf.SDfindattr(sdsId, name);
        if (attrIdx == HDFConstants.FAIL) {
            _logger.warning("Unable to access the attribute '" + name + '\'');
            return null;
        } else {
            final int[] attrInfo = new int[2];
            final String[] dsName = new String[]{""};

            ihdf.SDattrinfo(sdsId, attrIdx, dsName, attrInfo);
            final int attrSize = ihdf.DFKNTsize(attrInfo[0]) * attrInfo[1];
            final byte[] buf = new byte[attrSize];
            if (ihdf.SDreadattr(sdsId, attrIdx, buf)) {
                fRet = HdfUtils.decodeByteBufferToAttribute(buf, attrInfo[0], attrInfo[1], dsName[0]).getIntValues();
            } else {
                _logger.warning("Unable to access the attribute '" + name + '\'');
                return null;
            }
        }

        return fRet;
    }

    /**
     * Retrieves a float (array) attribute with the given name
     *
     * @param sdsId
     * @param name
     * @return a float (array)
     */
    private float[] getNamedFloatAttribute(int sdsId, String name) throws IOException {
        float[] fRet;

        // shortcut for null names
        if (name == null) {
            return null;
        }

        final IHDF ihdf = HDF.getWrap();
        final int attrIdx = ihdf.SDfindattr(sdsId, name);
        if (attrIdx == HDFConstants.FAIL) {
            _logger.warning("Unable to access the attribute '" + name + '\'');
            return null;
        } else {
            final int[] attrInfo = new int[2];
            final String[] dsName = new String[]{""};

            ihdf.SDattrinfo(sdsId, attrIdx, dsName, attrInfo);
            final int attrSize = ihdf.DFKNTsize(attrInfo[0]) * attrInfo[1];
            final byte[] buf = new byte[attrSize];
            if (ihdf.SDreadattr(sdsId, attrIdx, buf)) {
                fRet = HdfUtils.decodeByteBufferToAttribute(buf, attrInfo[0], attrInfo[1], dsName[0]).getFloatValues();
            } else {
                _logger.warning("Unable to access the attribute '" + name + '\'');
                return null;
            }
        }

        return fRet;
    }

    /**
     * Adds all the bands for the given type to the given product
     *
     * @param sdStart the id of the HDF SDstart
     * @param type    the typeId which is to use to read from the database
     * @param product the product to be supplied with bands
     */
    private void addBandsToProduct(final int sdStart, final String type, final Product product) throws IOException {
        final String prodType;
        if (type == null) {
            prodType = product.getProductType();
        } else {
            prodType = type;
        }
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();

        final String[] bandNames = _prodDb.getBandNames(prodType);

        for (int i = 0; i < bandNames.length; i++) {
            int sdsId = HDFConstants.FAIL;
            try {
                sdsId = openNamedSds(sdStart, bandNames[i]);
                if (sdsId == HDFConstants.FAIL) {
                    continue;
                }

                final ModisBandDescription bandDesc = _prodDb.getBandDescription(prodType, bandNames[i]);
                final ModisBandReader[] readers = ModisBandReaderFactory.getReaders(sdsId, bandDesc);

                final String bandNameExtensions = getBandNameExtensions(sdStart, sdsId, prodType,
                        bandDesc.getBandAttribName());
                final float[] scales = getNamedFloatAttribute(sdsId, bandDesc.getScaleAttribName());
                final float[] offsets = getNamedFloatAttribute(sdsId, bandDesc.getOffsetAttribName());

                for (int j = 0; j < readers.length; j++) {
                    String bandNameExt = null;
                    if (bandNameExtensions != null) {
                        if (readers.length > 1) {
                            bandNameExt = ModisUtils.decodeBandName(bandNameExtensions, j);
                            final String name = readers[j].getName() + bandNameExt;
                            readers[j].setName(name);
                        } else {
                            bandNameExt = bandNameExtensions;
                        }
                    }

                    final String bandName = readers[j].getName();
                    final Band band = new Band(bandName, readers[j].getDataType(), width, height);

                    setValidRangeAndFillValue(sdsId, readers[j], band);

                    if (bandDesc.getScalingMethod() != null) {
                        if (scales.length <= j || offsets.length <= j) {
                            _logger.warning("Unable to assign the scaling method '" +
                                    bandDesc.getScalingMethod() + "' to the band '" + bandName + '\'');
                        } else if (bandDesc.getScalingMethod().equalsIgnoreCase(ModisConstants.EXPONENTIAL_SCALE_NAME)) {
                            readers[j].setScaleAndOffset(scales[j], offsets[j]);
                        } else if (bandDesc.getScalingMethod().equalsIgnoreCase(ModisConstants.LINEAR_SCALE_NAME)) {
                            band.setScalingFactor(scales[j]);
                            band.setScalingOffset(-offsets[j] * scales[j]);
                        } else if (bandDesc.getScalingMethod().equalsIgnoreCase(
                                ModisConstants.SLOPE_INTERCEPT_SCALE_NAME)) {
                            band.setScalingFactor(scales[j]);
                            band.setScalingOffset(offsets[j]);
                        } else if (bandDesc.getScalingMethod().equalsIgnoreCase(ModisConstants.POW_10_SCALE_NAME)) {
                            readers[j].setScaleAndOffset(scales[j], offsets[j]);
                        }
                    }

                    setBandSpectralInformation(bandDesc, bandNameExt, band);
                    setBandPhysicalUnit(sdsId, bandDesc, band);
                    setBandDescription(sdsId, bandDesc, band);

                    product.addBand(band);
                    _bandReaderMap.put(band, readers[j]);
                }
            } finally {
                closeSds(sdsId);
            }
        }
    }

    private void setValidRangeAndFillValue(int sdsId, ModisBandReader reader, Band band) throws IOException {
        final int[] rangeArray = getNamedIntAttribute(sdsId, ModisConstants.VALID_RANGE_KEY);
        if (rangeArray != null && rangeArray.length >= 2) {
            Range range = new Range();
            range.setMin(rangeArray[0] < rangeArray[1] ? rangeArray[0] : rangeArray[1]);
            range.setMax(rangeArray[0] > rangeArray[1] ? rangeArray[0] : rangeArray[1]);
            reader.setValidRange(range);
        }

        final int[] fillValue = getNamedIntAttribute(sdsId, ModisConstants.FILL_VALUE_KEY);
        if (fillValue != null && fillValue.length >= 1) {
            reader.setFillValue(fillValue[0]);
            band.setNoDataValue(fillValue[0]);
            band.setNoDataValueUsed(true);
        }
    }

    private static void setBandDescription(int sdsId, ModisBandDescription bandDesc, Band band) throws IOException {
        final String bandDescription = HdfUtils.getNamedStringAttribute(sdsId, bandDesc.getDescriptionAttribName());
        if (bandDescription != null) {
            band.setDescription(bandDescription);
        }
    }

    private static void setBandPhysicalUnit(int sdsId, ModisBandDescription bandDesc, Band band) throws IOException {
        final String unit = HdfUtils.getNamedStringAttribute(sdsId, bandDesc.getUnitAttribName());
        if (unit != null) {
            band.setUnit(unit);
        }
    }

    private static void setBandSpectralInformation(ModisBandDescription bandDesc, String bandNameExt, Band band) {
        if (bandDesc.isSpectral()) {
            if (bandDesc.hasSpectralInfo()) {
                final ModisSpectralInfo specInfo = bandDesc.getSpecInfo();
                band.setSpectralWavelength(specInfo.getSpectralWavelength());
                band.setSpectralBandwidth(specInfo.getSpectralBandwidth());
                band.setSpectralBandIndex(specInfo.getSpectralBandIndex());
            } else {
                final float[] data = ModisUtils.decodeSpectralInformation(bandNameExt, null);
                band.setSpectralWavelength(data[0]);
                band.setSpectralBandwidth(data[1]);
                band.setSpectralBandIndex((int) data[2]);
            }
        } else {
            band.setSpectralBandIndex(-1);
        }
    }


    /**
     * Adds the geolocation to the product
     *
     * @param product
     */
    private void addModisTiePointGeoCoding(Product product) throws IOException {
        ModisProductDescription prodDesc = _prodDb.getProductDescription(product.getProductType());
        final String[] geolocationDatasetNames;
        if (prodDesc.hasExternalGeolocation()) {
            // @todo 2 tb/tb this relies on the order of the tie point grids in the *.dd file.
            // better check the metadata for the lat/lon band names -
            // but for the test products this works fine.
            geolocationDatasetNames = loadExternalQCFile(product, prodDesc);
        } else {
            geolocationDatasetNames = prodDesc.getGeolocationDatasetNames();
        }

        if (geolocationDatasetNames != null) {
            final TiePointGrid latGrid = product.getTiePointGrid(geolocationDatasetNames[0]);
            final TiePointGrid lonGrid = product.getTiePointGrid(geolocationDatasetNames[1]);

            if (latGrid != null && lonGrid != null) {
                // set cyclic behaviour
                lonGrid.setDiscontinuity(TiePointGrid.DISCONT_AT_180);

                // and create geo coding
                GeoCoding coding = new ModisTiePointGeoCoding(latGrid, lonGrid);
                product.setGeoCoding(coding);
            }
        }
    }

    /**
     * Adds all tie point grids to the product
     *
     * @param sdId
     * @param productType
     * @param prod
     * @throws IOException
     */
    private void addTiePointGrids(int sdId, String productType, Product prod, ModisGlobalAttributes globalAttribs) throws IOException {
        TiePointGrid grid;
        String[] tiePointGridNames = _prodDb.getTiePointNames(productType);

        for (int n = 0; n < tiePointGridNames.length; n++) {
            grid = readNamedTiePointGrid(sdId, productType, tiePointGridNames[n], globalAttribs);
            if (grid != null) {
                prod.addTiePointGrid(grid);
            }
        }
    }

    /**
     * Reads the tie point grid with the given name
     *
     * @param sdId
     * @param name
     * @return
     * @throws IOException
     */
    private TiePointGrid readNamedTiePointGrid(int sdId, String prodType, String name,
                                               ModisGlobalAttributes globalAttribs) throws IOException {
        TiePointGrid gridRet = null;
        int sdsId = HDFConstants.FAIL;
        int dimId;
        int[] dimInfo = new int[3];
        int[] dimSize = new int[3];
        String[] dimName = {""};
        int dataType;
        Object buffer;
        float[] fArray = null;
        String scaleAttribName;
        String offsetAttribName;
        float[] scale = new float[]{1.f};
        float[] offset = new float[]{0.f};
        String unitAttribName;
        String units = null;
        ModisTiePointDescription desc;

        int[] start = new int[2];
        int[] count = new int[2];
        int[] stride = new int[2];

        start[0] = start[1] = 0;
        stride[0] = stride[1] = 1;

        try {
            sdsId = openNamedSds(sdId, name);

            if (sdsId == HDFConstants.FAIL) {
                return null;
            }

            if (!HDF.getWrap().SDgetinfo(sdsId, dimName, dimSize, dimInfo)) {
                final String message = "Unable to access tie point grid '" + name + '\'';
                _logger.severe(message);
                throw new IOException(message);
            }

            desc = _prodDb.getTiePointDescription(prodType, name);
            dataType = HdfUtils.decodeHdfDataType(dimInfo[1]);

            dimId = HDF.getWrap().SDgetdimid(sdsId, 0);
            HDF.getWrap().SDdiminfo(dimId, dimName, dimInfo);
            count[0] = dimInfo[0];

            dimId = HDF.getWrap().SDgetdimid(sdsId, 1);
            HDF.getWrap().SDdiminfo(dimId, dimName, dimInfo);
            count[1] = dimInfo[0];

            buffer = allocateDataArray(count[0] * count[1], dataType);
            HDF.getWrap().SDreaddata(sdsId, start, stride, count, buffer);

            scaleAttribName = desc.getScaleAttribName();
            if (scaleAttribName != null) {
                scale = getNamedFloatAttribute(sdsId, scaleAttribName);
                if (scale == null || scale.length <= 0) {
                    scale = new float[]{1.f};
                }
            }
            offsetAttribName = desc.getOffsetAttribName();
            if (offsetAttribName != null) {
                offset = getNamedFloatAttribute(sdsId, offsetAttribName);
                if (offset == null || offset.length <= 0) {
                    offset = new float[]{0.f};
                }
            }
            fArray = scaleArray(dataType, buffer, scale[0], offset[0]);

            HdfDataField field = globalAttribs.getDatafield(name);
            String[] dimNames = field.getDimensionNames();

            final int[] tiePtInfoX = globalAttribs.getSubsamplingAndOffset(dimNames[0]);
            final int[] tiePtInfoY = globalAttribs.getSubsamplingAndOffset(dimNames[1]);
            if (tiePtInfoX != null && tiePtInfoY != null && tiePtInfoX.length > 1 && tiePtInfoY.length > 1) {
                gridRet = new TiePointGrid(name, count[1], count[0], tiePtInfoX[1], tiePtInfoY[1],
                        tiePtInfoX[0], tiePtInfoY[0], fArray);

                unitAttribName = desc.getUnitAttribName();
                if (unitAttribName != null) {
                    units = HdfUtils.getNamedStringAttribute(sdsId, unitAttribName);
                    if (units != null) {
                        gridRet.setUnit(units);
                    }
                }
            } else {
                _logger.warning("Unable to access tie point grid: '" + name + '\'');
            }
        } finally {
            HDF.getWrap().SDendaccess(sdsId);
            sdsId = HDFConstants.FAIL;
        }

        return gridRet;
    }

    /**
     * Allocates an array of the given size and with the given Product data type
     *
     * @param size
     * @param dataType
     * @return the array
     */
    private static Object allocateDataArray(int size, int dataType) {
        Object ret = null;

        switch (dataType) {
            case ProductData.TYPE_FLOAT32:
                ret = new float[size];
                break;

            case ProductData.TYPE_INT16:
            case ProductData.TYPE_UINT16:
                ret = new short[size];
                break;
            case ProductData.TYPE_INT8:
            case ProductData.TYPE_UINT8:
                ret = new byte[size];
                break;
        }
        return ret;
    }

    /**
     * Scales the array passed in.
     *
     * @param dataType
     * @param buffer
     * @param scale
     * @param offset
     * @return the scaled array
     */
    private static float[] scaleArray(int dataType, Object buffer, float scale, float offset) {
        final float[] fRet;

        if (dataType == ProductData.TYPE_FLOAT32) {
            fRet = (float[]) buffer;
            for (int n = 0; n < fRet.length; n++) {
                fRet[n] = scale * fRet[n] + offset;
            }
        } else if (dataType == ProductData.TYPE_INT8) {
            byte[] sData = (byte[]) buffer;
            fRet = new float[sData.length];
            for (int n = 0; n < fRet.length; n++) {
                fRet[n] = sData[n] * scale + offset;
            }
        } else if (dataType == ProductData.TYPE_UINT8) {
            byte[] sData = (byte[]) buffer;
            fRet = new float[sData.length];
            for (int n = 0; n < fRet.length; n++) {
                if (sData[n] < 0) {
                    fRet[n] = (sData[n] + 256) * scale + offset;
                } else {
                    fRet[n] = sData[n] * scale + offset;
                }
            }

        } else if (dataType == ProductData.TYPE_INT16) {
            short[] sData = (short[]) buffer;
            fRet = new float[sData.length];
            for (int n = 0; n < fRet.length; n++) {
                fRet[n] = sData[n] * scale + offset;
            }
        } else if (dataType == ProductData.TYPE_UINT16) {
            short[] sData = (short[]) buffer;
            fRet = new float[sData.length];
            for (int n = 0; n < fRet.length; n++) {
                if (sData[n] < 0) {
                    fRet[n] = (sData[n] + 65536) * scale + offset;
                } else {
                    fRet[n] = sData[n] * scale + offset;
                }
            }
        } else {
            fRet = null;
        }

        return fRet;
    }

    /**
     * Loads an exernal QC file into the product
     *
     * @param product
     */
    private String[] loadExternalQCFile(Product product, ModisProductDescription prodDesc) throws IOException {
        FileContainer qcFile = assembleQCFile(product, prodDesc);
        if (qcFile == null) {
            _logger.warning("MODIS QC file not found.");
            return null;
        }

        _logger.info("MODIS QC file found: " + qcFile.getFile().getPath());

        _qcFileId = HDF.getWrap().SDstart(qcFile.getFile().getPath(), HDFConstants.DFACC_RDONLY);

        final HdfAttributes globalHdfAttrs = HdfUtils.readAttributes(_qcFileId);
        final ModisGlobalAttributes globalAttributes;

        // check wheter daac or imapp
        if (globalHdfAttrs.getStringAttributeValue(ModisConstants.HDF_EOS_VERSION_KEY) == null) {
            globalAttributes = new ModisImappAttributes(qcFile.getFile(), _qcFileId, globalHdfAttrs);
        } else {
            globalAttributes = new ModisDaacAttributes(globalHdfAttrs);
        }

        final String[] tiePointGridNames = _prodDb.getTiePointNames(qcFile.getType());

        for (int n = 0; n < tiePointGridNames.length; n++) {
            final TiePointGrid grid
                    = readNamedTiePointGrid(_qcFileId, qcFile.getType(), tiePointGridNames[n], globalAttributes);
            if (grid != null) {
                product.addTiePointGrid(grid);
            }
        }

        addBandsToProduct(_qcFileId, qcFile.getType(), product);
        return tiePointGridNames;
    }

    /**
     * Assembles the geolocation file path from the product passed in and the *.dd file
     *
     * @param product
     * @param desc
     * @return a file container with the qc file or null
     */
    private static FileContainer assembleQCFile(Product product, ModisProductDescription desc) {

        if (product.getProductType().length() < 2) {
            return null;
        }
        final String qcProductType = getQcFileType(product, desc);
        final File productFile = product.getFileLocation();
        final String qcFileNamePart = getQcFileNamePart(productFile, qcProductType);

        final File productDir = productFile.getParentFile();
        if (productDir != null) {
            Debug.trace("searching for MODIS QC file: " + new File(productDir, '*' + qcFileNamePart + '*').getPath());
            File[] qcFileList = productDir.listFiles(new QCFileFilter(qcFileNamePart));
            if (qcFileList != null && qcFileList.length > 0) {
                final File qcFile = qcFileList[0];
                Debug.trace("MODIS QC file found: " + qcFile.getPath());
                return new FileContainer(qcFile, qcProductType);
            }
        }

        return null;
    }

    private static String getQcFileNamePart(File productFile, String qcProductType) {
        final String fileName = FileUtils.getFilenameWithoutExtension(productFile.getName());

        final int startPos = fileName.indexOf('.');
        final int endPos = fileName.lastIndexOf('.');

        String toAppend = "";
        if (startPos > 0 && endPos > startPos) {
            toAppend = fileName.substring(startPos, endPos);
        }
        return qcProductType + toAppend;
    }

    private static String getQcFileType(Product product, ModisProductDescription desc) {
        final String replaceWith = product.getProductType().substring(1, 2);

        final String pattern = desc.getExternalGeolocationPattern();
        return pattern.replaceFirst("[xX]", replaceWith);
    }

    /**
     * Opens the scientific dataset with the given name.
     *
     * @@param sdId the sd interface identifier
     * @@param name the name
     * @@return the sds identifier
     * @@throws ncsa.hdf.hdflib.HDFException
     */
    private int openNamedSds(int sdId, String name) throws IOException {
        int sdsIdx;
        int sdsId;

        // converts dataset name to index
        final IHDF ihdf = HDF.getWrap();
        sdsIdx = ihdf.SDnametoindex(sdId, name);
        if (sdsIdx == HDFConstants.FAIL) {
            String message = "Unable to access the dataset '" + name + '\'';
            _logger.warning(message);
//            throw new HDFException(message);
            return HDFConstants.FAIL;
        }

        // opens index as identifier
        sdsId = ihdf.SDselect(sdId, sdsIdx);
        if (sdsId == HDFConstants.FAIL) {
            String message = "Unable to access the dataset '" + name + '\'';
            _logger.warning(message);
            throw new IOException(message);
        }

        return sdsId;
    }

    /**
     * Retrieves the band name extension for this band.
     *
     * @@param sdsId
     * @@param productType
     * @@param bandNameAttribName
     * @@return
     * @@throws ncsa.hdf.hdflib.HDFException
     */
    protected String getBandNameExtensions(
            final int sdId, final int sdsId,
            final String productType, final String bandNameAttribName) throws IOException {
        if (bandNameAttribName == null) {
            return null;
        }

        final String attribValue;
        // we have to distinguish three possibilities here
        if (bandNameAttribName.startsWith("@")) {
            // band names are referenced in another band of this product

            final String correspBand = bandNameAttribName.substring(1);
            final ModisBandDescription desc = _prodDb.getBandDescription(productType, correspBand);
            final String bandAttribName = desc.getBandAttribName();
            final String value = HdfUtils.getNamedStringAttribute(sdsId, bandAttribName);
            if (value == null) {
                final int correspSdsId = openNamedSds(sdId, correspBand);
                if (correspSdsId != HDFConstants.FAIL) {
                    attribValue = HdfUtils.getNamedStringAttribute(correspSdsId, bandAttribName);
                    closeSds(correspSdsId);
                } else {
                    attribValue = null;
                }
            } else {
                attribValue = value;
            }

        } else if (StringUtils.isIntegerString(bandNameAttribName)) {
            // band name is directly in the *.dd file
            attribValue = bandNameAttribName;
        } else {
            // band name is in an attribute
            attribValue = HdfUtils.getNamedStringAttribute(sdsId, bandNameAttribName);
        }

        return attribValue;
    }

    ///////////////////////////////////////////////////////////////////////////
    //////// INTERNAL CLASSES
    ///////////////////////////////////////////////////////////////////////////

    static class QCFileFilter implements FilenameFilter {

        private final String _fileNamePart;

        public QCFileFilter(String fileNamePart) {
            _fileNamePart = fileNamePart;
        }

        /**
         * Tests if a specified file should be included in a file list.
         *
         * @param dir  the directory in which the file was found.
         * @param name the name of the file.
         * @return <code>true</code> if and only if the name should be included in the file list; <code>false</code>
         *         otherwise.
         */
        public boolean accept(File dir, String name) {
            return name.indexOf(_fileNamePart) >= 0;
        }
    }

    static class FileContainer {

        private String _fileType;
        private File _file;

        public FileContainer(File file, String type) {
            _file = file;
            _fileType = type;
        }

        public File getFile() {
            return _file;
        }

        public String getType() {
            return _fileType;
        }
    }
}
