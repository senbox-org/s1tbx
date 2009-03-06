/*
 * $Id: GeneralInformationHeader.java,v 1.2 2007/01/30 15:31:41 marcoz Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.avhrr.noaa;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.HeaderUtil;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 13.06.2005
 * Time: 13:46:42
 * To change this template use File | Settings | File Templates.
 */
class GeneralInformationHeader {
    private static final String META_DATA_NAME = "GENERAL_INFORMATION";
    private static final int SUPPORTED_BLOCK_LENGTH = 15872;

    private final Map noaaFormat;
    private final Map spaceCraftCode;
    private final Map dataType;

    private String dataSetCreationSiteID;
    private int formatVersion;
    private int formatVersionYear;
    private int formatVersionDayOfYear;
    private int logicalRecordLength;
    private int blockSize;
    private int headerRecordCount;
    private String dataSetName;
    private String processingBlockID;
    private int spacecraftID;
    private int instrumentID;
    private int dataTypeID;
    private int tipSourceCode;
    private ProductData.UTC startDate;
    private ProductData.UTC endDate;
    private int cpidsYear;
    private int cpidsDayOfYear;
    private int bitsPerPixel;

    public GeneralInformationHeader(InputStream header) throws IOException {
        noaaFormat = new HashMap();
        noaaFormat.put(1, "TIROS-N, NOAA-6 through NOAA-14");
        noaaFormat.put(2, "NOAA-15, -16, -17 (pre-April 28, 2005)");
        noaaFormat.put(3, "All satellites post-April 28, 2005");
        noaaFormat.put(4, "All satellites post-April 28, 2005 (with CLAVR-x)");
        noaaFormat.put(5, "All satellites post-November 14, 2006 (with CLAVR-x)");

        spaceCraftCode = new HashMap();
        spaceCraftCode.put(2, "NOAA-16 (NOAA-L)");
        spaceCraftCode.put(4, "NOAA-15 (NOAA-K)");
        spaceCraftCode.put(6, "NOAA-17 (NOAA-M)");
        spaceCraftCode.put(7, "NOAA-18 (NOAA-N)");
        spaceCraftCode.put(8, "(NOAA-P)");
        spaceCraftCode.put(11, "MetOp-1");
        spaceCraftCode.put(12, "MetOp-A");

        dataType = new HashMap();
        dataType.put(1, "LAC");
        dataType.put(2, "GAC");
        dataType.put(3, "HRPT");
        dataType.put(13, "FRAC");
        parse(header);
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    public int getHeaderRecordCount() {
        return headerRecordCount;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public ProductData.UTC getStartDate() {
        return startDate;
    }

    public ProductData.UTC getEndDate() {
        return endDate;
    }

    public void setBitsPerPixel(int bitsPerPixel) {
        this.bitsPerPixel = bitsPerPixel;
    }

    public MetadataElement getMetadata() {
    	String formatString = (String) noaaFormat.get(formatVersion);
        if (formatString == null) {
        	formatString = "Unkown format version.";
        }

        final MetadataElement element = new MetadataElement(META_DATA_NAME);
        element.addAttribute(HeaderUtil.createAttribute("DATA_SET_CREATION_SITE_ID", dataSetCreationSiteID));
        element.addAttribute(HeaderUtil.createAttribute("DATA_SET_CREATION_SITE", DataSetCreationSite.getDatasetCreationSite(dataSetCreationSiteID)));
        element.addAttribute(HeaderUtil.createAttribute("NOAA_LEVEL_1B_FORMAT_VERSION_NUMBER", formatVersion));
        element.addAttribute(HeaderUtil.createAttribute("NOAA_LEVEL_1B_FORMAT_VERSION", formatString));
        element.addAttribute(HeaderUtil.createAttribute("NOAA_LEVEL_1B_FORMAT_VERSION_YEAR", formatVersionYear, AvhrrConstants.UNIT_YEARS));
        element.addAttribute(HeaderUtil.createAttribute("NOAA_LEVEL_1B_FORMAT_VERSION_DAY_OF_YEAR", formatVersionDayOfYear, AvhrrConstants.UNIT_DAYS));
        element.addAttribute(HeaderUtil.createAttribute("LOGICAL_RECORD_LENGTH", logicalRecordLength));
        element.addAttribute(HeaderUtil.createAttribute("BLOCK_SIZE", blockSize));
        element.addAttribute(HeaderUtil.createAttribute("COUNT_OF_HEADER_RECORDS", headerRecordCount));
        element.addAttribute(HeaderUtil.createAttribute("DATA_SET_NAME", dataSetName));
        element.addAttribute(HeaderUtil.createAttribute("PROCESSING_BLOCK_IDENTIFICATION", processingBlockID));
        element.addAttribute(HeaderUtil.createAttribute("NOAA_SPACECRAFT_IDENTIFICATION_CODE", (String) spaceCraftCode.get(spacecraftID)));
        element.addAttribute(HeaderUtil.createAttribute("INSTRUMENT_ID", instrumentID));
        element.addAttribute(HeaderUtil.createAttribute("DATA_TYPE_CODE", (String) dataType.get(dataTypeID)));
        element.addAttribute(HeaderUtil.createAttribute("TIP_SOURCE_CODE", tipSourceCode));
        element.addAttribute(HeaderUtil.createAttribute("START_OF_DATA_SET", startDate.getElemString(), AvhrrConstants.UNIT_DATE));
        element.addAttribute(HeaderUtil.createAttribute("END_OF_DATA_SET", endDate.getElemString(), AvhrrConstants.UNIT_DATE));
        element.addAttribute(HeaderUtil.createAttribute("YEAR_OF_LAST_CPIDS_UPDATE", cpidsYear, AvhrrConstants.UNIT_YEARS));
        element.addAttribute(HeaderUtil.createAttribute("DAY_OF_YEAR_OF_LAST_CPIDS_UPDATE", cpidsDayOfYear, AvhrrConstants.UNIT_DAYS));
        element.addAttribute(HeaderUtil.createAttribute("BITS_PER_PIXEL", bitsPerPixel, AvhrrConstants.UNIT_BITS));
        return element;
    }

    private void parse(InputStream header) throws IOException {
        ExtendedDataInputStream inStream = new ExtendedDataInputStream(header);

        dataSetCreationSiteID = inStream.readString(3);
        inStream.skip(1);

        formatVersion = inStream.readUnsignedShort();
        if (!isSupportedFormatVersion(formatVersion)) {
            throw new ProductIOException("Unsupported AVHRR format version: " + formatVersion);
        }
        formatVersionYear = inStream.readUnsignedShort();
        formatVersionDayOfYear = inStream.readUnsignedShort();

        logicalRecordLength = inStream.readUnsignedShort();
        blockSize = inStream.readUnsignedShort();
        if (!isSupportedBlockLength(blockSize)) {
            throw new ProductIOException("Unsupported AVHRR block length: " + blockSize);
        }
        headerRecordCount = inStream.readUnsignedShort();
        inStream.skip(6);

        dataSetName = inStream.readString(42);
        processingBlockID = inStream.readString(8);
        spacecraftID = inStream.readUnsignedShort();
        instrumentID = inStream.readUnsignedShort();
        dataTypeID = inStream.readUnsignedShort();
        if (!isSupportedDataType(dataTypeID)) {
            final String errorMessage = dataTypeID + " (" + dataType.get(dataTypeID) + ")";
            throw new ProductIOException("Unsupported AVHRR data type ID: " + errorMessage);
        }
        tipSourceCode = inStream.readUnsignedShort();

        /*int startDayFrom1950 =*/ inStream.readInt(); // read next 4 byte integer
        int startYear = inStream.readUnsignedShort();
        int startDayOfYear = inStream.readUnsignedShort();
        int startUTCmillis = inStream.readInt();
        startDate = HeaderUtil.createUTCDate(startYear, startDayOfYear, startUTCmillis);

        /*int endDayFrom1950 =*/ inStream.readInt(); // read next 4 byte integer
        int endYear = inStream.readUnsignedShort();
        int endDayOfYear = inStream.readUnsignedShort();
        int endUTCmillis = inStream.readInt();
        endDate = HeaderUtil.createUTCDate(endYear, endDayOfYear, endUTCmillis);

        cpidsYear = inStream.readUnsignedShort();
        cpidsDayOfYear = inStream.readUnsignedShort();
    }

    private boolean isSupportedFormatVersion(int formatVersion) {
        return (formatVersion >= 2);
    }

    private boolean isSupportedBlockLength(int blockLength) {
        return (blockLength == SUPPORTED_BLOCK_LENGTH);
    }

    private boolean isSupportedDataType(int dataType) {
        return (dataType == 1 || dataType == 3 || dataType == 13);
    }
}
