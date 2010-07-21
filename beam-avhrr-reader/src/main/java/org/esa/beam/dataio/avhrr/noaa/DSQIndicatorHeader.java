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
package org.esa.beam.dataio.avhrr.noaa;

import java.io.IOException;
import java.io.InputStream;

import org.esa.beam.dataio.avhrr.HeaderUtil;
import org.esa.beam.framework.datamodel.MetadataElement;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 22.04.2005
 * Time: 10:35:24
 * To change this template use File | Settings | File Templates.
 */
class DSQIndicatorHeader {
    private static final String META_DATA_NAME = "DATA_SET_QUALITY_INDICATORS";

    private int instrumentStatus;
    private int recordOfStatusChange;
    private int secondInstrumentstatus;
    private int dataRecordCounts;
    private int calibratedScans;
    private int missingLinesCount;
    private int dataGapsCount;
    private int framesWithoutSyncE;
    private int pacsCount;
    private int sumAuxErrors;
    private int timeError;
    private int timeErrorCode;
    private int soccClockUpdate;
    private int earthLocationError;
    private int earthLocationErrorCode;
    private int pacsStatus;
    private int pacsDataSource;
    private String ingester;
    private String decommutation;

    public DSQIndicatorHeader(InputStream header) throws IOException {
        parse(header);
    }

    public int getDataRecordCounts() {
        return dataRecordCounts;
    }

    public MetadataElement getMetadata() {
        MetadataElement element = new MetadataElement(META_DATA_NAME);

        element.addAttribute(HeaderUtil.createAttribute("RECORD_NUMBER_OF_STATUS_CHANGE", recordOfStatusChange));
        element.addAttribute(HeaderUtil.createAttribute("COUNT_OF_DATA_RECORDS", dataRecordCounts));
        element.addAttribute(HeaderUtil.createAttribute("COUNT_OF_CALIBRATED_EARTH_LOCATED_SCAN_LINES", calibratedScans));
        element.addAttribute(HeaderUtil.createAttribute("COUNT_OF_MISSING_SCAN_LINES", missingLinesCount));
        element.addAttribute(HeaderUtil.createAttribute("COUNT_OF_DATA_GAPS", dataGapsCount));
        element.addAttribute(HeaderUtil.createAttribute("COUNT_OF_DATA_FRAMES_WITHOUT_FRAME_SYNC_WORD_ERRORS", framesWithoutSyncE));
        element.addAttribute(HeaderUtil.createAttribute("COUNT_OF_PACS_DETECTED_TIP_PARITY_ERRORS", pacsCount));
        element.addAttribute(HeaderUtil.createAttribute("COUNT_OF_ALL_AUXILIARY_SYNC_ERRORS", sumAuxErrors));
        element.addAttribute(HeaderUtil.createAttribute("TIME_SEQUENCE_ERROR", timeError, "record Number"));
        element.addAttribute(HeaderUtil.createAttribute("TIME_SEQUENCE_ERROR_CODE", timeErrorCode));
        element.addAttribute(HeaderUtil.createAttribute("SOCC_CLOCK_UPDATE_INDICATOR", soccClockUpdate, "record Number"));
        element.addAttribute(HeaderUtil.createAttribute("EARTH_LOCATION_ERROR_INDICATOR", earthLocationError, "record Number"));
        element.addAttribute(HeaderUtil.createAttribute("EARTH_LOCATION_ERROR_CODE", earthLocationErrorCode));
        element.addAttribute(HeaderUtil.createAttribute("PACS_STATUS_BIT_FIELD", pacsStatus));
        element.addAttribute(HeaderUtil.createAttribute("PACS_DATA_SOURCE", pacsDataSource));
        element.addAttribute(HeaderUtil.createAttribute("INGESTER", ingester));
        element.addAttribute(HeaderUtil.createAttribute("DECOMUTATION", decommutation));

        element.addElement(getInstrumentStatus("INSTRUMENT_STATUS", instrumentStatus));
        if (recordOfStatusChange != 0) {
            element.addElement(getInstrumentStatus("SECOND_INSTRUMENT_STATUS", secondInstrumentstatus));
        }

        return element;
    }

    private MetadataElement getInstrumentStatus(String title, int status) {
        MetadataElement elem = new MetadataElement(title);
        elem.addAttribute(HeaderUtil.createAttribute("MOTOR_TELEMETRY", status, 15, "off", "on"));
        elem.addAttribute(HeaderUtil.createAttribute("ELECTRONIC_TELEMETRY", status, 14, "off", "on"));
        elem.addAttribute(HeaderUtil.createAttribute("CHANNEL_1_STATUS", status, 13, "disable", "enable"));
        elem.addAttribute(HeaderUtil.createAttribute("CHANNEL_2_STATUS", status, 12, "disable", "enable"));
        elem.addAttribute(HeaderUtil.createAttribute("CHANNEL_3A_STATUS", status, 11, "disable", "enable"));
        elem.addAttribute(HeaderUtil.createAttribute("CHANNEL_3B_STATUS", status, 10, "disable", "enable"));
        elem.addAttribute(HeaderUtil.createAttribute("CHANNEL_4_STATUS", status, 9, "disable", "enable"));
        elem.addAttribute(HeaderUtil.createAttribute("CHANNEL_5_STATUS", status, 8, "disable", "enable"));
        elem.addAttribute(HeaderUtil.createAttribute("CHANNEL_3A_3B_SELECT_STATUS", status, 7, "3a", "3b"));
        elem.addAttribute(HeaderUtil.createAttribute("VOLTAGE_CALIBRATE_STATUS", status, 6, "off", "on"));
        elem.addAttribute(HeaderUtil.createAttribute("COOLER_HEAT", status, 5, "off", "on"));
        elem.addAttribute(HeaderUtil.createAttribute("SCAN_MOTOR", status, 4, "low", "high"));
        elem.addAttribute(HeaderUtil.createAttribute("TELEMETRY_LOCK", status, 3, "off", "lock"));
        elem.addAttribute(HeaderUtil.createAttribute("EARTH_SHIELD", status, 2, "disable", "deploy"));
        elem.addAttribute(HeaderUtil.createAttribute("PATCH_CONTROL", status, 1, "off", "on"));
        return elem;
    }

    private void parse(InputStream header) throws IOException {
        ExtendedDataInputStream inStream = new ExtendedDataInputStream(header);

        instrumentStatus = inStream.readInt();
        inStream.skip(2);
        recordOfStatusChange = inStream.readUnsignedShort();
        secondInstrumentstatus = inStream.readInt();
        dataRecordCounts = inStream.readUnsignedShort();
        calibratedScans = inStream.readUnsignedShort();
        missingLinesCount = inStream.readUnsignedShort();
        dataGapsCount = inStream.readUnsignedShort();
        framesWithoutSyncE = inStream.readUnsignedShort();
        pacsCount = inStream.readUnsignedShort();
        sumAuxErrors = inStream.readUnsignedShort();
        timeError = inStream.readUnsignedShort();
        timeErrorCode = inStream.readUnsignedShort();
        soccClockUpdate = inStream.readUnsignedShort();
        earthLocationError = inStream.readUnsignedShort();
        earthLocationErrorCode = inStream.readUnsignedShort();
        pacsStatus = inStream.readUnsignedShort();
        pacsDataSource = inStream.readUnsignedShort();
        inStream.skip(4);
        ingester = inStream.readString(8);
        decommutation = inStream.readString(8);
    }
}
