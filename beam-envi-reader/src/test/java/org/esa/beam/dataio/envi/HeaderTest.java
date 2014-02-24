package org.esa.beam.dataio.envi;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteOrder;
import java.util.Locale;

public class HeaderTest extends TestCase {


    @Override
    public void setUp() throws Exception {
        Locale.setDefault(Locale.ENGLISH);
    }

    public void testParseNumSamples() throws IOException {
        final int samples = 56;
        final String line = "samples = " + samples + "\n";
        final BufferedReader in = createReader(line);

        final Header header = new Header(in);

        assertEquals(samples, header.getNumSamples());
    }

    public void testParseNumLines() throws IOException {
        final int lines = 105;
        final String line = "lines = " + lines + "\n";
        final BufferedReader in = createReader(line);

        final Header header = new Header(in);

        assertEquals(lines, header.getNumLines());
    }

    public void testParseNumBands() throws IOException {
        final int bands = 4;
        final String line = "bands =   " + bands + "\n";
        final BufferedReader in = createReader(line);

        final Header header = new Header(in);

        assertEquals(bands, header.getNumBands());
    }

    public void testParseHeaderOffset() throws IOException {
        final int offset = 19523;
        final String line = "header offset =   " + offset + "\n";
        final BufferedReader in = createReader(line);

        final Header header = new Header(in);

        assertEquals(offset, header.getHeaderOffset());
    }

    public void testParseFileType() throws IOException {
        final String type = "toms test type";
        final String line = "file type =   " + type + "\n";
        final BufferedReader in = createReader(line);

        final Header header = new Header(in);

        assertEquals(type, header.getFileType());
    }

    public void testParseDataType() throws IOException {
        final int dataType = 5;
        final String line = "data type =   " + dataType + "\n";
        final BufferedReader in = createReader(line);

        final Header header = new Header(in);

        assertEquals(dataType, header.getDataType());
    }

    public void testParseInterleave() throws IOException {
        final String interleave = "bsq";
        final String line = "interleave =   " + interleave + "\n";
        final BufferedReader in = createReader(line);

        final Header header = new Header(in);

        assertEquals(interleave, header.getInterleave());
    }

    public void testParseSensorType() throws IOException {
        final String sensorType = "blaberzupp";
        final String line = "sensor type =   " + sensorType + "\n";
        final BufferedReader in = createReader(line);

        final Header header = new Header(in);

        assertEquals(sensorType, header.getSensorType());
    }

    public void testParseSensorType_NoSensorTypeDefined() throws IOException {
        final String line = "   \n";
        final BufferedReader in = createReader(line);

        final Header header = new Header(in);

        assertEquals(Header.UNKNOWN_SENSOR_TYPE, header.getSensorType());
    }

    public void testParseBigEndianByteOrder() throws IOException {
        final int bigEndianByteOrder = 1;
        final String line = "byte order =   " + bigEndianByteOrder + "\n";
        final BufferedReader in = createReader(line);

        final Header header = new Header(in);

        assertEquals(bigEndianByteOrder, header.getByteOrder());
        assertSame(ByteOrder.BIG_ENDIAN, header.getJavaByteOrder());
    }

    public void testParseLittleEndianByteOrder() throws IOException {
        final int littleEndianByteOrder = 0;
        final String line = "byte order =   " + littleEndianByteOrder + "\n";
        final BufferedReader in = createReader(line);

        final Header header = new Header(in);

        assertEquals(littleEndianByteOrder, header.getByteOrder());
        assertSame(ByteOrder.LITTLE_ENDIAN, header.getJavaByteOrder());
    }

    // @todo 2 tb/tb more tests with incomplete description, mis-spelled etc.
    public void testParseMapInfo() throws IOException {
        final String mapInfoLine = "map info = {testName, 1.0000, 2.0000, 3.0, 4.0, 5.0, 6.0, WGS-62, units=micrometer}";
        final BufferedReader in = createReader(mapInfoLine);


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

    public void testParseMapInfo_UTM() throws IOException {
        final String mapInfoLine = "map info = {UTM, 1.000, 1.000, 691415.705, 5974743.844, 9.3500000000e+01, 9.3500000000e+01, 32, North, WGS-84, units=Meters, rotation=-86.00000000}";
        final BufferedReader in = createReader(mapInfoLine);

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

    public void testParseMapInfo_multipleLines() throws IOException {
        final String lines = "map info = {testName, 1.0000, 2.0000,\n 3.0, 4.0, 5.0, 6.0, \nWGS-62, units=micrometer}";
        final BufferedReader in = createReader(lines);

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
    public void testParseProjectionInfo() throws IOException {
        final String line = "projection info = {9, 6378137.0, 6356752.3, -17.500000, -63.500000, 0.0, 0.0, -32.500000, -2.500000, WGS-84, SamerAlbers, units=Meters}";
        final BufferedReader in = createReader(line);

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

    public void testParseProjectionInfo_multipleLines() throws IOException {
        final String lines = "projection info = {9, 6378137.0, 6356752.3, \n-17.500000, -63.500000, 0.0, 0.0, \n-32.500000, -2.500000, \nWGS-84, SamerAlbers, units=Meters}";
        final BufferedReader in = createReader(lines);

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

    public void testParseOffsetValues() throws IOException {
        double[] expectedOffsetValues = {0.01000000, 1.45, 0.0367654321};
        final String line = String.format("data offset values = {%f,%f,\n%.10f}", expectedOffsetValues[0], expectedOffsetValues[1], expectedOffsetValues[2]);
        final BufferedReader in = createReader(line);

        final Header header = new Header(in);
        double[] actualOffsetValues = header.getDataOffsetValues();

        for (int i = 0; i < expectedOffsetValues.length; i++) {
            assertEquals(expectedOffsetValues[i], actualOffsetValues[i]);
        }
    }

    public void testParseGainValues() throws IOException {
        double[] expectedOffsetValues = {10, 0.0045, 1.987654321};
        final String line = String.format("data gain values = {%f\n,%f,%.10f}", expectedOffsetValues[0], expectedOffsetValues[1], expectedOffsetValues[2]);
        final BufferedReader in = createReader(line);

        final Header header = new Header(in);
        double[] actualOffsetValues = header.getDataGainValues();

        for (int i = 0; i < expectedOffsetValues.length; i++) {
            assertEquals(expectedOffsetValues[i], actualOffsetValues[i]);
        }
    }

    public void testParseBandNames_empty() throws IOException {
        final String line = "band names = {}";

        final BufferedReader in = createReader(line);

        final Header header = new Header(in);
        final String[] bandNames = header.getBandNames();
        assertEquals(0, bandNames.length);
    }

    public void testParseBandNames_noBandnameProperty() throws IOException {
        final String line = ""; // no bandname property

        final BufferedReader in = createReader(line);

        final Header header = new Header(in);
        final String[] bandNames = header.getBandNames();
        assertNull(bandNames);
    }

    public void testParseBandNames() throws IOException {
        final String line = "band names = {Karl, Fritzi, Petra}";

        final BufferedReader in = createReader(line);

        final Header header = new Header(in);
        final String[] bandNames = header.getBandNames();
        assertEquals(3, bandNames.length);
        assertEquals("Karl", bandNames[0]);
        assertEquals("Fritzi", bandNames[1]);
        assertEquals("Petra", bandNames[2]);
    }

    public void testParseBandNames_multipleLines() throws IOException {
        final String line = "band names = {Hugh, \nGrant, \nJulia, \n Roberts\n}";
        final BufferedReader in = createReader(line);

        final Header header = new Header(in);

        final String[] bandNames = header.getBandNames();
        assertEquals(4, bandNames.length);
        assertEquals("Hugh", bandNames[0]);
        assertEquals("Grant", bandNames[1]);
        assertEquals("Julia", bandNames[2]);
        assertEquals("Roberts", bandNames[3]);
    }

    public void testParseDescription_WithBeamProperties_SensingStartStop() throws IOException {
        final String beamProps = Header.BEAM_PROPERTIES + " = [" + Header.SENSING_START + " = 12324, " + Header.SENSING_STOP + " = 438976]";
        final String description = "any Description\t" + beamProps + "\tany other Description";
        final String line = "description = {" + description + "}";
        final BufferedReader in = createReader(line);

        final Header header = new Header(in);

        assertEquals(description, header.getDescription());
        final Header.BeamProperties beamProperties = header.getBeamProperties();
        assertNotNull(beamProperties);
        assertEquals("12324", beamProperties.getSensingStart());
        assertEquals("438976", beamProperties.getSensingStop());
    }

    public void testParseDescription_WithoutSensingStartStop() throws IOException {
        final String noStartStop = "[else other one = 12324; else other two = 438976]";
        final String description = "any Description " + noStartStop + " any other Description";
        final String line = "description = {" + description + "}";
        final BufferedReader in = createReader(line);

        final Header header = new Header(in);

        assertEquals(description, header.getDescription());
        assertEquals(null, header.getBeamProperties());
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private BufferedReader createReader(String line) {
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(line.getBytes())));
    }
}
