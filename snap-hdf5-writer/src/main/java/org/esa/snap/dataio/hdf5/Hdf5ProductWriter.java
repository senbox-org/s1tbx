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
package org.esa.snap.dataio.hdf5;

import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.MapGeoCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.maptransf.MapInfo;
import org.esa.snap.core.dataop.maptransf.MapProjection;
import org.esa.snap.core.dataop.maptransf.MapTransform;
import org.esa.snap.core.dataop.maptransf.MapTransformDescriptor;
import org.esa.snap.core.param.Parameter;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A product writer implementation for the HDF5 format.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class Hdf5ProductWriter extends AbstractProductWriter {

    private File outputFile;
    private Map<Band, Integer> bandIDs;
    private int fileID;
    private boolean hdf5LibInit;
    private boolean metadataAnnotated;

    /**
     * Construct a new instance of a product writer for the given HDF5 product writer plug-in.
     *
     * @param writerPlugIn the given HDF5 product writer plug-in, must not be {@code null}
     */
    public Hdf5ProductWriter(ProductWriterPlugIn writerPlugIn) {
        super(writerPlugIn);
        metadataAnnotated = false;
    }

    /**
     * Returns the output file of the product being written.
     */
    public File getOutputFile() {
        return outputFile;
    }

    /**
     * Returns all band output streams opened so far.
     */
    public Map getBandOutputStreams() {
        return bandIDs;
    }


    /**
     * Returns whether the given product node is to be written.
     *
     * @param node the product node
     * @return {@code true} if so
     */
    @Override
    public boolean shouldWrite(ProductNode node) {
        return !(node instanceof VirtualBand) && super.shouldWrite(node);
    }

    /**
     * Writes the in-memory representation of a data product. This method was called by {@code writeProductNodes(product,
     * output)} of the AbstractProductWriter.
     *
     * @throws IllegalArgumentException if {@code output} type is not one of the supported output sources.
     * @throws IOException              if an I/O error occurs
     */
    @Override
    protected void writeProductNodesImpl() throws IOException {

        if (!hdf5LibInit) {
            try {
                H5.H5open();
                hdf5LibInit = true;
            } catch (HDF5LibraryException e) {
                throw new ProductIOException(createErrorMessage(e), e);
            }
        }

        if (getOutput() instanceof String) {
            outputFile = new File((String) getOutput());
        } else if (getOutput() instanceof File) {
            outputFile = (File) getOutput();
        }
        Debug.assertNotNull(outputFile); // super.writeProductNodes should have checked this already

        outputFile = FileUtils.ensureExtension(outputFile, Hdf5ProductWriterPlugIn.HDF5_FILE_EXTENSION);

        try {
            Debug.trace("creating HDF5 file " + outputFile.getPath());
            fileID = H5.H5Fcreate(outputFile.getPath(),
                                  HDF5Constants.H5F_ACC_TRUNC,
                                  HDF5Constants.H5P_DEFAULT,
                                  HDF5Constants.H5P_DEFAULT);
        } catch (HDF5LibraryException e) {
            throw new ProductIOException(createErrorMessage(e), e);
        }

        writeTiePointGrids();
        writeGeoCoding();
        writeFlagCodings();
        writeMetadata();
    }

    private void writeGeoCoding() throws IOException {
        final Product product = getSourceProduct();
        if (product.getSceneGeoCoding() instanceof TiePointGeoCoding) {
            writeGeoCoding((TiePointGeoCoding) product.getSceneGeoCoding());
        } else if (product.getSceneGeoCoding() instanceof MapGeoCoding) {
            writeGeoCoding((MapGeoCoding) product.getSceneGeoCoding());
        }
    }

    private void writeGeoCoding(final TiePointGeoCoding geoCoding) throws IOException {
        final int groupID = createH5G(fileID, "geo_coding");
        try {
            createScalarAttribute(groupID, "java_class_name", TiePointGeoCoding.class.getName());
            createScalarAttribute(groupID, "lat_grid", geoCoding.getLatGrid().getName());
            createScalarAttribute(groupID, "lon_grid", geoCoding.getLonGrid().getName());
        } finally {
            closeH5G(groupID);
        }
    }

    private void writeGeoCoding(final MapGeoCoding geoCoding) throws IOException {
        final MapInfo mapInfo = geoCoding.getMapInfo();
        final MapProjection mapProjection = mapInfo.getMapProjection();
        final int groupID = createH5G(fileID, "geo_coding");
        try {
            createScalarAttribute(groupID, "java_class_name", MapGeoCoding.class.getName());
            createScalarAttribute(groupID, "easting", mapInfo.getEasting());
            createScalarAttribute(groupID, "northing", mapInfo.getNorthing());
            createScalarAttribute(groupID, "pixel_x", mapInfo.getPixelX());
            createScalarAttribute(groupID, "pixel_y", mapInfo.getPixelY());
            createScalarAttribute(groupID, "pixel_size_x", mapInfo.getPixelSizeX());
            createScalarAttribute(groupID, "pixel_size_y", mapInfo.getPixelSizeY());
            createScalarAttribute(groupID, "datum", mapInfo.getDatum().getName());
            createScalarAttribute(groupID, "unit", mapProjection.getMapUnit());
            createScalarAttribute(groupID, "projection", mapProjection.getName());
            final int paramsID = createH5G(groupID, "projection_params");
            try {
                final MapTransform mapTransform = mapProjection.getMapTransform();
                final MapTransformDescriptor mapTransformDescriptor = mapTransform.getDescriptor();
                final Parameter[] parameters = mapTransformDescriptor.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    createScalarAttribute(paramsID,
                                          parameters[i].getName(),
                                          mapTransform.getParameterValues()[i]);
                }
            } finally {
                closeH5G(paramsID);
            }
        } finally {
            closeH5G(groupID);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeBandRasterData(Band sourceBand,
                                    int sourceOffsetX,
                                    int sourceOffsetY,
                                    int sourceWidth,
                                    int sourceHeight,
                                    ProductData sourceBuffer,
                                    ProgressMonitor pm) throws IOException {
        checkBufferSize(sourceWidth, sourceHeight, sourceBuffer);
        final int sourceBandWidth = sourceBand.getRasterWidth();
        final int sourceBandHeight = sourceBand.getRasterHeight();
        checkSourceRegionInsideBandRegion(sourceWidth, sourceBandWidth, sourceHeight, sourceBandHeight, sourceOffsetX,
                                          sourceOffsetY);

        int memTypeID = -1;
        int memSpaceID = -1;

        pm.beginTask("Writing band '" + sourceBand.getName() + "'...", 1);
        try {
            final int datasetID = getOrCreateBandH5D(sourceBand);
            final int fileSpaceID = H5.H5Dget_space(datasetID);
            final long[] memDims = new long[]{sourceHeight, sourceWidth};
            final long[] memStart = new long[2];
            final long[] memCount = new long[2];
            final long[] fileStart = new long[2];
            final long[] fileCount = new long[2];

            memTypeID = createH5TypeID(sourceBuffer.getType());
            memSpaceID = H5.H5Screate_simple(2, memDims, null);

            memStart[0] = 0;
            memStart[1] = 0;
            memCount[0] = sourceHeight;
            memCount[1] = sourceWidth;
            H5.H5Sselect_hyperslab(memSpaceID,
                                   HDF5Constants.H5S_SELECT_SET,
                                   memStart, null, memCount, null);

            fileStart[0] = sourceOffsetY;
            fileStart[1] = sourceOffsetX;
            fileCount[0] = sourceHeight;
            fileCount[1] = sourceWidth;
            H5.H5Sselect_hyperslab(fileSpaceID,
                                   HDF5Constants.H5S_SELECT_SET,
                                   fileStart, null, fileCount, null);

            H5.H5Dwrite(datasetID,
                        memTypeID,
                        memSpaceID,
                        fileSpaceID,
                        HDF5Constants.H5P_DEFAULT,
                        sourceBuffer.getElems());

            pm.worked(1);
        } catch (HDF5Exception e) {
            throw new ProductIOException(createErrorMessage(e), e);
        } finally {
            closeH5S(memSpaceID);
            closeH5T(memTypeID);
            pm.done();
        }
    }

    /**
     * Deletes the physically representation of the given product from the hard disk.
     */
    public void deleteOutput() {
        if (outputFile != null) {
            outputFile.delete();
        }
    }

    /**
     * Writes all data in memory to disk. After a flush operation, the writer can be closed safely
     *
     * @throws IOException on failure
     */
    public void flush() throws IOException {
        if (fileID == -1) {
            return;
        }
        if (bandIDs != null) {
            for (Integer datasetID : bandIDs.values()) {
                if (datasetID != -1) {
                    try {
                        H5.H5Fflush(datasetID, HDF5Constants.H5F_SCOPE_LOCAL);
                    } catch (HDF5LibraryException e) {
                        Debug.trace(e);
                        /*...*/
                    }
                }
            }
        }
        try {
            H5.H5Fflush(fileID, HDF5Constants.H5F_SCOPE_LOCAL);
        } catch (HDF5LibraryException e) {
            throw new ProductIOException(createErrorMessage(e), e);
        }
    }

    /**
     * Closes all output streams currently open.
     *
     * @throws IOException on failure
     */
    public void close() throws IOException {
        if (fileID == -1) {
            return;
        }
        if (bandIDs != null) {
            bandIDs.values().forEach(this::closeH5D);
            bandIDs.clear();
            bandIDs = null;
        }
        try {
            H5.H5Fclose(fileID);
        } catch (HDF5LibraryException e) {
            throw new ProductIOException(createErrorMessage(e), e);
        }
        fileID = -1;
    }

    private int getH5DataType(int productDataType) {
        switch (productDataType) {
            case ProductData.TYPE_INT8:
                return HDF5Constants.H5T_NATIVE_INT8;
            case ProductData.TYPE_UINT8:
                return HDF5Constants.H5T_NATIVE_UINT8;
            case ProductData.TYPE_INT16:
                return HDF5Constants.H5T_NATIVE_INT16;
            case ProductData.TYPE_UINT16:
                return HDF5Constants.H5T_NATIVE_UINT16;
            case ProductData.TYPE_INT32:
                return HDF5Constants.H5T_NATIVE_INT32;
            case ProductData.TYPE_UINT32:
                return HDF5Constants.H5T_NATIVE_UINT32;
            case ProductData.TYPE_INT64:
                return HDF5Constants.H5T_NATIVE_INT64;
            case ProductData.TYPE_FLOAT32:
                return HDF5Constants.H5T_NATIVE_FLOAT;
            case ProductData.TYPE_FLOAT64:
                return HDF5Constants.H5T_NATIVE_DOUBLE;
            case ProductData.TYPE_ASCII:
            case ProductData.TYPE_UTC:
                return HDF5Constants.H5T_C_S1;
            default:
                throw new IllegalArgumentException("illegal data type ID: " + productDataType);
        }
    }

    private int createH5TypeID(int productDataType) throws IOException {
        try {
            final int h5DataType = getH5DataType(productDataType);
            return H5.H5Tcopy(h5DataType);
        } catch (HDF5LibraryException e) {
            throw new ProductIOException(createErrorMessage(e), e);
        }
    }

    private void checkSourceRegionInsideBandRegion(int sourceWidth, final int sourceBandWidth, int sourceHeight,
                                                   final int sourceBandHeight, int sourceOffsetX, int sourceOffsetY) {
        Guardian.assertWithinRange("sourceWidth", sourceWidth, 1, sourceBandWidth);
        Guardian.assertWithinRange("sourceHeight", sourceHeight, 1, sourceBandHeight);
        Guardian.assertWithinRange("sourceOffsetX", sourceOffsetX, 0, sourceBandWidth - sourceWidth);
        Guardian.assertWithinRange("sourceOffsetY", sourceOffsetY, 0, sourceBandHeight - sourceHeight);
    }

    private void checkBufferSize(int sourceWidth, int sourceHeight, ProductData sourceBuffer) {
        int expectedBufferSize = sourceWidth * sourceHeight;
        int actualBufferSize = sourceBuffer.getNumElems();
        Guardian.assertEquals("wrong sourceBuffer size", actualBufferSize, expectedBufferSize);
    }

    private void writeFlagCodings() throws IOException {
        ProductNodeGroup<FlagCoding> flagCodingGroup = getSourceProduct().getFlagCodingGroup();
        if (flagCodingGroup.getNodeCount() == 0) {
            return;
        }
        int groupID = createH5G(fileID, "/flag_codings");
        try {
            for (int i = 0; i < flagCodingGroup.getNodeCount(); i++) {
                FlagCoding flagCoding = flagCodingGroup.get(i);
                writeMetadataElement(groupID, flagCoding);
            }
        } finally {
            closeH5G(groupID);
        }
    }

    private void writeMetadata() throws IOException {
        final MetadataElement root = getSourceProduct().getMetadataRoot();
        if (root != null) {
            writeMetadataElement(fileID, root);
        }
    }

    private void writeMetadataElement(int locationID, MetadataElement element) throws IOException {
        int groupID = createH5G(locationID, element.getName());
        try {
            List<MetadataAttribute> attributes = enumerateAttributesWithSameName(element);
            for (MetadataAttribute attribute : attributes) {
                writeMetadataAttribute(groupID, attribute);
            }

            List<MetadataElement> childElements = enumerateChildrenWithSameName(element);
            for (MetadataElement subElement : childElements) {
                writeMetadataElement(groupID, subElement);
            }
        } finally {
            closeH5G(groupID);
        }
    }

    static List<MetadataElement> enumerateChildrenWithSameName(MetadataElement parent) {
        MetadataElement[] elements = parent.getElements();
        Map<String, List<MetadataElement>> nameMap = new LinkedHashMap<>();

        for (MetadataElement element : elements) {
            String name = element.getName();
            List<MetadataElement> elementList = nameMap.get(name);
            if (elementList == null) {
                elementList = new ArrayList<>();
                nameMap.put(name, elementList);
            }
            elementList.add(element);
        }

        if (nameMap.size() == elements.length) {
            // if size is equal, all names are different
            return Arrays.asList(elements);
        }


        ArrayList<MetadataElement> elementArrayList = new ArrayList<>();
        for (String elemName : nameMap.keySet()) {
            List<MetadataElement> metadataElements = nameMap.get(elemName);
            if (metadataElements.size() == 1) {
                elementArrayList.add(metadataElements.get(0));
            } else {
                for (int i = 0; i < metadataElements.size(); i++) {
                    MetadataElement metadataElement = metadataElements.get(i);
                    MetadataElement clone = metadataElement.createDeepClone();
                    clone.setName(String.format("%s.%d", elemName, i + 1));
                    elementArrayList.add(clone);
                }
            }
        }

        return elementArrayList;
    }

    static List<MetadataAttribute> enumerateAttributesWithSameName(MetadataElement parent) {
        MetadataAttribute[] attributes = parent.getAttributes();
        Map<String, List<MetadataAttribute>> nameMap = new LinkedHashMap<>();

        for (MetadataAttribute attribute : attributes) {
            String name = attribute.getName();
            List<MetadataAttribute> attributeList = nameMap.get(name);
            if (attributeList == null) {
                attributeList = new ArrayList<>();
                nameMap.put(name, attributeList);
            }
            attributeList.add(attribute);
        }

        if (nameMap.size() == attributes.length) {
            // if size is equal, all names are different
            return Arrays.asList(attributes);
        }


        ArrayList<MetadataAttribute> attributeArrayList = new ArrayList<>();
        for (String attribName : nameMap.keySet()) {
            List<MetadataAttribute> metadataAttributes = nameMap.get(attribName);
            if (metadataAttributes.size() == 1) {
                attributeArrayList.add(metadataAttributes.get(0));
            } else {
                for (int i = 0; i < metadataAttributes.size(); i++) {
                    MetadataAttribute metadataElement = metadataAttributes.get(i);
                    MetadataAttribute clone = metadataElement.createDeepClone();
                    clone.setName(String.format("%s.%d", attribName, i + 1));
                    attributeArrayList.add(clone);
                }
            }
        }

        return attributeArrayList;
    }

    private void writeMetadataAttribute(int locationID, MetadataAttribute attribute) throws IOException {
        int productDataType = attribute.getDataType();
        if (attribute.getData() instanceof ProductData.ASCII
            || attribute.getData() instanceof ProductData.UTC) {
            createScalarAttribute(locationID,
                                  attribute.getName(),
                                  attribute.getData().getElemString());
        } else if (attribute.getData().isScalar()) {
            createScalarAttribute(locationID,
                                  attribute.getName(),
                                  getH5DataType(productDataType),
                                  attribute.getData().getElems());
        } else {
            createArrayAttribute(locationID,
                                 attribute.getName(),
                                 getH5DataType(productDataType),
                                 attribute.getData().getNumElems(),
                                 attribute.getData().getElems());
        }
        if (metadataAnnotated) {
            if (attribute.getUnit() != null) {
                createScalarAttribute(locationID,
                                      attribute.getName() + ".unit",
                                      attribute.getUnit());
            }

            if (attribute.getDescription() != null) {
                createScalarAttribute(locationID,
                                      attribute.getName() + ".descr",
                                      attribute.getDescription());
            }
        }
    }

    private void writeTiePointGrids() throws IOException {
        if (getSourceProduct().getNumTiePointGrids() > 0) {
            int groupId = createH5G(fileID, "/tie_point_grids");
            try {
                for (int i = 0; i < getSourceProduct().getNumTiePointGrids(); i++) {
                    TiePointGrid grid = getSourceProduct().getTiePointGridAt(i);
                    writeTiePointGrid(grid, "/tie_point_grids");
                }
            } finally {
                closeH5G(groupId);
            }
        }
    }

    private void writeTiePointGrid(TiePointGrid grid, String path) throws IOException {
        final int w = grid.getGridWidth();
        final int h = grid.getGridHeight();
        long[] dims = new long[]{h, w};
        int dataTypeID = -1;
        int dataSpaceID = -1;
        int datasetID = -1;
        try {
            dataTypeID = createH5TypeID(grid.getDataType());
            dataSpaceID = H5.H5Screate_simple(2, dims, null);
            String dataSetPath = path + "/" + grid.getName();
            datasetID = H5.H5Dcreate(fileID,
                                     dataSetPath,
                                     dataTypeID,
                                     dataSpaceID,
                                     HDF5Constants.H5P_DEFAULT,
                                     HDF5Constants.H5P_DEFAULT,
                                     HDF5Constants.H5P_DEFAULT);

            // Very important attributes
            createScalarAttribute(datasetID, "scene_raster_width", grid.getRasterWidth());
            createScalarAttribute(datasetID, "scene_raster_height", grid.getRasterHeight());
            createScalarAttribute(datasetID, "offset_x", grid.getOffsetX());
            createScalarAttribute(datasetID, "offset_y", grid.getOffsetY());
            createScalarAttribute(datasetID, "sub_sampling_x", grid.getSubSamplingX());
            createScalarAttribute(datasetID, "sub_sampling_y", grid.getSubSamplingY());

            // Less important attributes
            try {
                createScalarAttribute(datasetID, "raster_width", grid.getGridWidth());
                createScalarAttribute(datasetID, "raster_height", grid.getGridHeight());
                createScalarAttribute(datasetID, "unit", grid.getUnit());
                createScalarAttribute(datasetID, "description", grid.getDescription());
                createScalarAttribute(datasetID, "CLASS", "IMAGE");
                createScalarAttribute(datasetID, "IMAGE_VERSION", 1.0F);
            } catch (IOException e) {
                /* ignore IOException because these attributes are not very essential... */
                Debug.trace("failed to create attribute: " + e.getMessage());
            }

            H5.H5Dwrite(datasetID,
                        dataTypeID,
                        HDF5Constants.H5S_ALL,
                        HDF5Constants.H5S_ALL,
                        HDF5Constants.H5P_DEFAULT,
                        grid.getGridData().getElems());

        } catch (HDF5Exception e) {
            throw new ProductIOException(createErrorMessage(e), e);
        } finally {
            closeH5D(datasetID);
            closeH5S(dataSpaceID);
            closeH5T(dataTypeID);
        }
    }

    private void createScalarAttribute(int locationID, String name, int value) throws IOException {
        createScalarAttribute(locationID, name, HDF5Constants.H5T_NATIVE_INT, new int[]{value});
    }

    private void createScalarAttribute(int locationID, String name, float value) throws IOException {
        createScalarAttribute(locationID, name, HDF5Constants.H5T_NATIVE_FLOAT, new float[]{value});
    }

    private void createScalarAttribute(int locationID, String name, double value) throws IOException {
        createScalarAttribute(locationID, name, HDF5Constants.H5T_NATIVE_DOUBLE, new double[]{value});
    }

    private void createScalarAttribute(int locationID, String name, String value) throws IOException {
        if (value != null && value.length() > 0) {
            createScalarAttribute(locationID, name, HDF5Constants.H5T_C_S1, value.length(), value.getBytes());
        }
    }

    private void createScalarAttribute(int locationID, String name, int jh5DataType, Object value) throws IOException {
        createScalarAttribute(locationID, name, jh5DataType, -1, value);
    }

    private void createScalarAttribute(int locationID, String name, int jh5DataType, int typeSize, Object value) throws
                                                                                                                 IOException {
        Debug.trace("Hdf5ProductWriter.createScalarAttribute("
                    + "locationID=" + locationID
                    + ", name=" + name
                    + ", jh5DataType=" + jh5DataType
                    + ", typeSize=" + typeSize
                    + ", value=" + value
                    + ")");
        int attrTypeID = -1;
        int attrSpaceID = -1;
        int attributeID = -1;
        try {
            attrTypeID = H5.H5Tcopy(jh5DataType);
            if (typeSize > 0) {
                H5.H5Tset_size(attrTypeID, typeSize);
            }
            attrSpaceID = H5.H5Screate(HDF5Constants.H5S_SCALAR);
            attributeID = H5.H5Acreate(locationID, name, attrTypeID, attrSpaceID, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
            H5.H5Awrite(attributeID, attrTypeID, value);
        } catch (HDF5Exception e) {
            throw new ProductIOException(createErrorMessage(e), e);
        } finally {
            closeH5A(attributeID);
            closeH5S(attrSpaceID);
            closeH5T(attrTypeID);
        }
    }

    private void createArrayAttribute(int locationID, String name, float[] values) throws IOException {
        createArrayAttribute(locationID, name, HDF5Constants.H5T_NATIVE_FLOAT, values.length, values);
    }

    private void createArrayAttribute(int locationID, String name, int jh5DataType, int arraySize, Object value) throws
                                                                                                                 IOException {
        //Debug.trace("creating array attribute " + name + ", JH5 type " + jh5DataType + ", size " + arraySize);
        int attrTypeID = -1;
        int attrSpaceID = -1;
        int attributeID = -1;
        try {
            attrTypeID = H5.H5Tcopy(jh5DataType);
            attrSpaceID = H5.H5Screate_simple(1, new long[]{arraySize}, null);
            attributeID = H5.H5Acreate(locationID, name, attrTypeID, attrSpaceID, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
            H5.H5Awrite(attributeID, attrTypeID, value);
        } catch (HDF5Exception e) {
            throw new ProductIOException(createErrorMessage(e), e);
        } finally {
            closeH5A(attributeID);
            closeH5S(attrSpaceID);
            closeH5T(attrTypeID);
        }
    }

    /**
     * Returns the dataset ID associated with the given {@code Band}. If no dataset ID exists, one is created and
     * fed into the hash map
     */
    private Integer getOrCreateBandH5D(Band band) throws IOException {
        Integer bandID = getBandH5D(band);
        if (bandID == null) {
            if (bandIDs == null) {
                bandIDs = new HashMap<>();
                createGroup("/bands");
            }
            bandID = createBandH5D(band);
            bandIDs.put(band, bandID);
        }
        return bandID;
    }

    private Integer getBandH5D(Band band) {
        if (bandIDs != null) {
            return bandIDs.get(band);
        }
        return null;
    }


    private Integer createBandH5D(Band band) throws IOException {

        final int w = band.getRasterWidth();
        final int h = band.getRasterHeight();
        long[] dims = new long[]{h, w};
        int datasetID = -1;
        int fileTypeID = -1;
        int fileSpaceID = -1;

        try {
            fileTypeID = createH5TypeID(band.getDataType());
            fileSpaceID = H5.H5Screate_simple(2, dims, null);
            datasetID = H5.H5Dcreate(fileID,
                                     "/bands/" + band.getName(),
                                     fileTypeID,
                                     fileSpaceID,
                                     HDF5Constants.H5P_DEFAULT,
                                     HDF5Constants.H5P_DEFAULT,
                                     HDF5Constants.H5P_DEFAULT);

            try {
                createScalarAttribute(datasetID, "raster_width", band.getRasterWidth());
                createScalarAttribute(datasetID, "raster_height", band.getRasterHeight());
                createScalarAttribute(datasetID, "scaling_factor", band.getScalingFactor());
                createScalarAttribute(datasetID, "scaling_offset", band.getScalingOffset());
                createScalarAttribute(datasetID, "log10_scaled", band.isLog10Scaled() ? "true" : "false");
                createScalarAttribute(datasetID, "unit", band.getUnit());
                createScalarAttribute(datasetID, "description", band.getDescription());
                if (band.getSpectralBandIndex() >= 0) {
                    createScalarAttribute(datasetID, "spectral_band_index", band.getSpectralBandIndex() + 1);
                    createScalarAttribute(datasetID, "solar_flux", band.getSolarFlux());
                    createScalarAttribute(datasetID, "bandwidth", band.getSpectralBandwidth());
                    createScalarAttribute(datasetID, "wavelength", band.getSpectralWavelength());
                }
                if (band.getFlagCoding() != null) {
                    createScalarAttribute(datasetID, "flag_coding", band.getFlagCoding().getName());
                }
                createScalarAttribute(datasetID, "CLASS", "IMAGE");
                createScalarAttribute(datasetID, "IMAGE_VERSION", 1.2F);

                if (band.isStxSet()) {
                    final Stx stx = band.getStx();
                    createScalarAttribute(datasetID, "min_sample", stx.getMinimum());
                    createScalarAttribute(datasetID, "max_sample", stx.getMaximum());
                }
                if (band.getImageInfo() != null) {
                    final ColorPaletteDef paletteDef = band.getImageInfo().getColorPaletteDef();
                    float[] minmax = new float[]{
                            (float) paletteDef.getMinDisplaySample(),
                            (float) paletteDef.getMaxDisplaySample()
                    };
                    createArrayAttribute(datasetID, "IMAGE_MINMAXRANGE", minmax);
                }
            } catch (IOException e) {
                /* ignore IOException because these attributes are not very essential... */
                Debug.trace("failed to create attribute: " + e.getMessage());
            }

        } catch (HDF5Exception e) {
            closeH5D(datasetID);
            throw new ProductIOException(createErrorMessage(e), e);
        } finally {
            closeH5S(fileSpaceID);
            closeH5T(fileTypeID);
        }

        return datasetID;
    }

    private void createGroup(String path) throws IOException {
        int groupID = -1;
        try {
            groupID = H5.H5Gcreate(fileID, path, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
        } catch (HDF5LibraryException e) {
            throw new ProductIOException(createErrorMessage(e), e);
        } finally {
            closeH5G(groupID);
        }
    }

    private int createH5G(int locationID, String name) throws IOException {
        try {
            return H5.H5Gcreate(locationID, name, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
        } catch (HDF5LibraryException e) {
            throw new ProductIOException(createErrorMessage(e), e);
        }
    }

    private void closeH5T(int typeID) {
        if (typeID != -1) {
            try {
                H5.H5Tclose(typeID);
            } catch (HDF5LibraryException e) {
                Debug.trace(e);
            }
        }
    }

    private void closeH5S(int spaceID) {
        if (spaceID != -1) {
            try {
                H5.H5Sclose(spaceID);
            } catch (HDF5LibraryException e) {
                Debug.trace(e);
            }
        }
    }

    private void closeH5D(int datasetID) {
        if (datasetID != -1) {
            try {
                H5.H5Dclose(datasetID);
            } catch (HDF5LibraryException e) {
                Debug.trace(e);
            }
        }
    }

    private void closeH5A(int attributeID) {
        if (attributeID != -1) {
            try {
                H5.H5Aclose(attributeID);
            } catch (HDF5LibraryException e) {
                Debug.trace(e);
            }
        }
    }

    private void closeH5G(int groupID) {
        if (groupID != -1) {
            try {
                H5.H5Gclose(groupID);
            } catch (HDF5LibraryException e) {
                Debug.trace(e);
            }
        }
    }


    private String createErrorMessage(HDF5Exception e) {
        String message = StringUtils.isNullOrEmpty(e.getMessage()) ? "Unknown Error" : e.getMessage();
        return "HDF library error: " + message;
    }
}
