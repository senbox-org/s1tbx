package org.esa.snap.dataio.envi;

import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteOrder;
import java.util.Locale;

import static org.junit.Assert.*;

public class HeaderTest {

    @Before
    public void setUp() throws Exception {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Test
    public void testParseNumSamples() throws IOException {
        final BufferedReader in = createReader(createMandatoryHeader());
        final Header header = new Header(in);
        assertEquals(56, header.getNumSamples());
    }

    @Test
    public void testParseNumLines() throws IOException {
        final BufferedReader in = createReader(createMandatoryHeader());
        final Header header = new Header(in);
        assertEquals(105, header.getNumLines());
    }

    @Test
    public void testParseNumBands() throws IOException {
        final int bands = 4;
        final String line = "bands =   " + bands + "\n";
        final BufferedReader in = createReader(createMandatoryHeader() + line);
        final Header header = new Header(in);
        assertEquals(bands, header.getNumBands());
    }

    @Test
    public void testParseHeaderOffset() throws IOException {
        final int offset = 19523;
        final String line = "header offset =   " + offset + "\n";
        final BufferedReader in = createReader(createMandatoryHeader() + line);
        final Header header = new Header(in);

        assertEquals(offset, header.getHeaderOffset());
    }

    @Test
    public void testParseFileType() throws IOException {
        final String type = "toms test type";
        final String line = "file type =   " + type + "\n";
        final BufferedReader in = createReader(createMandatoryHeader() + line);
        final Header header = new Header(in);

        assertEquals(type, header.getFileType());
    }

    @Test
    public void testParseDataType() throws IOException {
        final int dataType = 5;
        final String line = "data type =   " + dataType + "\n";
        final BufferedReader in = createReader(createMandatoryHeader() + line);
        final Header header = new Header(in);

        assertEquals(dataType, header.getDataType());
    }

    @Test
    public void testParseInterleave() throws IOException {
        final BufferedReader in = createReader(createMandatoryHeader());
        final Header header = new Header(in);

        assertEquals("bsq", header.getInterleave());
    }

    @Test
    public void testParseSensorType() throws IOException {
        final String sensorType = "blaberzupp";
        final String line = "sensor type =   " + sensorType + "\n";
        final BufferedReader in = createReader(createMandatoryHeader() + line);
        final Header header = new Header(in);

        assertEquals(sensorType, header.getSensorType());
    }

    @Test
    public void testParseSensorType_NoSensorTypeDefined() throws IOException {
        String mandatoryHeader = createMandatoryHeader();
        final BufferedReader in = createReader(mandatoryHeader);
        final Header header = new Header(in);

        assertEquals(Header.UNKNOWN_SENSOR_TYPE, header.getSensorType());
    }

    @Test
    public void testParseBigEndianByteOrder() throws IOException {
        final int bigEndianByteOrder = 1;
        final String line = "byte order =   " + bigEndianByteOrder + "\n";
        final BufferedReader in = createReader(createMandatoryHeader() + line);
        final Header header = new Header(in);

        assertEquals(bigEndianByteOrder, header.getByteOrder());
        assertSame(ByteOrder.BIG_ENDIAN, header.getJavaByteOrder());
    }

    @Test
    public void testParseLittleEndianByteOrder() throws IOException {
        final int littleEndianByteOrder = 0;
        final String line = "byte order =   " + littleEndianByteOrder + "\n";
        final BufferedReader in = createReader(createMandatoryHeader() + line);
        final Header header = new Header(in);

        assertEquals(littleEndianByteOrder, header.getByteOrder());
        assertSame(ByteOrder.LITTLE_ENDIAN, header.getJavaByteOrder());
    }

    // @todo 2 tb/tb more tests with incomplete description, mis-spelled etc.
    @Test
    public void testParseMapInfo() throws IOException {
        final String mapInfoLine = "map info = {testName, 1.0000, 2.0000, 3.0, 4.0, 5.0, 6.0, WGS-62, units=micrometer}";
        final BufferedReader in = createReader(createMandatoryHeader() + mapInfoLine);
        final Header header = new Header(in);

        final EnviMapInfo mapInfo = header.getMapInfo();
        assertEquals("testName", mapInfo.getProjectionName());
        assertEquals(1.0, mapInfo.getReferencePixelX(), 1e-8);
        assertEquals(2.0, mapInfo.getReferencePixelY(), 1e-8);
        assertEquals(3.0, mapInfo.getEasting(), 1e-8);
        assertEquals(4.0, mapInfo.getNorthing(), 1e-8);
        assertEquals(5.0, mapInfo.getPixelSizeX(), 1e-8);
        assertEquals(6.0, mapInfo.getPixelSizeY(), 1e-8);
        assertEquals("WGS-62", mapInfo.getDatum());
        assertEquals("micrometer", mapInfo.getUnit());
        assertEquals(0, mapInfo.getOrientation(), 1E-5);
    }

    @Test
    public void testParseMapInfo_UTM() throws IOException {
        final String mapInfoLine = "map info = {UTM, 1.000, 1.000, 691415.705, 5974743.844, 9.3500000000e+01, 9.3500000000e+01, 32, North, WGS-84, units=Meters, rotation=-86.00000000}";
        final BufferedReader in = createReader(createMandatoryHeader() + mapInfoLine);
        final Header header = new Header(in);

        final EnviMapInfo mapInfo = header.getMapInfo();
        assertEquals("UTM", mapInfo.getProjectionName());
        assertEquals(1.0, mapInfo.getReferencePixelX(), 1e-8);
        assertEquals(1.0, mapInfo.getReferencePixelY(), 1e-8);
        assertEquals(691415.705, mapInfo.getEasting(), 1e-8);
        assertEquals(5974743.844, mapInfo.getNorthing(), 1e-8);
        assertEquals(9.3500000000e+01, mapInfo.getPixelSizeX(), 1e-8);
        assertEquals(9.3500000000e+01, mapInfo.getPixelSizeY(), 1e-8);
        assertEquals(32, mapInfo.getUtmZone());
        assertEquals("North", mapInfo.getUtmHemisphere());
        assertEquals("WGS-84", mapInfo.getDatum());
        assertEquals("Meters", mapInfo.getUnit());
        assertEquals(-86.0, mapInfo.getOrientation(), 1E-5);
    }

    @Test
    public void testParseMapInfo_multipleLines() throws IOException {
        final String lines = "map info = {testName, 1.0000, 2.0000,\n 3.0, 4.0, 5.0, 6.0, \nWGS-62, units=micrometer}";
        final BufferedReader in = createReader(createMandatoryHeader() + lines);
        final Header header = new Header(in);

        final EnviMapInfo mapInfo = header.getMapInfo();
        assertEquals("testName", mapInfo.getProjectionName());
        assertEquals(1.0, mapInfo.getReferencePixelX(), 1e-8);
        assertEquals(2.0, mapInfo.getReferencePixelY(), 1e-8);
        assertEquals(3.0, mapInfo.getEasting(), 1e-8);
        assertEquals(4.0, mapInfo.getNorthing(), 1e-8);
        assertEquals(5.0, mapInfo.getPixelSizeX(), 1e-8);
        assertEquals(6.0, mapInfo.getPixelSizeY(), 1e-8);
        assertEquals("WGS-62", mapInfo.getDatum());
        assertEquals("micrometer", mapInfo.getUnit());
    }

    // @todo 2 tb/tb more tests with incomplete description, mis-spelled etc.
    @Test
    public void testParseProjectionInfo() throws IOException {
        final String line = "projection info = {9, 6378137.0, 6356752.3, -17.500000, -63.500000, 0.0, 0.0, -32.500000, -2.500000, WGS-84, SamerAlbers, units=Meters}";
        final BufferedReader in = createReader(createMandatoryHeader() + line);
        final Header header = new Header(in);

        EnviProjectionInfo projectionInfo = header.getProjectionInfo();
        assertEquals(9, projectionInfo.getProjectionNumber());
        final double[] parameter = projectionInfo.getParameter();
        assertEquals(8, parameter.length);
        assertEquals(6378137.0, parameter[0], 1e-8);
        assertEquals(6356752.3, parameter[1], 1e-8);
        assertEquals(-17.5, parameter[2], 1e-8);
        assertEquals(-63.5, parameter[3], 1e-8);
        assertEquals(0.0, parameter[4], 1e-8);
        assertEquals(0.0, parameter[5], 1e-8);
        assertEquals(-32.5, parameter[6], 1e-8);
        assertEquals(-2.5, parameter[7], 1e-8);
        assertEquals("WGS-84", projectionInfo.getDatum());
        assertEquals("SamerAlbers", projectionInfo.getName());
    }

    @Test
    public void testParseProjectionInfo_multipleLines() throws IOException {
        final String lines = "projection info = {9, 6378137.0, 6356752.3, \n-17.500000, -63.500000, 0.0, 0.0, \n-32.500000, -2.500000, \nWGS-84, SamerAlbers, units=Meters}";
        final BufferedReader in = createReader(createMandatoryHeader() + lines);
        final Header header = new Header(in);

        EnviProjectionInfo projectionInfo = header.getProjectionInfo();
        assertEquals(9, projectionInfo.getProjectionNumber());
        final double[] parameter = projectionInfo.getParameter();
        assertEquals(8, parameter.length);
        assertEquals(6378137.0, parameter[0], 1e-8);
        assertEquals(6356752.3, parameter[1], 1e-8);
        assertEquals(-17.5, parameter[2], 1e-8);
        assertEquals(-63.5, parameter[3], 1e-8);
        assertEquals(0.0, parameter[4], 1e-8);
        assertEquals(0.0, parameter[5], 1e-8);
        assertEquals(-32.5, parameter[6], 1e-8);
        assertEquals(-2.5, parameter[7], 1e-8);
        assertEquals("WGS-84", projectionInfo.getDatum());
        assertEquals("SamerAlbers", projectionInfo.getName());
    }

    @Test
    public void testParseOffsetValues() throws IOException {
        double[] expectedOffsetValues = {0.01000000, 1.45, 0.0367654321};
        final String line = String.format("data offset values = {%f,%f,\n%.10f}", expectedOffsetValues[0], expectedOffsetValues[1], expectedOffsetValues[2]);
        final BufferedReader in = createReader(createMandatoryHeader() + line);
        final Header header = new Header(in);
        double[] actualOffsetValues = header.getDataOffsetValues();

        assertArrayEquals(expectedOffsetValues, actualOffsetValues,1e-10);
    }

    @Test
    public void testParseGainValues() throws IOException {
        double[] expectedOffsetValues = {10, 0.0045, 1.987654321};
        final String line = String.format("data gain values = {%f\n,%f,%.10f}", expectedOffsetValues[0], expectedOffsetValues[1], expectedOffsetValues[2]);
        final BufferedReader in = createReader(createMandatoryHeader() + line);
        final Header header = new Header(in);
        double[] actualOffsetValues = header.getDataGainValues();

        assertArrayEquals(expectedOffsetValues, actualOffsetValues,1e-10);
    }

    @Test
    public void testParseBandNames_empty() throws IOException {
        final String line = "band names = {}";
        final BufferedReader in = createReader(createMandatoryHeader() + line);
        final Header header = new Header(in);
        final String[] bandNames = header.getBandNames();

        assertEquals(0, bandNames.length);
    }

    @Test
    public void testParseBandNames_noBandnameProperty() throws IOException {
        final BufferedReader in = createReader(createMandatoryHeader());
        final Header header = new Header(in);
        final String[] bandNames = header.getBandNames();

        assertNotNull(bandNames);
        assertEquals(0, bandNames.length);
    }

    @Test
    public void testParseBandNames() throws IOException {
        final String line = "band names = {Karl, Fritzi, Petra}";
        final BufferedReader in = createReader(createMandatoryHeader() + line);
        final Header header = new Header(in);
        final String[] bandNames = header.getBandNames();

        assertEquals(3, bandNames.length);
        assertEquals("Karl", bandNames[0]);
        assertEquals("Fritzi", bandNames[1]);
        assertEquals("Petra", bandNames[2]);
    }

    @Test
    public void testParseBandNames_multipleLines() throws IOException {
        final String line = "band names = {Hugh, \nGrant, \nJulia, \n Roberts\n}";
        final BufferedReader in = createReader(createMandatoryHeader() + line);
        final Header header = new Header(in);
        final String[] bandNames = header.getBandNames();

        assertEquals(4, bandNames.length);
        assertEquals("Hugh", bandNames[0]);
        assertEquals("Grant", bandNames[1]);
        assertEquals("Julia", bandNames[2]);
        assertEquals("Roberts", bandNames[3]);
    }

    @Test
    public void testParseDescription_WithBeamProperties_SensingStartStop() throws IOException {
        final String beamProps = Header.BEAM_PROPERTIES + " = [" + Header.SENSING_START + " = 12324, " + Header.SENSING_STOP + " = 438976]";
        final String description = "any Description\t" + beamProps + "\tany other Description";
        final String line = "description = {" + description + "}";
        final BufferedReader in = createReader(createMandatoryHeader() + line);
        final Header header = new Header(in);

        assertEquals(description, header.getDescription());
        final Header.BeamProperties beamProperties = header.getBeamProperties();
        assertNotNull(beamProperties);
        assertEquals("12324", beamProperties.getSensingStart());
        assertEquals("438976", beamProperties.getSensingStop());
    }

    @Test
    public void testParseDescription_WithoutSensingStartStop() throws IOException {
        final String noStartStop = "[else other one = 12324; else other two = 438976]";
        final String description = "any Description " + noStartStop + " any other Description";
        final String line = "description = {" + description + "}";
        final BufferedReader in = createReader(createMandatoryHeader() + line);
        final Header header = new Header(in);

        assertEquals(description, header.getDescription());
        assertEquals(null, header.getBeamProperties());
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    static String createMandatoryHeader() {
        final int samples = 56;
        final int lines = 105;
        final int dataType = 5;
        final String interleave = "bsq";
        return createMandatoryHeader(samples, lines, dataType, interleave);
    }

    static String createMandatoryHeader(int samples, int lines, int dataType, String interleave) {
        StringBuilder sb = new StringBuilder();
        sb.append("samples = ").append(samples).append("\n");
        sb.append("lines = ").append(lines).append("\n");
        sb.append("data type = ").append(dataType).append("\n");
        sb.append("interleave = ").append(interleave).append("\n");
        return sb.toString();
    }

    private BufferedReader createReader(String line) {
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(line.getBytes())));
    }
}
