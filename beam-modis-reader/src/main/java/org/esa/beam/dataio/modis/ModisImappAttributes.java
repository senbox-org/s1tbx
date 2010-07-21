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

import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.modis.hdf.HdfAttributes;
import org.esa.beam.dataio.modis.hdf.HdfDataField;
import org.esa.beam.dataio.modis.hdf.HdfUtils;
import org.esa.beam.dataio.modis.hdf.lib.HDF;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.Dimension;
import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;


class ModisImappAttributes implements ModisGlobalAttributes {

    private final File _inFile;
    private final Logger _logger;
    private final int _sdId;
    private Dimension _productDimension;
    private HashMap<String, Integer> _dimensionMap;
    private HashMap<String, IncrementOffset> _subsamplingMap;

    private String _productName;
    private String _productType;
    private Date _sensingStart;
    private Date _sensingStop;


    public ModisImappAttributes(File inFile, int sdId, final HdfAttributes hdfAttributes) throws ProductIOException {
        _logger = BeamLogManager.getSystemLogger();
        _inFile = inFile;
        this._sdId = sdId;

        parseFileNameAndType();
        parseProductDimensions();
        extractStartAndStopTimes(hdfAttributes);
    }

    public String getProductName() {
        return _productName;
    }

    public String getProductType() {
        return _productType;
    }

    public Dimension getProductDimensions() {
        return _productDimension;
    }

    public boolean isImappFormat() {
        return true;
    }

    public String getEosType() {
        return null;
    }

    public GeoCoding createGeocoding() {
        return null;
    }

    public HdfDataField getDatafield(String name) throws ProductIOException {
        final String widthName = name + "_width";
        final String heightName = name + "_height";
        final String layersName = name + "_z";
        Integer width = _dimensionMap.get(widthName);
        Integer height = _dimensionMap.get(heightName);
        Integer z = _dimensionMap.get(layersName);

        if (width == null || height == null) {
            return null;
        }

        final HdfDataField dataField = new HdfDataField();
        dataField.setWidth(width);
        dataField.setHeight(height);
        if (z != null) {
            dataField.setLayers(z);
        } else {
            dataField.setLayers(1);
        }
        dataField.setDimensionNames(new String[]{widthName, heightName, layersName});
        dataField.setName(name);
        return dataField;
    }

    public int[] getSubsamplingAndOffset(String dimensionName) {
        final IncrementOffset incrementOffset = _subsamplingMap.get(dimensionName);
        if (incrementOffset != null) {
            int[] result = new int[2];
            result[0] = incrementOffset.increment;
            result[1] = incrementOffset.offset;

            return result;
        }
        return null;
    }

    public Date getSensingStart() {
        return _sensingStart;
    }

    public Date getSensingStop() {
        return _sensingStop;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private void parseFileNameAndType() {
        _productName = FileUtils.getFilenameWithoutExtension(_inFile);
        final int index = _productName.indexOf('.');
        if (index > 0) {
            _productType = _productName.substring(0, index);
        } else {
            _logger.warning("Unable to retrieve the product type from the file name.");
            _productType = "unknown";
        }
    }

    private void parseProductDimensions() throws ProductIOException {
        // @todo 1 tb/tb this is a rather crude method to retrieve the product dimension: scan all datasets.
        // Find out if there is a clever and more performant way to do this
        int[] numDatasets = new int[1];
        int maxWidth = 0;
        int maxHeight = 0;
        _dimensionMap = new HashMap<String, Integer>();
        _subsamplingMap = new HashMap<String, IncrementOffset>();
        try {
            HDF.getWrap().SDfileinfo(_sdId, numDatasets);

            int[] dimSize = new int[3];
            int[] dimInfo = new int[3];
            String[] dimName = {""};
            for (int n = 0; n < numDatasets[0]; n++) {
                final int sdsId = HDF.getWrap().SDselect(_sdId, n);

                if (!HDF.getWrap().SDgetinfo(sdsId, dimName, dimSize, dimInfo)) {
                    final String msg = "Unable to retrieve meta information for dataset '" + dimName[0] + '\'';
                    _logger.severe(msg);
                    throw new HDFException(msg);
                }

                final String widthName = dimName[0] + "_width";
                final String heightName = dimName[0] + "_height";
                final String zName = dimName[0] + "_z";

                if (dimSize[2] == 0) {
                    maxWidth = Math.max(maxWidth, dimSize[1]);
                    maxHeight = Math.max(maxHeight, dimSize[0]);
                    _dimensionMap.put(widthName, dimSize[1]);
                    _dimensionMap.put(heightName, dimSize[0]);
                } else {
                    maxWidth = Math.max(maxWidth, dimSize[2]);
                    maxHeight = Math.max(maxHeight, dimSize[1]);
                    _dimensionMap.put(widthName, dimSize[2]);
                    _dimensionMap.put(heightName, dimSize[1]);
                    _dimensionMap.put(zName, dimSize[0]);
                }

                ModisUtils.clearDimensionArrays(dimInfo, dimSize);
                addTiePointOffsetAndSubsampling(sdsId, widthName, heightName);

                HDF.getWrap().SDendaccess(sdsId);
            }
        } catch (HDFException e) {
            throw new ProductIOException(e.getMessage());
        } finally {
            _productDimension = new Dimension(maxWidth, maxHeight);
        }
    }

    private void addTiePointOffsetAndSubsampling(int sdsId, String widthName, String heightName) throws HDFException {
        final String lineNumbers = HdfUtils.getNamedStringAttribute(sdsId, "line_numbers");
        if (StringUtils.isNotNullAndNotEmpty(lineNumbers)) {
            _subsamplingMap.put(heightName, ModisUtils.getIncrementOffset(lineNumbers));
        }
        final String frameNumbers = HdfUtils.getNamedStringAttribute(sdsId, "frame_numbers");
        if (StringUtils.isNotNullAndNotEmpty(frameNumbers)) {
            _subsamplingMap.put(widthName, ModisUtils.getIncrementOffset(frameNumbers));
        }
    }

    private void extractStartAndStopTimes(HdfAttributes hdfAttributes) throws ProductIOException {
        try {
            final String startDate = hdfAttributes.getStringAttributeValue(ModisConstants.RANGE_BEGIN_DATE_KEY);
            final String startTime = hdfAttributes.getStringAttributeValue(ModisConstants.RANGE_BEGIN_TIME_KEY);
            final String endDate = hdfAttributes.getStringAttributeValue(ModisConstants.RANGE_END_DATE_KEY);
            final String endTime = hdfAttributes.getStringAttributeValue(ModisConstants.RANGE_END_TIME_KEY);

            if (startDate == null || startTime == null) {
                _logger.warning("Unable to retrieve sensing start time from metadata");
                _sensingStart = null;
                //throw new ProductIOException("Unable to retrieve sensing start time from metadata");
            } else {
                _sensingStart = ModisUtils.createDateFromStrings(startDate, startTime);
            }

            if (endDate == null || endTime == null) {
                _logger.warning("Unable to retrieve sensing stop time from metadata");
                _sensingStop = null;
                //throw new ProductIOException("Unable to retrieve sensing stop time from metadata");
            } else {
                _sensingStop = ModisUtils.createDateFromStrings(endDate, endTime);
            }
        } catch (ParseException e) {
            throw new ProductIOException(e.getMessage());
        }
    }
}
