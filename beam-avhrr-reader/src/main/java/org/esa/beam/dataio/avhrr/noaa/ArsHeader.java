/*
 * $Id: ArsHeader.java,v 1.1 2006/09/12 11:42:42 marcop Exp $
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
import org.esa.beam.framework.datamodel.MetadataElement;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 13.06.2005
 * Time: 12:00:17
 * To change this template use File | Settings | File Templates.
 */
class ArsHeader {
    private static final String META_DATA_NAME = "ARCHIVE_RETRIEVAL_SYSTEM_HEADER";

    private String costNumber;
    private String classNumber;
    private String orderCreationYear;
    private String orderCreationDay;
    private String processingSiteCode;
    private String processingSoftwareID;
    private String dataSetName;
    private String selectFlag;
    private String beginnigLat;
    private String endingLat;
    private String beginnigLon;
    private String endingLon;
    private String startHour;
    private String startMinutes;
    private String numberOfMinutes;
    private String appendDataFlag;
    private String channelSelectFlags;
    private String dataWordSize;
    private String ascDescFlag;
    private String firstLat;
    private String lastLat;
    private String fistLon;
    private String lastLon;
    private String dataFormat;
    private String sizeOfRecords;
    private String numberOfRecords;

    private Map processingSites;

    public ArsHeader(InputStream header) throws IOException {
        parse(header);

        processingSites = new HashMap();
        processingSites.put("A", "CLASS");
        processingSites.put("S", "NCDC/Suitland");
        processingSites.put("N", "NCDC/Asheville");
    }

    public String getDataWordSize() {
        return dataWordSize;
    }

    public MetadataElement getMetadata() {
        MetadataElement element = new MetadataElement(META_DATA_NAME);

        element.addAttribute(HeaderUtil.createAttribute("COST_NUMBER", costNumber));
        element.addAttribute(HeaderUtil.createAttribute("CLASS_NUMBER", classNumber));
        element.addAttribute(HeaderUtil.createAttribute("ORDER_CREATION_YEAR", orderCreationYear));
        element.addAttribute(HeaderUtil.createAttribute("ORDER_CREATION_DAY_OF_YEAR", orderCreationDay));
        String processingSite = (String) processingSites.get(processingSiteCode);
        if (processingSite == null) {
            processingSite = "unknown Site Code: " + processingSiteCode;
        }
        element.addAttribute(HeaderUtil.createAttribute("PROCESSING_SITE", processingSite));
        element.addAttribute(HeaderUtil.createAttribute("PROCESSING_SOFTWARE_ID", processingSoftwareID));

        element.addElement(getSelectionCriteria());
        element.addElement(getDataSetSummary());
        return element;
    }

    private MetadataElement getSelectionCriteria() {
        String selectionType;
        if (selectFlag.equals("T")) {
            selectionType = "Total Data Set Copy";
        } else if (selectFlag.equals("S")) {
            selectionType = "selective Data Set Copy (Subset)";
        } else {
            selectionType = "unknown: " + selectFlag;
        }

        MetadataElement selection = new MetadataElement("DATA_SELECTION_CRITERIA");
        selection.addAttribute(HeaderUtil.createAttribute("DATA_SET_NAME", dataSetName));
        selection.addAttribute(HeaderUtil.createAttribute("SELECT_FLAG", selectionType));
        selection.addAttribute(HeaderUtil.createAttribute("BEGINNIG_LATITUDE", beginnigLat, AvhrrConstants.UNIT_DEG));
        selection.addAttribute(HeaderUtil.createAttribute("ENDING_LATITUDE", endingLat, AvhrrConstants.UNIT_DEG));
        selection.addAttribute(HeaderUtil.createAttribute("BEGINNIG_LONGITUDE", beginnigLon, AvhrrConstants.UNIT_DEG));
        selection.addAttribute(HeaderUtil.createAttribute("ENDING_LONGITUDE", endingLon, AvhrrConstants.UNIT_DEG));
        selection.addAttribute(HeaderUtil.createAttribute("START_HOUR", startHour));
        selection.addAttribute(HeaderUtil.createAttribute("START_MINUTE", startMinutes));
        selection.addAttribute(HeaderUtil.createAttribute("NUMBER_OF_MINUTES", numberOfMinutes));
        selection.addAttribute(HeaderUtil.createAttribute("APPEND_DATA_FLAG", appendDataFlag));
        selection.addAttribute(HeaderUtil.createAttribute("CHANNEL_SELECT_FLAGS", channelSelectFlags));
        selection.addAttribute(HeaderUtil.createAttribute("SENSOR_DATA_WORD_SIZE", dataWordSize));
        return selection;
    }

    private MetadataElement getDataSetSummary() {
        MetadataElement summary = new MetadataElement("DATA_SET_SUMMARY");

        String ascDescText;
        if (ascDescFlag.equals("A")) {
            ascDescText = "Ascending only";
        } else if (ascDescFlag.equals("D")) {
            ascDescText = "Descending only";
        } else if (ascDescFlag.equals("B")) {
            ascDescText = "Both ascending and descending";
        } else {
            ascDescText = "unkown: " + ascDescFlag;
        }
        summary.addAttribute(HeaderUtil.createAttribute("ASCEND_DESCEND_FLAG", ascDescText));
        summary.addAttribute(HeaderUtil.createAttribute("FIRST_LATITUDE", firstLat, AvhrrConstants.UNIT_DEG, "First latitude value in the first data record"));
        summary.addAttribute(HeaderUtil.createAttribute("LAST_LATITUDE", lastLat, AvhrrConstants.UNIT_DEG, "Last latitude value in the last data record"));
        summary.addAttribute(HeaderUtil.createAttribute("FIRST_LONGITUDE", fistLon, AvhrrConstants.UNIT_DEG, "First longitude value in the first data record"));
        summary.addAttribute(HeaderUtil.createAttribute("LAST_LONGITUDE", lastLon, AvhrrConstants.UNIT_DEG, "Last longitude value in the last data record"));
        summary.addAttribute(HeaderUtil.createAttribute("DATA_FORMAT", dataFormat));
        summary.addAttribute(HeaderUtil.createAttribute("SIZE_OF_RECORDS", sizeOfRecords, AvhrrConstants.UNIT_BYTES));
        summary.addAttribute(HeaderUtil.createAttribute("NUMBER_OF_RECORDS", numberOfRecords, "1", "Total, including ARS and Data Set Header Records"));
        return summary;
    }

    private void parse(InputStream header) throws IOException {
        ExtendedDataInputStream inStream = new ExtendedDataInputStream(header);

        costNumber = inStream.readString(6);
        classNumber = inStream.readString(8);
        orderCreationYear = inStream.readString(4);
        orderCreationDay = inStream.readString(3);
        processingSiteCode = inStream.readString(1);
        processingSoftwareID = inStream.readString(8);

        dataSetName = inStream.readString(42);
        inStream.skip(2);
        selectFlag = inStream.readString(1);
        beginnigLat = inStream.readString(3);
        endingLat = inStream.readString(3);
        beginnigLon = inStream.readString(4);
        endingLon = inStream.readString(4);
        startHour = inStream.readString(2);
        startMinutes = inStream.readString(2);
        numberOfMinutes = inStream.readString(3);
        appendDataFlag = inStream.readString(1);
        channelSelectFlags = inStream.readString(20);
        dataWordSize = inStream.readString(2);

        inStream.skip(27);
        ascDescFlag = inStream.readString(1);
        firstLat = inStream.readString(3);
        lastLat = inStream.readString(3);
        fistLon = inStream.readString(4);
        lastLon = inStream.readString(4);
        dataFormat = inStream.readString(20);
        sizeOfRecords = inStream.readString(6);
        numberOfRecords = inStream.readString(6);
    }
}
