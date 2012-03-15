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

import org.esa.beam.dataio.modis.hdf.HdfDataField;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;


class ModisImappAttributes implements ModisGlobalAttributes {

    private static final String LINE_NUMBERS = "line_numbers";
    private static final String FRAME_NUMBERS = "frame_numbers";
    private final Logger logger;
    private NetcdfFile ncFile;
    private Dimension productDimension;
    private HashMap<String, Integer> dimensionMap;
    private HashMap<String, IncrementOffset> subsamplingMap;

    private String productName;
    private String productType;
    private Date sensingStart;
    private Date sensingStop;

    public ModisImappAttributes(File inFile, NetcdfFile ncFile) throws ProductIOException {
        this.ncFile = ncFile;
        logger = BeamLogManager.getSystemLogger();

        final FileDescriptor descriptor = parseFileNameAndType(inFile);
        productName = descriptor.getProductName();
        productType = descriptor.getProductType();

        parseProductDimensions();
        extractStartAndStopTimes();
    }

    public String getProductName() {
        return productName;
    }

    public String getProductType() {
        return productType;
    }

    public Dimension getProductDimensions() {
        return productDimension;
    }

    public boolean isImappFormat() {
        return true;
    }

    public String getEosType() {
        return null;
    }

    public GeoCoding createGeocoding() {
        // @todo 1 tb/tb why no geocoding?
        return null;
    }

    public HdfDataField getDatafield(String name) throws ProductIOException {
        final String widthName = name + "_width";
        final String heightName = name + "_height";
        final String layersName = name + "_z";
        Integer width = dimensionMap.get(widthName);
        Integer height = dimensionMap.get(heightName);
        Integer z = dimensionMap.get(layersName);

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
        final IncrementOffset incrementOffset = subsamplingMap.get(dimensionName);
        if (incrementOffset != null) {
            int[] result = new int[2];
            result[0] = incrementOffset.increment;
            result[1] = incrementOffset.offset;

            return result;
        }
        return null;
    }

    public Date getSensingStart() {
        return sensingStart;
    }

    public Date getSensingStop() {
        return sensingStop;
    }

    @Override
    public int getNumGlobalAttributes() {
        return ncFile.getGlobalAttributes().size();
    }

    @Override
    public MetadataAttribute getMetadataAttributeAt(int index) {
        throw new NotImplementedException();
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    static FileDescriptor parseFileNameAndType(File file) {
        final FileDescriptor descriptor = new FileDescriptor();

        final String productName = FileUtils.getFilenameWithoutExtension(file);
        descriptor.setProductName(productName);

        final int index = productName.indexOf('.');
        if (index > 0) {
            descriptor.setProductType(productName.substring(0, index));
        } else {
            BeamLogManager.getSystemLogger().warning("Unable to retrieve the product type from the file name.");
            descriptor.setProductType("unknown");
        }
        return descriptor;
    }

    private void parseProductDimensions() throws ProductIOException {
        dimensionMap = new HashMap<String, Integer>();
        subsamplingMap = new HashMap<String, IncrementOffset>();
        int maxWidth = 0;
        int maxHeight = 0;

        final List<Variable> variables = ncFile.getVariables();
        for (int i = 0; i < variables.size(); i++) {
            final Variable variable = variables.get(i);

            final String name = variable.getName();
            final String widthName = name + "_width";
            final String heightName = name + "_height";
            final String zName = name + "_z";

            final List<ucar.nc2.Dimension> dimensions = variable.getDimensions();
            if (dimensions.size() == 2) {
                maxWidth = Math.max(maxWidth, dimensions.get(1).getLength());
                maxHeight = Math.max(maxHeight, dimensions.get(0).getLength());
                dimensionMap.put(widthName, dimensions.get(1).getLength());
                dimensionMap.put(heightName, dimensions.get(0).getLength());
            } else if(dimensions.size() == 3){
                maxWidth = Math.max(maxWidth, dimensions.get(2).getLength());
                maxHeight = Math.max(maxHeight, dimensions.get(1).getLength());
                dimensionMap.put(widthName, dimensions.get(2).getLength());
                dimensionMap.put(heightName, dimensions.get(1).getLength());
                dimensionMap.put(zName, dimensions.get(0).getLength());
            }

            addTiePointOffsetAndSubsampling(variable, widthName, heightName);
        }

        productDimension = new Dimension(maxWidth, maxHeight);
    }

    private void addTiePointOffsetAndSubsampling(Variable variable, String widthName, String heightName) {
        final List<Attribute> attributes = variable.getAttributes();
        Attribute lineNumbersAttribute = null;
        Attribute frameNumbersAttribute = null;
        for (int i = 0; i < attributes.size(); i++) {
            final Attribute attribute = attributes.get(i);
            if (LINE_NUMBERS.equals(attribute.getName())) {
                lineNumbersAttribute = attribute;
            }

            if (FRAME_NUMBERS.equals(attribute.getName())) {
                frameNumbersAttribute = attribute;
            }
        }

        if (lineNumbersAttribute != null) {
            subsamplingMap.put(heightName, ModisUtils.getIncrementOffset(lineNumbersAttribute.getStringValue()));
        }
        if (frameNumbersAttribute != null) {
            subsamplingMap.put(widthName, ModisUtils.getIncrementOffset(frameNumbersAttribute.getStringValue()));
        }
    }

    private void extractStartAndStopTimes() throws ProductIOException {
        final List<Attribute> globalAttributes = ncFile.getGlobalAttributes();
        Attribute startDateAttribute = null;
        Attribute startTimeAttribute = null;
        Attribute endDateAttribute = null;
        Attribute endTimeAttribute = null;

        for (int i = 0; i < globalAttributes.size(); i++) {
            final Attribute attribute = globalAttributes.get(i);
            final String attributeName = attribute.getName();
            if (ModisConstants.RANGE_BEGIN_DATE_KEY.equals(attributeName)) {
                startDateAttribute = attribute;
            }
            if (ModisConstants.RANGE_BEGIN_TIME_KEY.equals(attributeName)) {
                startTimeAttribute = attribute;
            }
            if (ModisConstants.RANGE_END_DATE_KEY.equals(attributeName)) {
                endDateAttribute = attribute;
            }
            if (ModisConstants.RANGE_END_TIME_KEY.equals(attributeName)) {
                endTimeAttribute = attribute;
            }
        }

        try {
            if (startDateAttribute == null || startTimeAttribute == null) {
                logger.warning("Unable to retrieve sensing start time from metadata");
                sensingStart = null;
            } else {
                sensingStart = ModisUtils.createDateFromStrings(startDateAttribute.getStringValue(),
                                                                 startTimeAttribute.getStringValue());
            }
            if (endDateAttribute == null || endTimeAttribute == null) {
                logger.warning("Unable to retrieve sensing stop time from metadata");
                sensingStop = null;
            } else {
                sensingStop = ModisUtils.createDateFromStrings(endDateAttribute.getStringValue(),
                                                                endTimeAttribute.getStringValue());
            }
        } catch (ParseException e) {
            throw new ProductIOException(e.getMessage());
        }
    }

    static class FileDescriptor {
        private String productName;
        private String productType;

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductType(String productType) {
            this.productType = productType;
        }

        public String getProductType() {
            return productType;
        }
    }
}
