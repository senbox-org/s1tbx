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

import org.esa.beam.dataio.modis.attribute.DaacAttributes;
import org.esa.beam.dataio.modis.attribute.ImappAttributes;
import org.esa.beam.dataio.modis.bandreader.ModisBandReader;
import org.esa.beam.dataio.modis.bandreader.ModisBandReaderFactory;
import org.esa.beam.dataio.modis.hdf.HdfDataField;
import org.esa.beam.dataio.modis.netcdf.NetCDFAttributes;
import org.esa.beam.dataio.modis.netcdf.NetCDFUtils;
import org.esa.beam.dataio.modis.netcdf.NetCDFVariables;
import org.esa.beam.dataio.modis.productdb.*;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.util.math.Range;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

class ModisFileReader {

    private Logger logger;
    private HashMap<Band, ModisBandReader> bandReaderMap;
    private ModisProductDb prodDb;
    private NetcdfFile qcFile;

    /**
     * Constructs the object with default parameters
     */
    public ModisFileReader() {
        prodDb = ModisProductDb.getInstance();
        logger = BeamLogManager.getSystemLogger();
        bandReaderMap = new HashMap<Band, ModisBandReader>();
    }

    /**
     * Adds all the bands, the tie point grids and the geocoding to the product.
     *
     * @param globalAttribs   the struct global attributes object
     * @param product         the product to be supplied with bands
     * @param netCDFVariables the list of variables in the product
     */
    public void addRastersAndGeoCoding(final Product product, ModisGlobalAttributes globalAttribs, NetCDFVariables netCDFVariables) throws IOException {
        String productType = product.getProductType();
        if (globalAttribs.isImappFormat()) {
            productType += "_IMAPP";
        }

        addBandsToProduct(netCDFVariables, productType, product);
        if (isEosGridType(globalAttribs)) {
            addMapGeocoding(product, globalAttribs);
        } else {
            addTiePointGrids(netCDFVariables, productType, product, globalAttribs);
            addModisTiePointGeoCoding(product, null);
        }
    }

    /**
     * Retrieves the band reader with the given name. If none exists returns null.
     *
     * @param band the band the reader is needed for
     * @return the band reader
     */
    public ModisBandReader getBandReader(final Band band) {
        return bandReaderMap.get(band);
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    static boolean isEosGridType(ModisGlobalAttributes globalAttribs) throws IOException {
        return ModisConstants.EOS_TYPE_GRID.equals(globalAttribs.getEosType());
    }

    private static int[] getNamedIntAttribute(Variable variable, String name) {
        final List<Attribute> attributes = variable.getAttributes();
        for (final Attribute attribute : attributes) {
            if (attribute.getName().equals(name)) {
                final Array values = attribute.getValues();
                final long size = values.getSize();
                final int[] result = new int[(int) size];
                for (int i = 0; i < size; i++) {
                    result[i] = values.getInt(i);
                }
                return result;
            }
        }
        return new int[0];
    }

    private static float[] getNamedFloatAttribute(NetCDFAttributes attributes, String name) {
        final Attribute attribute = attributes.get(name);
        if (attribute != null) {
            return NetCDFUtils.getFloatValues(attribute);
        }
        return new float[0];
    }

    private void addBandsToProduct(NetCDFVariables netCDFVariables, final String type, final Product product) throws IOException {
        final String prodType = getTypeString(type, product);

        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();

        final String[] bandNames = prodDb.getBandNames(prodType);

        for (String bandName : bandNames) {
            final ModisBandDescription bandDesc = prodDb.getBandDescription(prodType, bandName);
            if (bandDesc == null) {
                logger.warning("No band description for band '" + bandName + "' of product type '" + prodType + "'");
                continue;
            }

            final ModisBandReader[] bandReaders = ModisBandReaderFactory.getReaders(netCDFVariables, bandDesc);
            final Variable variable = netCDFVariables.get(bandName);
            if (variable == null) {
                logger.warning("Name Variable '" + bandName + "' of product type '" + prodType + "' not found");
                continue;
            }
            final List<Attribute> attributes = variable.getAttributes();
            NetCDFAttributes netCDFAttributes = new NetCDFAttributes();
            netCDFAttributes.add(attributes);

            final String bandNameExtensions = getBandNameExtensions(bandDesc.getBandAttribName(), prodType, netCDFAttributes, netCDFVariables);
            final float[] scales = getNamedFloatAttribute(netCDFAttributes, bandDesc.getScaleAttribName());
            final float[] offsets = getNamedFloatAttribute(netCDFAttributes, bandDesc.getOffsetAttribName());

            for (int readerIdx = 0; readerIdx < bandReaders.length; readerIdx++) {
                final ModisBandReader bandReader = bandReaders[readerIdx];
                String bandNameExt = null;
                if (bandNameExtensions != null) {
                    if (bandReaders.length > 1) {
                        bandNameExt = ModisUtils.decodeBandName(bandNameExtensions, readerIdx);
                        final String name = bandReader.getName() + bandNameExt;
                        bandReader.setName(name);
                    } else {
                        bandNameExt = bandNameExtensions;
                    }
                }

                final String readerBandName = bandReader.getName();
                final Band band = new Band(readerBandName, bandReader.getDataType(), width, height);
                setValidRangeAndFillValue(variable, bandReader, band);

                if (bandDesc.getScalingMethod() != null) {
                    if (hasInvalidScaleAndOffset(scales, offsets, readerIdx)) {
                        logger.warning("Unable to assign the scaling method '" + bandDesc.getScalingMethod() + "' to the band '" + bandName + '\'');
                    } else if (bandDesc.isExponentialScaled()) {
                        bandReader.setScaleAndOffset(scales[readerIdx], offsets[readerIdx]);
                    } else if (bandDesc.isLinearScaled()) {
                        band.setScalingFactor(scales[readerIdx]);
                        band.setScalingOffset(-offsets[readerIdx] * scales[readerIdx]);
                    } else if (bandDesc.isLinearInvertedScaled()) {
                        invert(scales);
                        band.setScalingFactor(scales[readerIdx]);
                        band.setScalingOffset(-offsets[readerIdx] * scales[readerIdx]);
                    } else if (bandDesc.isSlopeInterceptScaled()) {
                        band.setScalingFactor(scales[readerIdx]);
                        band.setScalingOffset(offsets[readerIdx]);
                    } else if (bandDesc.isPow10Scaled()) {
                        bandReader.setScaleAndOffset(scales[readerIdx], offsets[readerIdx]);
                    }
                }

                setBandSpectralInformation(bandDesc, bandNameExt, band);
                setBandPhysicalUnit(variable, bandDesc, band);
                setBandDescription(variable, bandDesc, band);

                product.addBand(band);
                bandReaderMap.put(band, bandReader);
            }
        }
    }

    static void invert(float[] scales) {
        for (int i = 0; i < scales.length; i++) {
            if (scales[i] != 0.f) {
                scales[i] = 1.f / scales[i];
            }
        }
    }

    static String getTypeString(String type, Product product) {
        final String prodType;

        if (type == null) {
            prodType = product.getProductType();
        } else {
            prodType = type;
        }

        return prodType;
    }

    static boolean hasInvalidScaleAndOffset(float[] scales, float[] offsets, int readerIdx) {
        return scales.length <= readerIdx || offsets.length <= readerIdx;
    }

    private static void setValidRangeAndFillValue(Variable variable, ModisBandReader reader, Band band) throws IOException {
        final int[] rangeArray = getNamedIntAttribute(variable, ModisConstants.VALID_RANGE_KEY);
        final Range range = createRangeFromArray(rangeArray);
        if (range != null) {
            reader.setValidRange(range);
        }

        final int[] fillValue = getNamedIntAttribute(variable, ModisConstants.FILL_VALUE_KEY);
        if (fillValue != null && fillValue.length >= 1) {
            reader.setFillValue(fillValue[0]);
            band.setNoDataValue(fillValue[0]);
            band.setNoDataValueUsed(true);
        }
    }

    private static void setBandDescription(Variable variable, ModisBandDescription bandDesc, Band band) throws IOException {
        final String descriptionAttribName = bandDesc.getDescriptionAttribName();
        final List<Attribute> attributes = variable.getAttributes();
        for (final Attribute attribute : attributes) {
            if (attribute.getName().equalsIgnoreCase(descriptionAttribName)) {
                final String description = attribute.getStringValue();
                band.setDescription(description);
                return;
            }
        }
    }

    private static void setBandPhysicalUnit(Variable variable, ModisBandDescription bandDesc, Band band) throws IOException {
        final String unitAttribName = bandDesc.getUnitAttribName();
        final List<Attribute> attributes = variable.getAttributes();
        for (final Attribute attribute : attributes) {
            if (attribute.getName().equalsIgnoreCase(unitAttribName)) {
                final String unit = attribute.getStringValue();
                band.setUnit(unit);
                return;
            }
        }
    }

    static void setBandSpectralInformation(ModisBandDescription bandDesc, String bandNameExt, Band band) {
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

    private void addModisTiePointGeoCoding(Product product, NetCDFVariables netCDFVariables) throws IOException {
        ModisProductDescription prodDesc = prodDb.getProductDescription(product.getProductType());
        final String[] geolocationDatasetNames;
        if (prodDesc.hasExternalGeolocation()) {
            // @todo 2 tb/tb this relies on the order of the tie point grids in the *.dd file.
            // better check the metadata for the lat/lon band names -
            // but for the test products this works fine.
            geolocationDatasetNames = loadExternalQCFile(product, prodDesc, netCDFVariables);
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
     * @param netCDFVariables
     * @param productType
     * @param prod
     * @throws IOException
     */
    private void addTiePointGrids(NetCDFVariables netCDFVariables, String productType, Product prod, ModisGlobalAttributes globalAttribs) throws IOException {
        TiePointGrid grid;
        String[] tiePointGridNames = prodDb.getTiePointNames(productType);

        for (int n = 0; n < tiePointGridNames.length; n++) {
            final Variable variable = netCDFVariables.get(tiePointGridNames[n]);
            if (variable == null) {
                logger.warning("Unable to access tie point grid: '" + tiePointGridNames[n] + '\'');
                continue;
            }
            final NetCDFAttributes attributes = new NetCDFAttributes();
            attributes.add(variable.getAttributes());
            grid = readNamedTiePointGrid(variable, attributes, productType, tiePointGridNames[n], globalAttribs);
            if (grid != null) {
                prod.addTiePointGrid(grid);
            }
        }
    }

    /**
     * Reads the tie point grid with the given name
     *
     * @param variable
     * @param netCDFAttributes
     * @param name             @return
     * @throws IOException
     */
    private TiePointGrid readNamedTiePointGrid(Variable variable, NetCDFAttributes netCDFAttributes, String prodType, String name,
                                               ModisGlobalAttributes globalAttribs) throws IOException {
        Object buffer;
        TiePointGrid gridRet = null;
        final ModisTiePointDescription desc = prodDb.getTiePointDescription(prodType, name);
        final DataType ncDataType = variable.getDataType();
        final int dataType1 = DataTypeUtils.getEquivalentProductDataType(ncDataType, false, false); // @todo tb/tb rename variable

        final List<Dimension> dimensions = variable.getDimensions();
        final int height = dimensions.get(0).getLength();
        final int width = dimensions.get(1).getLength();

        float[] floatBuffer = new float[width * height];
        final Array array = variable.read();
        for (int i = 0; i < floatBuffer.length; i++) {
            floatBuffer[i] = array.getFloat(i);
        }

        String scaleAttribName = desc.getScaleAttribName();
        float[] scale = new float[]{1.f};
        if (scaleAttribName != null) {
            scale = getNamedFloatAttribute(netCDFAttributes, scaleAttribName);
            if (scale == null || scale.length <= 0) {
                scale = new float[]{1.f};
            }
        }

        String offsetAttribName = desc.getOffsetAttribName();
        float[] offset = new float[]{0.f};
        if (offsetAttribName != null) {
            offset = getNamedFloatAttribute(netCDFAttributes, offsetAttribName);
            if (offset == null || offset.length <= 0) {
                offset = new float[]{0.f};
            }
        }

        floatBuffer = scaleArray(dataType1, floatBuffer, scale[0], offset[0]);
        HdfDataField field = globalAttribs.getDatafield(name);
        String[] dimNames = field.getDimensionNames();

        final int[] tiePtInfoX = globalAttribs.getSubsamplingAndOffset(dimNames[0]);
        final int[] tiePtInfoY = globalAttribs.getSubsamplingAndOffset(dimNames[1]);

        if (tiePtInfoX != null && tiePtInfoY != null && tiePtInfoX.length > 1 && tiePtInfoY.length > 1) {
            gridRet = new TiePointGrid(name, width, height, tiePtInfoX[1], tiePtInfoY[1] + 0.5f,
                    tiePtInfoX[0], tiePtInfoY[0], floatBuffer);

            String unitAttribName = desc.getUnitAttribName();
            if (unitAttribName != null) {
                String units = NetCDFUtils.getNamedStringAttribute(unitAttribName, netCDFAttributes);
                if (units != null) {
                    gridRet.setUnit(units);
                }
            }
        } else {
            logger.warning("Unable to access tie point grid: '" + name + '\'');
        }

        return gridRet;
    }

    private static float[] scaleArray(int dataType, float[] buffer, float scale, float offset) {

        if (dataType == ProductData.TYPE_FLOAT32) {
            for (int n = 0; n < buffer.length; n++) {
                buffer[n] = scale * buffer[n] + offset;
            }
        } else if (dataType == ProductData.TYPE_INT8) {
            for (int n = 0; n < buffer.length; n++) {
                buffer[n] = buffer[n] * scale + offset;
            }
        } else if (dataType == ProductData.TYPE_UINT8) {
            for (int n = 0; n < buffer.length; n++) {
                if (buffer[n] < 0) {
                    buffer[n] = (buffer[n] + 256) * scale + offset;
                } else {
                    buffer[n] = buffer[n] * scale + offset;
                }
            }

        } else if (dataType == ProductData.TYPE_INT16) {
            for (int n = 0; n < buffer.length; n++) {
                buffer[n] = buffer[n] * scale + offset;
            }
        } else if (dataType == ProductData.TYPE_UINT16) {
            for (int n = 0; n < buffer.length; n++) {
                if (buffer[n] < 0) {
                    buffer[n] = (buffer[n] + 65536) * scale + offset;
                } else {
                    buffer[n] = buffer[n] * scale + offset;
                }
            }
        }

        return buffer;
    }

    /**
     * Loads an exernal QC file into the product
     *
     * @param product
     */
    private String[] loadExternalQCFile(Product product, ModisProductDescription prodDesc, NetCDFVariables netCDFVariables) throws IOException {
        FileContainer qcFileContainer = assembleQCFile(product, prodDesc);
        if (qcFileContainer == null) {
            logger.warning("MODIS QC file not found.");
            return null;
        }

        final File qcFileContainerFile = qcFileContainer.getFile();
        logger.info("MODIS QC file found: " + qcFileContainerFile.getPath());

        qcFile = NetcdfFile.open(qcFileContainerFile.getPath(), null);

        NetCDFAttributes netCDFQCAttributes = new NetCDFAttributes();
        netCDFQCAttributes.add(qcFile.getGlobalAttributes());

        NetCDFVariables netCDFQCVariables = new NetCDFVariables();
        netCDFQCVariables.add(qcFile.getVariables());

        // check wheter daac or imapp
        final ModisGlobalAttributes globalAttributes;
        if (isImappFormat(netCDFQCVariables)) {
            globalAttributes = new ImappAttributes(qcFileContainerFile, netCDFQCVariables, netCDFQCAttributes);
        } else {
            globalAttributes = new DaacAttributes(netCDFQCVariables);
        }

        final String[] tiePointGridNames = prodDb.getTiePointNames(qcFileContainer.getType());
        for (int n = 0; n < tiePointGridNames.length; n++) {
            final Variable variable = netCDFQCVariables.get(tiePointGridNames[n]);
            if (variable != null) {
                final TiePointGrid grid = readNamedTiePointGrid(variable, netCDFQCAttributes, qcFileContainer.getType(), tiePointGridNames[n], globalAttributes);
                if (grid != null) {
                    product.addTiePointGrid(grid);
                }
            }
        }

        // @todo 1 tb/tb get variables from QC file
        //addBandsToProduct(netCDFVariables, _qcFileId, qcFile.getType(), product);
        addBandsToProduct(netCDFVariables, qcFileContainer.getType(), product);
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

    private String getBandNameExtensions(final String bandNameAttribName, final String productType, NetCDFAttributes attributes, NetCDFVariables netCDFVariables) {
        if (StringUtils.isNullOrEmpty(bandNameAttribName)) {
            return null;
        }

        String bandNameExtensions = null;

        // we have to distinguish three possibilities here
        if (bandNameAttribName.startsWith("@")) {
            // band names are referenced in another band of this product
            final String correspBand = bandNameAttribName.substring(1);
            final ModisBandDescription desc = prodDb.getBandDescription(productType, correspBand);
            final String bandAttribName = desc.getBandAttribName();
            final String attributeValue = NetCDFUtils.getNamedStringAttribute(bandAttribName, attributes);
            if (StringUtils.isNullOrEmpty(attributeValue)) {
                final Variable correspVariable = netCDFVariables.get(correspBand);
                if (correspVariable != null) {
                    final NetCDFAttributes correspAttributes = new NetCDFAttributes();
                    correspAttributes.add(correspVariable.getAttributes());
                    bandNameExtensions = NetCDFUtils.getNamedStringAttribute(bandAttribName, correspAttributes);
                }
            } else {
                bandNameExtensions = attributeValue;
            }
        } else if (StringUtils.isIntegerString(bandNameAttribName)) {
            // band name is directly in the *.dd file
            bandNameExtensions = bandNameAttribName;
        } else {
            // band name is in an attribute
            bandNameExtensions = NetCDFUtils.getNamedStringAttribute(bandNameAttribName, attributes);
        }

        return bandNameExtensions;
    }

    static Range createRangeFromArray(int[] rangeArray) {
        if (rangeArray != null && rangeArray.length >= 2) {
            final Range range = new Range();
            range.setMin(rangeArray[0] < rangeArray[1] ? rangeArray[0] : rangeArray[1]);
            range.setMax(rangeArray[0] > rangeArray[1] ? rangeArray[0] : rangeArray[1]);
            return range;
        }
        return null;
    }

    private void addMapGeocoding(final Product product, final ModisGlobalAttributes globalAttribs) {
        product.setGeoCoding(globalAttribs.createGeocoding());
    }

    private boolean isImappFormat(NetCDFVariables netCDFQCVariables) {
        return netCDFQCVariables.get(ModisConstants.STRUCT_META_KEY) == null;
    }

    public void close() throws IOException {
        if (qcFile != null) {
            qcFile.close();
            qcFile = null;
        }
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
