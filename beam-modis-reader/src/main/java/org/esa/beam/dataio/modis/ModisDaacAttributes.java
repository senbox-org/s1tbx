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
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.util.io.FileUtils;

import java.awt.Dimension;
import java.io.File;
import java.text.ParseException;
import java.util.Date;


class ModisDaacAttributes implements ModisGlobalAttributes {

    private String _productName;
    private String _productType;
    private Date _sensingStart;
    private Date _sensingStop;
    private HdfEosStructMetadata hdfEosStructMetadata;

    public ModisDaacAttributes(final HdfAttributes hdfAttributes) throws ProductIOException {
        decode(hdfAttributes);
    }

    public String getProductName() {
        return _productName;
    }

    public String getProductType() {
        return _productType;
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
        return _sensingStart;
    }

    public Date getSensingStop() {
        return _sensingStop;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private void decode(final HdfAttributes hdfAttributes) throws ProductIOException {
        decodeECSCore(hdfAttributes);

        final String structMetaString = hdfAttributes.getStringAttributeValue(ModisConstants.STRUCT_META_KEY);
        if (structMetaString == null) {
            throw new ProductIOException("Unknown MODIS format: no StructMetadata available");
        }
        hdfEosStructMetadata = new HdfEosStructMetadata(structMetaString);
    }

    private void decodeECSCore(HdfAttributes hdfAttributes) throws ProductIOException {
        final String coreString = ModisDaacUtils.extractCoreString(hdfAttributes);
        if (coreString == null) {
            throw new ProductIOException("Unknown MODIS format: no ECSCore metadata available");
        }

        final String productName = ModisUtils.extractValueForKey(coreString, ModisConstants.LOCAL_GRANULEID_KEY);
        if (productName == null) {
            throw new ProductIOException(
                    "Unknown MODIS format: ECSCore metadata field '" + ModisConstants.LOCAL_GRANULEID_KEY + "' missing");
        }
        _productName = FileUtils.getFilenameWithoutExtension(new File(productName));

        _productType = ModisDaacUtils.extractProductType(_productName);

        extractStartAndStopTimes(coreString);
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
            _sensingStart = ModisUtils.createDateFromStrings(startDate, startTime);

            if (endDate == null || endTime == null) {
                throw new IllegalFileFormatException("Unable to retrieve sensing stop time from metadata");
            }
            _sensingStop = ModisUtils.createDateFromStrings(endDate, endTime);
        } catch (ParseException e) {
            throw new ProductIOException(e.getMessage());
        }
    }
}
