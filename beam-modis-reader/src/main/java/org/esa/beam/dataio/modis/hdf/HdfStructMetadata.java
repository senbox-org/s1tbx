/*
 * $Id: HdfStructMetadata.java,v 1.1 2006/09/19 07:00:03 marcop Exp $
 *
 * Copyright (C) 2002,2003  by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package org.esa.beam.dataio.modis.hdf;

import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.modis.ModisConstants;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.util.StringUtils;

import java.awt.Dimension;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class HdfStructMetadata {

    private HashMap _dimensions;
    private HashMap _dimensionMaps;
    private HashMap _geoFields;
    private HashMap _dataFields;
    private Dimension _productDimension;
    private HdfDataField[] _dataFieldValues;

    /**
     * Constructs the object with default values.
     */
    public HdfStructMetadata() {
        _dimensions = new HashMap();
        _dimensionMaps = new HashMap();
        _geoFields = new HashMap();
        _dataFields = new HashMap();
        _productDimension = new Dimension(0, 0);
    }

    /**
     * Parses the string passed in as HDF EOS swath metastructure.
     *
     * @param metaString
     *
     * @throws ProductIOException
     */
    public void parse(String metaString) throws ProductIOException {
        final LineNumberReader reader = new LineNumberReader(new StringReader(metaString));

        String line;
        String value;

        try {
            while ((line = reader.readLine()) != null) {

                line = line.trim();

                if (line.startsWith(ModisConstants.GROUP_KEY)) {
                    value = getAssignedValue(line);

                    if (value.equalsIgnoreCase(ModisConstants.DIMENSION_KEY)) {
                        parseDimensionGroup(reader);
                    } else if (value.equalsIgnoreCase(ModisConstants.DIMENSION_MAP_KEY)) {
                        parseDimensionMapGroup(reader);
                    } else if (value.equalsIgnoreCase(ModisConstants.GEO_FIELD_KEY)) {
                        parseGeoFieldGroup(reader);
                    } else if (value.equalsIgnoreCase(ModisConstants.DATA_FIELD_KEY)) {
                        parseDataFieldGroup(reader);
                    }
                }
            }
        } catch (IOException e) {
            new ProductIOException(e.getMessage());
        }
        initProductDimension();
    }

    /**
     * Retrieves the data field with the given name
     *
     * @param name
     *
     * @return the data field
     */
    public HdfDataField getDatafield(final String name) {
        HdfDataField dfRet;

        dfRet = (HdfDataField) _dataFields.get(name);
        if (dfRet == null) {
            dfRet = (HdfDataField) _geoFields.get(name);
        }
        return dfRet;
    }

    /**
     * Gets the product dimension.
     *
     * @return the product dimension.
     */
    public Dimension getProductDimensions() {
        return _productDimension;
    }

    /**
     * Retrieves the maximum width and height of all dimensions defined in this product.
     */
    private void initProductDimension() {
        Iterator it = _dataFields.values().iterator();

        HdfDataField data;
        int width = -1;
        int height = -1;
        while (it.hasNext()) {
            data = (HdfDataField) it.next();

            if (data.getWidth() > width) {
                width = data.getWidth();
            }

            if (data.getHeight() > height) {
                height = data.getHeight();
            }
        }
        _productDimension.setSize(width, height);
    }

    public int[] getTiePointSubsAndOffset(String dimName) throws HDFException {
        HdfDimensionMap dim = (HdfDimensionMap) _dimensionMaps.get(dimName);
        final int subsampling;
        final int offset;
        if (dim == null) {
            subsampling = 1;
            offset = 0;
        } else {
            subsampling = dim.getIncrement();
            offset = dim.getOffset();
        }
        return new int[]{subsampling, offset};
    }

    /**
     * Retrieves the number of data fields read from the metadata.
     *
     * @return the number of data fields
     */
    public int getNumDataFields() {
        return _dataFields.size();
    }

    /**
     * Retrieves the meta data data field at the given index or null if the index is invalid.
     *
     * @param n
     *
     * @return the meta data data field
     */
    public HdfDataField getDataFieldAt(int n) {
        if ((n >= 0) && (n < _dataFields.size())) {
            if (_dataFieldValues == null) {
                final Collection values = _dataFields.values();
                _dataFieldValues = (HdfDataField[]) values.toArray(new HdfDataField[values.size()]);
            }
            return _dataFieldValues[n];
        }
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Retrieves the value assigned in a line of the format "variable=value"
     *
     * @param line
     *
     * @return the value
     */
    private String getAssignedValue(String line) {
        int posEqual = line.indexOf('=');

        String value = line.substring(posEqual + 1, line.length());
        value = value.trim();
        if (value.startsWith("\"")) {
            value = value.substring(1);
        }
        if (value.endsWith("\"")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * Parses the dimension group from the line reader passed in. The reader already has consumed the starting tag of
     * the group.
     *
     * @param reader
     */
    private void parseDimensionGroup(LineNumberReader reader) throws IOException {
        HdfDimension dim = new HdfDimension();
        String name = "";
        String value = "";

        String line;
        while (!(line = reader.readLine()).contains(ModisConstants.GROUP_END_KEY)) {
            line = line.trim();

            // note: the order of asking for END_OBJECT and OBJECT should not be changed.
            if (line.contains(ModisConstants.DIMENSION_NAME_KEY)) {
                name = getAssignedValue(line);
            } else if (line.contains(ModisConstants.SIZE_KEY)) {
                value = getAssignedValue(line);
            } else if (line.contains(ModisConstants.OBJECT_END_KEY)) {
                if (dim != null) {
                    dim.setName(name);
                    dim.setValue(Integer.parseInt(value));
                    _dimensions.put(name, dim);
                }
            } else if (line.contains(ModisConstants.OBJECT_KEY)) {
                dim = new HdfDimension();
            }
        }
    }

    /**
     * Parses the dimensionMap group from the line reader passed in. The reader already has consumed the starting tag of
     * the group.
     *
     * @param reader
     */
    private void parseDimensionMapGroup(LineNumberReader reader) throws IOException {
        HdfDimensionMap dim = null;
        String geoDim = "";
        String dataDim = "";
        String offset = "";
        String increment = "";

        String line;
        while (!(line = reader.readLine()).contains(ModisConstants.GROUP_END_KEY)) {
            line = line.trim();

            // note: the order of asking for END_OBJECT and OBJECT should not be changed.
            if (line.contains(ModisConstants.GEO_DIMENSION_KEY)) {
                geoDim = getAssignedValue(line);
            } else if (line.contains(ModisConstants.DATA_DIMENSION_KEY)) {
                dataDim = getAssignedValue(line);
            } else if (line.contains(ModisConstants.OFFSET_KEY)) {
                offset = getAssignedValue(line);
            } else if (line.contains(ModisConstants.INCREMENT_KEY)) {
                increment = getAssignedValue(line);
            } else if (line.contains(ModisConstants.OBJECT_END_KEY)) {
                if (dim != null) {
                    dim.setGeoDim(geoDim);
                    dim.setDataDim(dataDim);
                    dim.setOffset(Integer.parseInt(offset));
                    dim.setIncrement(Integer.parseInt(increment));
                    _dimensionMaps.put(geoDim, dim);
                }
            } else if (line.contains(ModisConstants.OBJECT_KEY)) {
                dim = new HdfDimensionMap();
            }
        }
    }

    /**
     * Parses the geoField group from the line reader passed in. The reader already has consumed the starting tag of the
     * group.
     *
     * @param reader
     */
    private void parseGeoFieldGroup(LineNumberReader reader) throws IOException {
        parseDataFields(reader, _geoFields);
    }

    /**
     * Parses the dataField group from the line reader passed in. The reader already has consumed the starting tag of
     * the group.
     *
     * @param reader
     */
    private void parseDataFieldGroup(LineNumberReader reader) throws IOException {
        _dataFieldValues = null;
        parseDataFields(reader, _dataFields);
    }

    /**
     * Parses data field elements into the hashmap supplied.
     *
     * @param reader
     * @param target
     */
    private void parseDataFields(LineNumberReader reader, HashMap target) throws IOException {
        String name = "";
        String dimList = "";
        HdfDataField field = null;
        int[] dimensions = null;
        String[] dimNames = null;

        String line;
        while (!(line = reader.readLine()).contains(ModisConstants.GROUP_END_KEY)) {
            line = line.trim();

            // note: the order of asking for END_OBJECT and OBJECT should not be changed.
            if (line.contains(ModisConstants.GEO_FIELD_NAME_KEY) || line.contains(ModisConstants.DATA_FIELD_NAME_KEY)) {
                name = getAssignedValue(line);
            } else if (line.contains(ModisConstants.DATA_TYPE_KEY)) {
                getAssignedValue(line);
            } else if (line.contains(ModisConstants.DIMENSION_LIST_KEY)) {
                dimList = getAssignedValue(line);
                dimNames = parseDimNames(dimList);
                dimensions = decodeDimNames(dimNames);
            } else if (line.contains(ModisConstants.OBJECT_END_KEY)) {
                if (field != null) {
                    field.setName(name);
                    field.setDimensionNames(dimNames);
                    if (dimensions.length == 1) {
                        field.setWidth(dimensions[0]);
                    } else if (dimensions.length == 2) {
                        field.setHeight(dimensions[0]);
                        field.setWidth(dimensions[1]);
                    } else if (dimensions.length == 3) {
                        field.setLayers(dimensions[0]);
                        field.setHeight(dimensions[1]);
                        field.setWidth(dimensions[2]);
                    }
                    target.put(name, field);
                }
            } else if (line.contains(ModisConstants.OBJECT_KEY)) {
                field = new HdfDataField();
            }
        }
    }

    /**
     * Decodes the dimension list string into an array of dimension integers
     *
     * @param dimNames a String array containing the dimension names
     *
     * @return the dimension integers
     */
    private int[] decodeDimNames(String[] dimNames) {
        int[] ints = new int[dimNames.length];

        for (int i = 0; i < dimNames.length; i++) {
            final HdfDimension dim = (HdfDimension) _dimensions.get(dimNames[i]);
            if (dim != null) {
                ints[i] = dim.getValue();
            }
        }

        return ints;
    }

    /**
     * Decodes the dimensions string into an array of dimension names.
     *
     * @param dimList
     *
     * @return the dimension names
     */

    private static String[] parseDimNames(String dimList) {
        final List tokens = StringUtils.split(dimList, new char[]{'(', '\"', ',', ')'}, true, null);

        // remove empty tokens
        while (tokens.contains("")) {
            tokens.remove("");
        }

        return (String[]) tokens.toArray(new String[tokens.size()]);
    }
}
