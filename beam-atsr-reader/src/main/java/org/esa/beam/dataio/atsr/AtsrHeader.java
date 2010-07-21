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
package org.esa.beam.dataio.atsr;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGrid;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

class AtsrHeader implements AtsrConstants {

    private MetadataElement _sph;
    private MetadataElement _mph;
    private MetadataElement _qads;

    private Vector _tiePointGrids;
    private InputStreamReader _reader;
    private boolean _nadirOnly;
    private boolean _thermalPresent;
    private boolean _visiblePresent;
    private boolean _latlonPresent;
    private boolean _xyPresent;
    private boolean _flagsPresent;
    private String _fileName;
    private String _sensor;

    /**
     * Constructs the object with default parameter
     */
    public AtsrHeader() {
        _tiePointGrids = new Vector();
    }

    /**
     * Parses the data array passed in as ATSR SADIST-2 header data
     */
    public void parse(byte[] rawHeaderData) throws IOException {
        _reader = new InputStreamReader(new ByteArrayInputStream(rawHeaderData));

        createEmptyNodes();

        parseBaseParameter();
        parseOrbitParameter();
        parseClockCalibrationParameter();
        parseProductOptionalContentsParameter();
        parseProductPositionParameters();
        parseInstrumentModesAndTemperatureParameter();
        parseSolarAndViewAngleParameter();
        parseProductConfidenceInformation();
    }

    /**
     * Retrieves the header information as metadata. The information is added to the <code>MetadataElement</code> passed
     * in.
     */
    public MetadataElement getMetadata(MetadataElement root) {
        root.addElement(_mph);
        root.addElement(_sph);
        root.addElement(_qads);
        return root;
    }

    /**
     * Retrieves the number of tie point grids parsed
     */
    public int getNumTiePointGrids() {
        int nRet = 0;

        if (_tiePointGrids != null) {
            nRet = _tiePointGrids.size();
        }

        return nRet;
    }

    /**
     * Retrieves the tie point grid at the given location.
     */
    public TiePointGrid getTiePointGridAt(int nIndex) {
        return (TiePointGrid) _tiePointGrids.elementAt(nIndex);
    }

    /**
     * Adds a tie point grid to the vector.
     */
    void addTiePointGrid(TiePointGrid grid) {
        _tiePointGrids.add(grid);
    }

    /**
     * Retrieves whether the product has only the nadir bands present.
     */
    public boolean isNadirOnly() {
        return _nadirOnly;
    }

    /**
     * Retrieves whether the product has the thermal bands present.
     */
    public boolean isThermalPresent() {
        return _thermalPresent;
    }

    /**
     * Retrieves whether the product has the visible bands present.
     */
    public boolean isVisiblePresent() {
        return _visiblePresent;
    }

    /**
     * Retrieves whether the product has the lat/lon bands present.
     */
    public boolean isLatLonPresent() {
        return _latlonPresent;
    }

    /**
     * Retrieves whether the product has the x/y coordinate bands present.
     */
    public boolean isXYPresent() {
        return _xyPresent;
    }

    /**
     * Retrieves whether the product has the flags band present.
     */
    public boolean areFlagsPresent() {
        return _flagsPresent;
    }

    /**
     * Retrieves the file name as coded in the header
     */
    public String getFileName() {
        return _fileName;
    }

    /**
     * Retrieves the type of sensor.
     */
    public String getSensorType() {
        return _sensor;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates the empty metadata nodes
     */
    private void createEmptyNodes() {
        _mph = new MetadataElement(MPH_NAME);
        _sph = new MetadataElement(SPH_NAME);
        _qads = new MetadataElement(QADS_NAME);
    }

    /**
     * Parses the base parameters <ul> <li>Byte order word</li> <li>Product file name</li> <li>Instrument name</li>
     * </ul>
     */
    private void parseBaseParameter() throws IOException {
        MetadataAttribute attribute = null;

        // read the byte swap word (and forget it gently :-)
        parseString(BYTE_ORDER_SIZE);

        // product file name
        attribute = parseStringAttribute(PRODUCT_FILE_NAME_SIZE, PRODUCT_FILE_NAME_FIELD_NAME, null, null);
        _fileName = attribute.getData().getElemString();
        _mph.addAttribute(attribute);

        // instrument name
        attribute = parseStringAttribute(INSTRUMENT_NAME_SIZE, INSTRUMENT_NAME_FIELD_NAME, null, null);
        _sensor = attribute.getData().getElemString();
        _mph.addAttribute(attribute);
    }

    /**
     * Parses the orbit parameter <ul> <li>state vector type</li> <li>ascending node time</li> <li>universal time at
     * ascending node</li> <li>ascending node state vector position</li> <li>ascending node state vector velocity</li>
     * <li>longitude os ascending node</li> </ul>
     */
    private void parseOrbitParameter() throws IOException {
        MetadataAttribute attribute = null;

        // state vector type
        attribute = parseStringAttribute(STATE_VECTOR_TYPE_SIZE, STATE_VECTOR_FIELD_NAME, null, null);
        _mph.addAttribute(attribute);

        // ascending node time
        attribute = parseFloatAttribute(ASCENDING_NODE_TIME_SIZE, ASCENDING_NODE_TIME_FIELD_NAME,
                                        ASCENDING_NODE_TIME_DESCRIPTION, ASCENDING_NODE_TIME_UNIT);
        _mph.addAttribute(attribute);

        // ascending node universal time
        attribute = parseStringAttribute(ASCENDING_NODE_UT_SIZE, ASCENDING_NODE_UT_FIELD_NAME,
                                         ASCENDING_NODE_UT_DESCRIPTION, null);
        _mph.addAttribute(attribute);

        // ascending node state vector position x
        attribute = parseFloatAttribute(ASCENDING_NODE_STATE_VECTOR_POSITION_SIZE,
                                        ASCENDING_NODE_STATE_VECTOR_POSITION_X_NAME,
                                        ASCENDING_NODE_STATE_VECTOR_POSITION_X_DESCRIPTION,
                                        ASCENDING_NODE_STATE_VECTOR_POSITION_UNIT);
        _mph.addAttribute(attribute);

        // ascending node state vector position y
        attribute = parseFloatAttribute(ASCENDING_NODE_STATE_VECTOR_POSITION_SIZE,
                                        ASCENDING_NODE_STATE_VECTOR_POSITION_Y_NAME,
                                        ASCENDING_NODE_STATE_VECTOR_POSITION_Y_DESCRIPTION,
                                        ASCENDING_NODE_STATE_VECTOR_POSITION_UNIT);
        _mph.addAttribute(attribute);

        // ascending node state vector position z
        attribute = parseFloatAttribute(ASCENDING_NODE_STATE_VECTOR_POSITION_SIZE,
                                        ASCENDING_NODE_STATE_VECTOR_POSITION_Z_NAME,
                                        ASCENDING_NODE_STATE_VECTOR_POSITION_Z_DESCRIPTION,
                                        ASCENDING_NODE_STATE_VECTOR_POSITION_UNIT);
        _mph.addAttribute(attribute);

        // ascending node state vector velocity x
        attribute = parseFloatAttribute(ASCENDING_NODE_STATE_VECTOR_VELOCITY_SIZE,
                                        ASCENDING_NODE_STATE_VECTOR_VELOCITY_X_NAME,
                                        ASCENDING_NODE_STATE_VECTOR_VELOCITY_X_DESCRIPTION,
                                        ASCENDING_NODE_STATE_VECTOR_VELOCITY_UNIT);
        _mph.addAttribute(attribute);

        // ascending node state vector velocity y
        attribute = parseFloatAttribute(ASCENDING_NODE_STATE_VECTOR_VELOCITY_SIZE,
                                        ASCENDING_NODE_STATE_VECTOR_VELOCITY_Y_NAME,
                                        ASCENDING_NODE_STATE_VECTOR_VELOCITY_Y_DESCRIPTION,
                                        ASCENDING_NODE_STATE_VECTOR_VELOCITY_UNIT);
        _mph.addAttribute(attribute);

        // ascending node state vector velocity z
        attribute = parseFloatAttribute(ASCENDING_NODE_STATE_VECTOR_VELOCITY_SIZE,
                                        ASCENDING_NODE_STATE_VECTOR_VELOCITY_Z_NAME,
                                        ASCENDING_NODE_STATE_VECTOR_VELOCITY_Z_DESCRIPTION,
                                        ASCENDING_NODE_STATE_VECTOR_VELOCITY_UNIT);
        _mph.addAttribute(attribute);

        // ascending node longitude
        attribute = parseStringAttribute(ASCENDING_NODE_LON_SIZE, ASCENDING_NODE_LON_NAME,
                                         ASCENDING_NODE_LON_DESCRIPTION, ASCENDING_NODE_LON_UNIT);
        _mph.addAttribute(attribute);
    }

    /**
     * Parses the clock calibration parameter <ul> <li>reference universal time</li> <li>reference ERS satellite
     * clock</li> <li>satellite clock period</li> </ul>
     */
    private void parseClockCalibrationParameter() throws IOException {
        float fData = 0.f;
        long nData = 0;
        MetadataAttribute attribute = null;
        ProductData data = null;

        // reference universal time
        fData = parseFloat(REFERENCE_UT_SIZE);
        data = ProductData.createInstance(new float[]{fData});
        attribute = new MetadataAttribute(REFERENCE_UT_FIELD_NAME, data, true);
        attribute.setUnit(REFERENCE_UT_UNIT);
        attribute.setDescription(REFERENCE_UT_DESCRIPTION);
        _mph.addAttribute(attribute);

        // reference ERS satellite clock time
        nData = parseLong(REFERENCE_ERS_CLOCK_SIZE);
        data = ProductData.createInstance(ProductData.TYPE_UINT32, 1);
        data.setElemUInt(nData);
        attribute = new MetadataAttribute(REFERENCE_ERS_CLOCK_TIME_NAME, data, true);
        attribute.setUnit(REFERENCE_ERS_CLOCK_UNIT);
        attribute.setDescription(REFERENCE_ERS_CLOCK_TIME_DESCRIPTION);
        _mph.addAttribute(attribute);

        // reference ERS satellite clock period
        nData = parseLong(REFERENCE_ERS_CLOCK_SIZE);
        data = ProductData.createInstance(ProductData.TYPE_UINT32, 1);
        data.setElemUInt(nData);
        attribute = new MetadataAttribute(REFERENCE_ERS_CLOCK_PERIOD_NAME, data, true);
        attribute.setUnit(REFERENCE_ERS_CLOCK_UNIT);
        attribute.setDescription(REFERENCE_ERS_CLOCK_PERIOD_DESCRIPTION);
        _mph.addAttribute(attribute);
    }

    /**
     * Parses the product optional content  parameter <ul> <li>nadir only records present</li> <li>Thermal infra red
     * detector records present</li> <li>Visible/near infra-red records present</li> <li>lat/lon records present</li>
     * <li>X/Y coordinates records present</li> <li>Cloudclearing/land flagging records present</li> </ul>
     */
    private void parseProductOptionalContentsParameter() throws IOException {

        // nadir only present
        int value = parseAndAddAttributeToSPH(NADIR_ONLY_PRESENT_NAME, NADIR_ONLY_DESCRIPTION);
        _nadirOnly = value > 0;

        // thermal bands present
        value = parseAndAddAttributeToSPH(THERMAL_PRESENT_NAME, THERMAL_PRESENT_DESCRIPTION);
        _thermalPresent = value > 0;

        // visible bands present
        value = parseAndAddAttributeToSPH(VISIBLE_PRESENT_NAME, VISIBLE_PRESENT_DESCRIPTION);
        _visiblePresent = value > 0;

        // lat/lon records present
        value = parseAndAddAttributeToSPH(LAT_LON_PRESENT_NAME, LAT_LON_PRESENT_DESCRIPTION);
        _latlonPresent = value > 0;

        // x/y records present
        value = parseAndAddAttributeToSPH(X_Y_PRESENT_NAME, X_Y_PRESENT_DESCRIPTION);
        _xyPresent = value > 0;

        // flags present
        value = parseAndAddAttributeToSPH(FLAGS_PRESENT_NAME, FLAGS_PRESENT_DESCRIPTION);
        _flagsPresent = value > 0;
    }

    /**
     * @param attributeName        the name of the attribute
     * @param attributeDescription the descrption of the atttribute
     *
     * @return the <code>int</code> value of the attribute
     *
     * @throws IOException
     */
    private int parseAndAddAttributeToSPH(String attributeName, String attributeDescription) throws IOException {
        int nData = parseInt(RECORD_CONTENTS_SIZE);
        ProductData data = ProductData.createInstance(new int[]{nData});
        MetadataAttribute attribute = new MetadataAttribute(attributeName, data, true);
        attribute.setDescription(attributeDescription);
        _sph.addAttribute(attribute);
        return nData;
    }

    /**
     * Parses the product position and time parameter <ul> <li>alog-track distances of start and end</li> <li>UT of data
     * acquisition</li> <li>lat/lon of product corners</li> </ul>
     */
    private void parseProductPositionParameters() throws IOException {
        MetadataAttribute attribute = null;

        // track distance of product start
        attribute = parseIntAttribute(TRACK_DISTANCE_SIZE, TRACK_DISTANCE_START_NAME,
                                      TRACK_DISTANCE_START_DESCRIPTION, TRACK_DISTANCE_UNIT);
        _sph.addAttribute(attribute);

        // track distance of product end
        attribute = parseIntAttribute(TRACK_DISTANCE_SIZE, TRACK_DISTANCE_END_NAME,
                                      TRACK_DISTANCE_END_DESCRIPTION, TRACK_DISTANCE_UNIT);
        _sph.addAttribute(attribute);

        // product start time
        attribute = parseStringAttribute(PRODUCT_TIME_SIZE, UT_PRODUCT_START_NAME,
                                         UT_PRODUCT_START_DESCRIPTION, null);
        _sph.addAttribute(attribute);

        // product end time
        attribute = parseStringAttribute(PRODUCT_TIME_SIZE, UT_PRODUCT_END_NAME,
                                         UT_PRODUCT_END_DESCRIPTION, null);
        _sph.addAttribute(attribute);

        // product corner lat lhs at start
        attribute = parseFloatAttribute(CORNER_LAT_SIZE, CORNER_LAT_LHS_START_NAME,
                                        CORNER_LAT_LHS_START_DESCRIPTION, CORNER_LAT_LON_UNITS);
        _sph.addAttribute(attribute);

        // product corner lat rhs at start
        attribute = parseFloatAttribute(CORNER_LAT_SIZE, CORNER_LAT_RHS_START_NAME,
                                        CORNER_LAT_RHS_START_DESCRIPTION, CORNER_LAT_LON_UNITS);
        _sph.addAttribute(attribute);

        // product corner lat lhs at end
        attribute = parseFloatAttribute(CORNER_LAT_SIZE, CORNER_LAT_LHS_END_NAME,
                                        CORNER_LAT_LHS_END_DESCRIPTION, CORNER_LAT_LON_UNITS);
        _sph.addAttribute(attribute);

        // product corner lat rhs at end
        attribute = parseFloatAttribute(CORNER_LAT_SIZE, CORNER_LAT_RHS_END_NAME,
                                        CORNER_LAT_RHS_END_DESCRIPTION, CORNER_LAT_LON_UNITS);
        _sph.addAttribute(attribute);

        // product corner lon lhs at start
        attribute = parseFloatAttribute(CORNER_LON_SIZE, CORNER_LON_LHS_START_NAME,
                                        CORNER_LON_LHS_START_DESCRIPTION, CORNER_LAT_LON_UNITS);
        _sph.addAttribute(attribute);

        // product corner lon rhs at start
        attribute = parseFloatAttribute(CORNER_LON_SIZE, CORNER_LON_RHS_START_NAME,
                                        CORNER_LON_RHS_START_DESCRIPTION, CORNER_LAT_LON_UNITS);
        _sph.addAttribute(attribute);

        // product corner lon lhs at end
        attribute = parseFloatAttribute(CORNER_LON_SIZE, CORNER_LON_LHS_END_NAME,
                                        CORNER_LON_LHS_END_DESCRIPTION, CORNER_LAT_LON_UNITS);
        _sph.addAttribute(attribute);

        // product corner lon rhs at end
        attribute = parseFloatAttribute(CORNER_LON_SIZE, CORNER_LON_RHS_END_NAME,
                                        CORNER_LON_RHS_END_DESCRIPTION, CORNER_LAT_LON_UNITS);
        _sph.addAttribute(attribute);
    }

    /**
     * Parses the instrument modes and temperature parameter <ul> <li>alog-track distances of start and end</li> <li>UT
     * of data acquisition</li> <li>lat/lon of product corners</li> </ul>
     */
    private void parseInstrumentModesAndTemperatureParameter() throws IOException {
        int[] dataArray = new int[2];
        ProductData data = null;
        MetadataAttribute attribute = null;

        dataArray[0] = parseInt(PIXEL_SELECTION_MAP_SIZE);
        dataArray[1] = parseInt(PIXEL_SELECTION_MAP_SIZE);
        data = ProductData.createInstance(dataArray);
        attribute = new MetadataAttribute(PIXEL_SELECTION_MAP_NADIR_NAME, data, true);
        attribute.setDescription(PIXEL_SELECTION_MAP_NADIR_DESCRIPTION);
        _sph.addAttribute(attribute);

        attribute = parseIntAttribute(PSM_CHANGE_SIZE, PSM_CHANGE_NADIR_NAME,
                                      PSM_CHANGE_NADIR_DESCRIPTION, PSM_CHANGE_UNIT);
        _sph.addAttribute(attribute);

        dataArray[0] = parseInt(PIXEL_SELECTION_MAP_SIZE);
        dataArray[1] = parseInt(PIXEL_SELECTION_MAP_SIZE);
        data = ProductData.createInstance(dataArray);
        attribute = new MetadataAttribute(PIXEL_SELECTION_MAP_FORWARD_NAME, data, true);
        attribute.setDescription(PIXEL_SELECTION_MAP_FORWARD_DESCRIPTION);
        _sph.addAttribute(attribute);

        attribute = parseIntAttribute(PSM_CHANGE_SIZE, PSM_CHANGE_FORWARD_NAME,
                                      PSM_CHANGE_FORWARD_DESCRIPTION, PSM_CHANGE_UNIT);
        _sph.addAttribute(attribute);

        attribute = parseStringAttribute(ATSR2_DATA_RATE_SIZE, ATSR2_DATA_RATE_NADIR_NAME,
                                         ATSR2_DATA_RATE_NADIR_DESCRIPTION, null);
        _sph.addAttribute(attribute);

        attribute = parseIntAttribute(ATSR2_DATA_RATE_CHANGE_SIZE, ATSR2_DATA_RATE_CHANGE_NADIR_NAME,
                                      ATSR2_DATA_RATE_CHANGE_NADIR_DESCRIPTION, ATSR2_DATA_RATE_CHANGE_UNIT);
        _sph.addAttribute(attribute);

        attribute = parseStringAttribute(ATSR2_DATA_RATE_SIZE, ATSR2_DATA_RATE_FORWARD_NAME,
                                         ATSR2_DATA_RATE_FORWARD_DESCRIPTION, null);
        _sph.addAttribute(attribute);

        attribute = parseIntAttribute(ATSR2_DATA_RATE_CHANGE_SIZE, ATSR2_DATA_RATE_CHANGE_FORWARD_NAME,
                                      ATSR2_DATA_RATE_CHANGE_FORWARD_DESCRIPTION, ATSR2_DATA_RATE_CHANGE_UNIT);
        _sph.addAttribute(attribute);

        attribute = parseFloatAttribute(TEMPERATURES_SIZE, MIN_SCC_TEMPERATURE_NAME,
                                        MIN_SCC_TEMPERATURE_DESCRIPTION, TEMPERATURES_UNIT);
        _sph.addAttribute(attribute);

        attribute = parseFloatAttribute(TEMPERATURES_SIZE, MIN_INSTRUMENT_TEMPERATURE_1200_NM_NAME,
                                        MIN_INSTRUMENT_TEMPERATURE_1200_NM_DESCRIPTION, TEMPERATURES_UNIT);
        _sph.addAttribute(attribute);

        attribute = parseFloatAttribute(TEMPERATURES_SIZE, MIN_INSTRUMENT_TEMPERATURE_1100_NM_NAME,
                                        MIN_INSTRUMENT_TEMPERATURE_1100_NM_DESCRIPTION, TEMPERATURES_UNIT);
        _sph.addAttribute(attribute);

        attribute = parseFloatAttribute(TEMPERATURES_SIZE, MIN_INSTRUMENT_TEMPERATURE_370_NM_NAME,
                                        MIN_INSTRUMENT_TEMPERATURE_370_NM_DESCRIPTION, TEMPERATURES_UNIT);
        _sph.addAttribute(attribute);

        attribute = parseFloatAttribute(TEMPERATURES_SIZE, MIN_INSTRUMENT_TEMPERATURE_160_NM_NAME,
                                        MIN_INSTRUMENT_TEMPERATURE_160_NM_DESCRIPTION, TEMPERATURES_UNIT);
        _sph.addAttribute(attribute);

        attribute = parseFloatAttribute(TEMPERATURES_SIZE, MIN_INSTRUMENT_TEMPERATURE_87_NM_NAME,
                                        MIN_INSTRUMENT_TEMPERATURE_87_NM_DESCRIPTION, TEMPERATURES_UNIT);
        _sph.addAttribute(attribute);

        attribute = parseFloatAttribute(TEMPERATURES_SIZE, MAX_SCC_TEMPERATURE_NAME,
                                        MAX_SCC_TEMPERATURE_DESCRIPTION, TEMPERATURES_UNIT);
        _sph.addAttribute(attribute);

        attribute = parseFloatAttribute(TEMPERATURES_SIZE, MAX_INSTRUMENT_TEMPERATURE_1200_NM_NAME,
                                        MAX_INSTRUMENT_TEMPERATURE_1200_NM_DESCRIPTION, TEMPERATURES_UNIT);
        _sph.addAttribute(attribute);

        attribute = parseFloatAttribute(TEMPERATURES_SIZE, MAX_INSTRUMENT_TEMPERATURE_1100_NM_NAME,
                                        MAX_INSTRUMENT_TEMPERATURE_1100_NM_DESCRIPTION, TEMPERATURES_UNIT);
        _sph.addAttribute(attribute);

        attribute = parseFloatAttribute(TEMPERATURES_SIZE, MAX_INSTRUMENT_TEMPERATURE_370_NM_NAME,
                                        MAX_INSTRUMENT_TEMPERATURE_370_NM_DESCRIPTION, TEMPERATURES_UNIT);
        _sph.addAttribute(attribute);

        attribute = parseFloatAttribute(TEMPERATURES_SIZE, MAX_INSTRUMENT_TEMPERATURE_160_NM_NAME,
                                        MAX_INSTRUMENT_TEMPERATURE_160_NM_DESCRIPTION, TEMPERATURES_UNIT);
        _sph.addAttribute(attribute);

        attribute = parseFloatAttribute(TEMPERATURES_SIZE, MAX_INSTRUMENT_TEMPERATURE_87_NM_NAME,
                                        MAX_INSTRUMENT_TEMPERATURE_87_NM_DESCRIPTION, TEMPERATURES_UNIT);
        _sph.addAttribute(attribute);
    }

    /**
     * Parses the instrument and view angles <ul> <li>solar elevations</li> <li>satellite elevations</li> <li>solar
     * azimuth</li> <li>satellite azimuth</li> </ul> for nadir and forward view
     */
    private void parseSolarAndViewAngleParameter() throws IOException {
        parseTiePointGrid(SUN_ELEVATION_NADIR_NAME, SUN_ELEVATION_NADIR_DESCRIPTION);
        parseTiePointGrid(VIEW_ELEVATION_NADIR_NAME, VIEW_ELEVATION_NADIR_DESCRIPTION);
        parseTiePointGrid(SUN_AZIMUTH_NADIR_NAME, SUN_AZIMUTH_NADIR_DESCRIPTION);
        parseTiePointGrid(VIEW_AZIMUTH_NADIR_NAME, VIEW_AZIMUTH_NADIR_DESCRIPTION);
        parseTiePointGrid(SUN_ELEVATION_FORWARD_NAME, SUN_ELEVATION_FORWARD_DESCRIPTION);
        parseTiePointGrid(VIEW_ELEVATION_FORWARD_NAME, VIEW_ELEVATION_FORWARD_DESCRIPTION);
        parseTiePointGrid(SUN_AZIMUTH_FORWARD_NAME, SUN_AZIMUTH_FORWARD_DESCRIPTION);
        parseTiePointGrid(VIEW_AZIMUTH_FORWARD_NAME, VIEW_AZIMUTH_FORWARD_DESCRIPTION);
    }

    /**
     * Parses the product confidence information <ul> <li>ERS platform modes</li> <li>Acquisition PCD information</li>
     * <li>Packet validation</li> <li>maximum pixel error</li> </ul> for nadir and forward view
     */
    private void parseProductConfidenceInformation() throws IOException {
        MetadataAttribute attribute = null;

        attribute = parseIntAttribute(CONFIDENCE_SIZE, ERS_MODE_YSM_NADIR_NAME,
                                      ERS_MODE_YSM_NADIR_DESCRIPTION, null);
        _qads.addAttribute(attribute);

        attribute = parseIntAttribute(CONFIDENCE_SIZE, ERS_MODE_FCM_NADIR_NAME,
                                      ERS_MODE_FCM_NADIR_DESCRIPTION, null);
        _qads.addAttribute(attribute);

        attribute = parseIntAttribute(CONFIDENCE_SIZE, ERS_MODE_OCM_NADIR_NAME,
                                      ERS_MODE_OCM_NADIR_DESCRIPTION, null);
        _qads.addAttribute(attribute);

        attribute = parseIntAttribute(CONFIDENCE_SIZE, ERS_MODE_FPM_NADIR_NAME,
                                      ERS_MODE_FPM_NADIR_DESCRIPTION, null);
        _qads.addAttribute(attribute);

        attribute = parseIntAttribute(CONFIDENCE_SIZE, ERS_MODE_RTMM_NADIR_NAME,
                                      ERS_MODE_RTMM_NADIR_DESCRIPTION, null);
        _qads.addAttribute(attribute);

        attribute = parseIntAttribute(CONFIDENCE_SIZE, ERS_MODE_RTMC_NADIR_NAME,
                                      ERS_MODE_RTMC_NADIR_DESCRIPTION, null);
        _qads.addAttribute(attribute);

        attribute = parseIntAttribute(CONFIDENCE_SIZE, ERS_MODE_YSM_FORWARD_NAME,
                                      ERS_MODE_YSM_FORWARD_DESCRIPTION, null);
        _qads.addAttribute(attribute);

        attribute = parseIntAttribute(CONFIDENCE_SIZE, ERS_MODE_FCM_FORWARD_NAME,
                                      ERS_MODE_FCM_FORWARD_DESCRIPTION, null);
        _qads.addAttribute(attribute);

        attribute = parseIntAttribute(CONFIDENCE_SIZE, ERS_MODE_OCM_FORWARD_NAME,
                                      ERS_MODE_OCM_FORWARD_DESCRIPTION, null);
        _qads.addAttribute(attribute);

        attribute = parseIntAttribute(CONFIDENCE_SIZE, ERS_MODE_FPM_FORWARD_NAME,
                                      ERS_MODE_FPM_FORWARD_DESCRIPTION, null);
        _qads.addAttribute(attribute);

        attribute = parseIntAttribute(CONFIDENCE_SIZE, ERS_MODE_RTMM_FORWARD_NAME,
                                      ERS_MODE_RTMM_FORWARD_DESCRIPTION, null);
        _qads.addAttribute(attribute);

        attribute = parseIntAttribute(CONFIDENCE_SIZE, ERS_MODE_RTMC_FORWARD_NAME,
                                      ERS_MODE_RTMC_FORWARD_DESCRIPTION, null);
        _qads.addAttribute(attribute);

        attribute = parseIntArrayAttribute(CONFIDENCE_SIZE, NUM_PCD_SETS, PCD_INFO_NADIR_NAME,
                                           PCD_INFO_NADIR_DESCRIPTION, null);
        _qads.addAttribute(attribute);

        attribute = parseIntArrayAttribute(CONFIDENCE_SIZE, NUM_PCD_SETS, PCD_INFO_FORWARD_NAME,
                                           PCD_INFO_FORWARD_DESCRIPTION, null);
        _qads.addAttribute(attribute);

        attribute = parseIntArrayAttribute(CONFIDENCE_SIZE, NUM_PACKET_SETS, PACKET_INFO_NADIR_NAME,
                                           PACKET_INFO_NADIR_DESCRIPTION, null);
        _qads.addAttribute(attribute);

        attribute = parseIntArrayAttribute(CONFIDENCE_SIZE, NUM_PACKET_SETS, PACKET_INFO_FORWARD_NAME,
                                           PACKET_INFO_FORWARD_DESCRIPTION, null);
        _qads.addAttribute(attribute);

        attribute = parseIntAttribute(PIXEL_ERROR_SIZE, MAX_PIXEL_ERROR_CODE_NAME,
                                      MAX_PIXEL_ERROR_CODE_DESCRIPTION, null);
        _qads.addAttribute(attribute);
    }

    /**
     * Parses <code>size</code> bytes from the stream and creates a string of it.
     */
    private String parseString(int size) throws IOException {
        char[] array = new char[size];
        String strRet;

        _reader.read(array, 0, size);
        strRet = new String(array);
        strRet = strRet.trim();

        return strRet;
    }

    /**
     * Parses <code>size</code> bytes from the stream and creates a float of it.
     */
    private float parseFloat(int size) throws IOException {
        char[] array = new char[size];

        _reader.read(array, 0, size);

        return Float.parseFloat(new String(array));
    }

    /**
     * Parses <code>numElems * size</code> bytes from the stream and creates a float array of numElems elements of it.
     */
    private float[] parseFloatArray(int size, int numElems) throws IOException {
        char[] array = new char[size];
        float[] fRet = new float[numElems];

        for (int n = 0; n < numElems; n++) {
            _reader.read(array, 0, size);
            fRet[n] = Float.parseFloat(new String(array));
        }

        return fRet;
    }

    /**
     * Parses <code>size</code> bytes from the stream and creates an int of it.
     */
    private int parseInt(int size) throws IOException {
        char[] array = new char[size];

        _reader.read(array, 0, size);
        String bufString = new String(array);
        bufString = bufString.trim();

        return Integer.parseInt(bufString);
    }

    /**
     * Parses <code>numElems * size</code> bytes from the stream and creates an int array of numElems elements of it.
     */
    private int[] parseIntArray(int size, int numElems) throws IOException {
        char[] array = new char[size];
        int[] nRet = new int[numElems];
        String str = null;

        for (int n = 0; n < numElems; n++) {
            _reader.read(array, 0, size);
            str = new String(array);
            str = str.trim();
            nRet[n] = Integer.parseInt(str);
        }

        return nRet;
    }

    /**
     * Parses <code>size</code> bytes from the stream and creates a long of it.
     */
    private long parseLong(int size) throws IOException {
        char[] array = new char[size];

        _reader.read(array, 0, size);
        String bufString = new String(array);
        bufString = bufString.trim();

        return Long.parseLong(bufString);
    }

    /**
     * Parses a string attribute from the stream
     */
    private MetadataAttribute parseStringAttribute(int size, String name,
                                                   String description, String unit) throws IOException {
        String string = null;
        MetadataAttribute attribute = null;
        ProductData data = null;

        string = parseString(size);
        data = ProductData.createInstance(string);
        attribute = new MetadataAttribute(name, data, true);
        if (description != null) {
            attribute.setDescription(description);
        }
        if (unit != null) {
            attribute.setUnit(unit);
        }

        return attribute;
    }

    /**
     * Parses an integer attribute from the stream
     */
    private MetadataAttribute parseIntAttribute(int size, String name,
                                                String description, String unit) throws IOException {
        int nData = 0;
        MetadataAttribute attribute = null;
        ProductData data = null;

        nData = parseInt(size);
        data = ProductData.createInstance(new int[]{nData});
        attribute = new MetadataAttribute(name, data, true);
        if (description != null) {
            attribute.setDescription(description);
        }
        if (unit != null) {
            attribute.setUnit(unit);
        }

        return attribute;
    }

    /**
     * Parses an integer array attribute from the stream
     */
    private MetadataAttribute parseIntArrayAttribute(int size, int numElems, String name,
                                                     String description, String unit) throws IOException {
        int[] nData = null;
        MetadataAttribute attribute = null;
        ProductData data = null;

        nData = parseIntArray(size, numElems);
        data = ProductData.createInstance(nData);
        attribute = new MetadataAttribute(name, data, true);
        if (description != null) {
            attribute.setDescription(description);
        }
        if (unit != null) {
            attribute.setUnit(unit);
        }

        return attribute;
    }

    /**
     * Parses a float attribute from the stream
     */
    private MetadataAttribute parseFloatAttribute(int size, String name,
                                                  String description, String unit) throws IOException {
        float fData = 0.f;
        MetadataAttribute attribute = null;
        ProductData data = null;

        fData = parseFloat(size);
        data = ProductData.createInstance(new float[]{fData});
        attribute = new MetadataAttribute(name, data, true);
        if (description != null) {
            attribute.setDescription(description);
        }
        if (unit != null) {
            attribute.setUnit(unit);
        }

        return attribute;
    }

    /**
     * Parses a complete tie point grid and adds it to the tie poinzts grids vector
     */
    private void parseTiePointGrid(String tiePtName, String description) throws IOException {
        TiePointGrid grid = null;
        int arraySize = ATSR_TIE_PT_GRID_WIDTH * ATSR_TIE_PT_GRID_HEIGHT;

        float[] fData = parseFloatArray(ANGLE_PARAMETER_SIZE, arraySize);

        grid = new TiePointGrid(tiePtName, ATSR_TIE_PT_GRID_WIDTH, ATSR_TIE_PT_GRID_HEIGHT,
                                ATSR_TIE_PT_OFFS_X, 0, ATSR_TIE_PT_SUBS_X, ATSR_TIE_PT_SUBS_Y, fData);
        grid.setUnit(ANGLE_UNIT);
        if (description != null) {
            grid.setDescription(description);
        }
        _tiePointGrids.add(grid);
    }
}
