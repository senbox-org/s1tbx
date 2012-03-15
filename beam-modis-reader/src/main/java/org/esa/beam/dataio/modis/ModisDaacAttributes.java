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

import org.esa.beam.dataio.modis.hdf.HdfAttributes;
import org.esa.beam.dataio.modis.hdf.HdfDataField;
import org.esa.beam.dataio.modis.hdf.HdfEosStructMetadata;
import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.util.io.FileUtils;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


class ModisDaacAttributes implements ModisGlobalAttributes {

    private String productName;
    private String productType;
    private Date sensingStart;
    private Date sensingStop;
    private NetcdfFile ncfile;
    private HdfEosStructMetadata hdfEosStructMetadata;

    public ModisDaacAttributes(NetcdfFile ncfile) throws ProductIOException {
        this.ncfile = ncfile;
        final List<Variable> variables = ncfile.getVariables();
        try {
            decode(variables);
        } catch (IOException e) {
            throw new ProductIOException(e.getMessage());
        }
    }

    @Deprecated
    public ModisDaacAttributes(HdfAttributes globalHdfAttrs) {
        // @todo 1 tb/tb just to satisfy the compiler - delete when finished with porting
    }

    public String getProductName() {
        return productName;
    }

    public String getProductType() {
        return productType;
    }

    public boolean isImappFormat() {
        return false;
    }

    public String getEosType() {
        return hdfEosStructMetadata.getEosType();
    }

    public GeoCoding createGeocoding() {
        return hdfEosStructMetadata.createGeocoding();
    }

    public Dimension getProductDimensions() {
        return hdfEosStructMetadata.getProductDimensions();
    }

    public HdfDataField getDatafield(String name) {
        return hdfEosStructMetadata.getDatafield(name);
    }

    public int[] getSubsamplingAndOffset(String dimensionName) {
        return hdfEosStructMetadata.getSubsamplingAndOffset(dimensionName);
    }

    public Date getSensingStart() {
        return sensingStart;
    }

    public Date getSensingStop() {
        return sensingStop;
    }

    @Override
    public int getNumGlobalAttributes() {
        return ncfile.getGlobalAttributes().size();
    }

    @Override
    public MetadataAttribute getMetadataAttributeAt(int index) {
        throw new NotImplementedException();
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private void decode(List<Variable> variables) throws IOException {
        decodeECSCore(variables);

        decodeStructMeta(variables);
    }

    private void decodeStructMeta(List<Variable> variables) throws IOException {
        Variable structMetaVariable = null;
        for (int i = 0; i < variables.size(); i++) {
            final Variable variable = variables.get(i);
            final String variableName = variable.getName();
            if (variableName.startsWith(ModisConstants.STRUCT_META_KEY)) {
                structMetaVariable = variable;
                break;
            }
        }

        if (structMetaVariable == null) {
            throw new ProductIOException("Unknown MODIS format: ECSCore metadata field '" + ModisConstants.STRUCT_META_KEY + "' missing");
        }

        final String structMetaString = structMetaVariable.readScalarString();
        if (structMetaString == null) {
            throw new ProductIOException("Unknown MODIS format: no StructMetadata available");
        }
        hdfEosStructMetadata = new HdfEosStructMetadata(structMetaString);
    }

    private void decodeECSCore(List<Variable> variables) throws IOException {
        final String coreKey = ModisConstants.CORE_META_KEY;
        final String coreString = coreKey.substring(0, coreKey.length() - 2);

        final ArrayList<Variable> resultList = new ArrayList<Variable>();
        for (int i = 0; i < variables.size(); i++) {
            final Variable variable = variables.get(i);
            final String variableName = variable.getName();
            if (variableName.startsWith(coreString) && variableName.length() == coreKey.length()) {
                resultList.add(variable);
            }
        }

        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < resultList.size(); i++) {
            final Variable variable = resultList.get(i);
            buffer.append(variable.readScalarString());
        }

        final String ecsCore = ModisDaacUtils.correctAmpersandWrap(buffer.toString());
        if (ecsCore == null) {
            throw new ProductIOException("Unknown MODIS format: no ECSCore metadata available");
        }

        final String productNameMeta = ModisUtils.extractValueForKey(ecsCore, ModisConstants.LOCAL_GRANULEID_KEY);
        if (productNameMeta == null) {
            throw new ProductIOException("Unknown MODIS format: ECSCore metadata field '" + ModisConstants.LOCAL_GRANULEID_KEY + "' missing");
        }

        productName = FileUtils.getFilenameWithoutExtension(new File(productNameMeta));
        productType = ModisDaacUtils.extractProductType(this.productName);

        extractStartAndStopTimes(ecsCore);
    }

    private void extractStartAndStopTimes(String coreString) throws ProductIOException {
        try {
            final String startDate = ModisUtils.extractValueForKey(coreString, ModisConstants.RANGE_BEGIN_DATE_KEY);
            final String startTime = ModisUtils.extractValueForKey(coreString, ModisConstants.RANGE_BEGIN_TIME_KEY);
            final String endDate = ModisUtils.extractValueForKey(coreString, ModisConstants.RANGE_END_DATE_KEY);
            final String endTime = ModisUtils.extractValueForKey(coreString, ModisConstants.RANGE_END_TIME_KEY);

            if (startDate == null || startTime == null) {
                throw new IllegalFileFormatException("Unable to retrieve sensing start time from metadata");
            }
            sensingStart = ModisUtils.createDateFromStrings(startDate, startTime);

            if (endDate == null || endTime == null) {
                throw new IllegalFileFormatException("Unable to retrieve sensing stop time from metadata");
            }
            sensingStop = ModisUtils.createDateFromStrings(endDate, endTime);
        } catch (ParseException e) {
            throw new ProductIOException(e.getMessage());
        }
    }
}
