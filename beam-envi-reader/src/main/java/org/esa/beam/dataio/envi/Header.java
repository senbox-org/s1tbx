package org.esa.beam.dataio.envi;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;

class Header {

    static final String UNKNOWN_SENSOR_TYPE = "Unknown Sensor Type";
    static final String SENSING_START = "sensingStart";
    static final String SENSING_STOP = "sensingStop";
    static final String BEAM_PROPERTIES = "beamProperties";

    Header(final BufferedReader reader) throws IOException {
        // @todo 2 tb/tb exception handling - for ANY parse operation

        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            line = line.trim();
            if (line.startsWith(EnviConstants.HEADER_KEY_SAMPLES)) {
                numSamples = Integer.parseInt(line.substring(line.indexOf('=') + 1).trim());
            } else if (line.startsWith(EnviConstants.HEADER_KEY_LINES)) {
                numLines = Integer.parseInt(line.substring(line.indexOf('=') + 1).trim());
            } else if (line.startsWith(EnviConstants.HEADER_KEY_BANDS)) {
                numBands = Integer.parseInt(line.substring(line.indexOf('=') + 1).trim());
            } else if (line.startsWith(EnviConstants.HEADER_KEY_HEADER_OFFSET)) {
                headerOffset = Integer.parseInt(line.substring(line.indexOf('=') + 1).trim());
            } else if (line.startsWith(EnviConstants.HEADER_KEY_FILE_TYPE)) {
                fileType = line.substring(line.indexOf('=') + 1).trim();
            } else if (line.startsWith(EnviConstants.HEADER_KEY_DATA_TYPE)) {
                dataType = Integer.parseInt(line.substring(line.indexOf('=') + 1).trim());
            } else if (line.startsWith(EnviConstants.HEADER_KEY_INTERLEAVE)) {
                interleave = line.substring(line.indexOf('=') + 1).trim();
            } else if (line.startsWith(EnviConstants.HEADER_KEY_SENSOR_TYPE)) {
                sensorType = line.substring(line.indexOf('=') + 1).trim();
            } else if (line.startsWith(EnviConstants.HEADER_KEY_WAVELENGTH_UNITS)) {
                wavelengthsUnits = line.substring(line.indexOf('=') + 1).trim();
            } else if (line.startsWith(EnviConstants.HEADER_KEY_BYTE_ORDER)) {
                byteOrder = Integer.parseInt(line.substring(line.indexOf('=') + 1).trim());
            } else if (line.startsWith(EnviConstants.HEADER_KEY_MAP_INFO)) {
                line = assembleMultilineString(reader, line);
                parseMapInfo(line);
            } else if (line.startsWith(EnviConstants.HEADER_KEY_PROJECTION_INFO)) {
                line = assembleMultilineString(reader, line);
                parseProjectionInfo(line);
            } else if (line.startsWith(EnviConstants.HEADER_KEY_BAND_NAMES)) {
                line = assembleMultilineString(reader, line);
                bandNames = parseCommaSeparated(line);
            } else if (line.startsWith(EnviConstants.HEADER_KEY_WAVELENGTH)) {
                line = assembleMultilineString(reader, line);
                wavelengths = parseCommaSeparated(line);
            } else if (line.startsWith(EnviConstants.HEADER_KEY_DESCRIPTION)) {
                line = assembleMultilineString(reader, line);
                description = line.substring(line.indexOf('{') + 1, line.lastIndexOf('}')).trim();
                parseBeamProperties(description);
            } else if (line.startsWith(EnviConstants.HEADER_KEY_DATA_OFFSET_VALUES)) {
                dataOffsetValues = getDoubleValues(reader, line);
            } else if (line.startsWith(EnviConstants.HEADER_KEY_DATA_GAIN_VALUES)) {
                dataGainValues = getDoubleValues(reader, line);
            }
        }
        // @todo 2 se/** after reading the headerFile validate the HeaderConstraints
    }

    private double[] getDoubleValues(BufferedReader reader, String line) throws IOException {
        line = assembleMultilineString(reader, line);
        final String[] valueStrings = parseCommaSeparated(line);
        double[] values = new double[valueStrings.length];
        for (int i = 0; i < valueStrings.length; i++) {
            values[i] = Double.valueOf(valueStrings[i]);
        }
        return values;
    }

    public ByteOrder getJavaByteOrder() {
        if (getByteOrder() == 1) {
            return ByteOrder.BIG_ENDIAN;
        } else {
            return ByteOrder.LITTLE_ENDIAN;
        }
    }

    public String getFileType() {
        return fileType;
    }

    public int getNumSamples() {
        return numSamples;
    }

    public int getNumLines() {
        return numLines;
    }

    public int getNumBands() {
        return numBands;
    }

    public int getHeaderOffset() {
        return headerOffset;
    }

    public int getDataType() {
        return dataType;
    }

    public String getInterleave() {
        return interleave;
    }

    public String getSensorType() {
        if (sensorType == null) {
            return UNKNOWN_SENSOR_TYPE;
        }
        return sensorType;
    }

    public int getByteOrder() {
        return byteOrder;
    }

    public EnviMapInfo getMapInfo() {
        return mapInfo;
    }

    public EnviProjectionInfo getProjectionInfo() {
        return projectionInfo;
    }

    public String[] getBandNames() {
        return bandNames;
    }

    public String getDescription() {
        return description;
    }

    public BeamProperties getBeamProperties() {
        return beamProperties;
    }

    public String[] getWavelengths() {
        return wavelengths;
    }

    public String getWavelengthsUnit() {
        return wavelengthsUnits;
    }

    public double[] getDataOffsetValues() {
        return dataOffsetValues;
    }

    public double[] getDataGainValues() {
        return dataGainValues;
    }



    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private int numSamples;
    private int numLines;
    private int numBands;
    private String fileType;
    private int headerOffset;
    private int dataType;
    private String interleave;
    private String sensorType;
    private int byteOrder;
    private EnviMapInfo mapInfo;
    private EnviProjectionInfo projectionInfo;
    private String[] bandNames;
    private String[] wavelengths;
    private String wavelengthsUnits;
    private String description;
    private double[] dataOffsetValues;
    private double[] dataGainValues;


    private BeamProperties beamProperties;

    private void parseMapInfo(String line) {
        try {
            mapInfo = new EnviMapInfo();
            final StringTokenizer tokenizer = createTokenizerFromLine(line);
            mapInfo.setProjectionName(tokenizer.nextToken().trim());
            mapInfo.setReferencePixelX(Double.parseDouble(tokenizer.nextToken()));
            mapInfo.setReferencePixelY(Double.parseDouble(tokenizer.nextToken()));
            mapInfo.setEasting(Double.parseDouble(tokenizer.nextToken()));
            mapInfo.setNorthing(Double.parseDouble(tokenizer.nextToken()));
            mapInfo.setPixelSizeX(Double.parseDouble(tokenizer.nextToken()));
            mapInfo.setPixelSizeY(Double.parseDouble(tokenizer.nextToken()));
            if (mapInfo.getProjectionName().equalsIgnoreCase("UTM")) {
                mapInfo.setUtmZone(Integer.parseInt(tokenizer.nextToken().trim()));
                mapInfo.setUtmHemisphere(tokenizer.nextToken().trim());
            }
            mapInfo.setDatum(tokenizer.nextToken().trim());
            while(tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken().trim();
                if (token.startsWith("units=")) {
                    mapInfo.setUnit(token.substring("units=".length()));
                } else if (token.startsWith("rotation=")) {
                    String rotation = token.substring("rotation=".length());
                    mapInfo.setOrientation(Double.parseDouble(rotation));
                }
            }
        } catch (NoSuchElementException e) {
            // handle shorter string gracefully
        }
    }

    private static StringTokenizer createTokenizerFromLine(String line) {
        final int start = line.indexOf('{') + 1;
        final int stop = line.lastIndexOf('}');
        return new StringTokenizer(line.substring(start, stop), ",");
    }

    private void parseProjectionInfo(String line) {
        projectionInfo = new EnviProjectionInfo();
        final StringTokenizer tokenizer = createTokenizerFromLine(line);
        projectionInfo.setProjectionNumber(Integer.parseInt(tokenizer.nextToken().trim()));

        final ArrayList<Double> parameterList = new ArrayList<Double>(20);
        String token = null;
        try {
            while (tokenizer.hasMoreTokens()) {
                token = tokenizer.nextToken().trim();
                parameterList.add(Double.parseDouble(token));
            }
        } catch (NumberFormatException e) {
            // ugly - but works. we encountered the first non-double token.
        }
        final double[] parameters = new double[parameterList.size()];
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = parameterList.get(i);
        }
        projectionInfo.setParameter(parameters);

        projectionInfo.setDatum(token);
        projectionInfo.setName(tokenizer.nextToken().trim());
    }

    private static boolean withoutEndTag(String line) {
        return line.indexOf('}') < 0;
    }

    private static String assembleMultilineString(BufferedReader reader, String line) throws IOException {
        StringBuilder buffer = new StringBuilder(10);
        buffer.append(line);
        while (withoutEndTag(line)) {
            line = reader.readLine();
            buffer.append(line);
        }
        final String bufferString = buffer.toString();
        line = bufferString.replace('\n', ' ');
        return line;
    }

    private String[] parseCommaSeparated(String line) {
        final StringTokenizer tokenizer = createTokenizerFromLine(line);
        String[] elems = new String[tokenizer.countTokens()];
        int index = 0;
        while (tokenizer.hasMoreTokens()) {
            elems[index] = tokenizer.nextToken().trim();
            ++index;
        }
        return elems;
    }

    private void parseBeamProperties(final String txt) throws IOException {
        if (txt.contains(BEAM_PROPERTIES)) {
            final int propsIdx = txt.indexOf(BEAM_PROPERTIES);
            final int openIdx = txt.indexOf('[', propsIdx);
            final int closeIdx = txt.indexOf(']', openIdx);
            final String beamProps = txt.substring(openIdx + 1, closeIdx);
            final String strings = beamProps.replace(',', '\n');
            final ByteArrayInputStream in = new ByteArrayInputStream(strings.getBytes());
            final Properties properties = loadProperties(in);
            final BeamProperties bean = new BeamProperties();
            if (properties.containsKey(Header.SENSING_START)) {
                bean.setSensingStart(properties.getProperty(Header.SENSING_START));
            }
            if (properties.containsKey(Header.SENSING_STOP)) {
                bean.setSensingStop(properties.getProperty(Header.SENSING_STOP));
            }
            this.beamProperties = bean;
        }
    }

    public static Properties loadProperties(final InputStream in) throws IOException {
        final Properties properties = new Properties();
        try {
            properties.load(in);
        } finally {
            in.close();
        }
        return properties;
    }

    public static class BeamProperties {

        private String sensingStart;
        private String sensingStop;

        public String getSensingStart() {
            return sensingStart;
        }

        public String getSensingStop() {
            return sensingStop;
        }

        public void setSensingStart(String sensingStart) {
            this.sensingStart = sensingStart;
        }

        public void setSensingStop(String sensingStop) {
            this.sensingStop = sensingStop;
        }
    }
}

