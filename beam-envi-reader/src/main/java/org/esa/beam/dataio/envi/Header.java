package org.esa.beam.dataio.envi;

import org.esa.beam.framework.datamodel.MetadataElement;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.*;

public class Header {

    static final String UNKNOWN_SENSOR_TYPE = "Unknown Sensor Type";
    static final String SENSING_START = "sensingStart";
    static final String SENSING_STOP = "sensingStop";
    static final String BEAM_PROPERTIES = "beamProperties";
    private final HeaderParser headerParser;

    public Header(final BufferedReader reader) throws IOException {
        headerParser = HeaderParser.parse(reader);
    }

    public ByteOrder getJavaByteOrder() {
        if (getByteOrder() == 1) {
            return ByteOrder.BIG_ENDIAN;
        } else {
            return ByteOrder.LITTLE_ENDIAN;
        }
    }

    public String getFileType() {
        return headerParser.getString(EnviConstants.HEADER_KEY_FILE_TYPE, null);
    }

    public int getNumSamples() {
        return headerParser.getInt(EnviConstants.HEADER_KEY_SAMPLES);
    }

    public int getNumLines() {
        return headerParser.getInt(EnviConstants.HEADER_KEY_LINES);
    }

    public int getNumBands() {
        return headerParser.getInt(EnviConstants.HEADER_KEY_BANDS, 0);
    }

    public int getHeaderOffset() {
        return headerParser.getInt(EnviConstants.HEADER_KEY_HEADER_OFFSET, 0);
    }

    public int getDataType() {
        return headerParser.getInt(EnviConstants.HEADER_KEY_DATA_TYPE);
    }

    public String getInterleave() {
        return headerParser.getString(EnviConstants.HEADER_KEY_INTERLEAVE);
    }

    public String getSensorType() {
        return headerParser.getString(EnviConstants.HEADER_KEY_SENSOR_TYPE, UNKNOWN_SENSOR_TYPE);
    }

    public int getByteOrder() {
        return headerParser.getInt(EnviConstants.HEADER_KEY_BYTE_ORDER, 0);
    }

    public EnviMapInfo getMapInfo() {
        String mapInfoString = headerParser.getString(EnviConstants.HEADER_KEY_MAP_INFO, null);
        if (mapInfoString != null) {
            return parseMapInfo(mapInfoString);
        }
        return null;
    }

    public EnviProjectionInfo getProjectionInfo() {
        String projectionInfoString = headerParser.getString(EnviConstants.HEADER_KEY_PROJECTION_INFO, null);
        if (projectionInfoString != null) {
            return parseProjectionInfo(projectionInfoString);
        }
        return null;
    }

    public String[] getBandNames() {
        return headerParser.getStrings(EnviConstants.HEADER_KEY_BAND_NAMES);
    }

    public String getDescription() {
        return headerParser.getString(EnviConstants.HEADER_KEY_DESCRIPTION, null);
    }

    public BeamProperties getBeamProperties() {
        return parseBeamProperties(getDescription());
    }

    public String[] getWavelengths() {
        return headerParser.getStrings(EnviConstants.HEADER_KEY_WAVELENGTH);
    }

    public String[] getFWHM() {
        return headerParser.getStrings(EnviConstants.HEADER_KEY_FWHM);
    }

    public String getWavelengthsUnit() {
        return headerParser.getString(EnviConstants.HEADER_KEY_WAVELENGTH_UNITS, null);
    }

    public double[] getDataOffsetValues() {
        return headerParser.getDoubles(EnviConstants.HEADER_KEY_DATA_OFFSET_VALUES);
    }

    public double[] getDataGainValues() {
        return headerParser.getDoubles(EnviConstants.HEADER_KEY_DATA_GAIN_VALUES);
    }

    public Double getDataIgnoreValue() {
        if (headerParser.contains(EnviConstants.HEADER_KEY_DATA_IGNORE_VALUE)) {
            return headerParser.getDouble(EnviConstants.HEADER_KEY_DATA_IGNORE_VALUE);
        }
        return null;
    }

    public int getNumClasses() {
        return headerParser.getInt(EnviConstants.HEADER_KEY_CLASSES, 0);
    }

    public String[] getClassNmaes() {
        return headerParser.getStrings(EnviConstants.HEADER_KEY_CLASS_NAMES);
    }

    public int[] getClassColorRGB() {
        return headerParser.getInts(EnviConstants.HEADER_KEY_CLASS_LOOKUP);
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private EnviMapInfo parseMapInfo(String line) {
        try {
            EnviMapInfo mapInfo = new EnviMapInfo();
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
            return mapInfo;
        } catch (NoSuchElementException e) {
            // handle shorter string gracefully
        }
        return null;
    }

    private static StringTokenizer createTokenizerFromLine(String line) {
        return new StringTokenizer(line, ",");
    }

    private EnviProjectionInfo parseProjectionInfo(String line) {
        EnviProjectionInfo projectionInfo = new EnviProjectionInfo();
        final StringTokenizer tokenizer = createTokenizerFromLine(line);
        projectionInfo.setProjectionNumber(Integer.parseInt(tokenizer.nextToken().trim()));

        final ArrayList<Double> parameterList = new ArrayList<>(20);
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
        return projectionInfo;
    }

    private BeamProperties parseBeamProperties(final String txt)  {
        if (txt != null && txt.contains(BEAM_PROPERTIES)) {
            final int propsIdx = txt.indexOf(BEAM_PROPERTIES);
            final int openIdx = txt.indexOf('[', propsIdx);
            final int closeIdx = txt.indexOf(']', openIdx);
            final String beamProps = txt.substring(openIdx + 1, closeIdx);
            final String strings = beamProps.replace(',', '\n');
            final Properties properties = loadProperties(strings);
            final BeamProperties bean = new BeamProperties();
            if (properties.containsKey(Header.SENSING_START)) {
                bean.setSensingStart(properties.getProperty(Header.SENSING_START));
            }
            if (properties.containsKey(Header.SENSING_STOP)) {
                bean.setSensingStop(properties.getProperty(Header.SENSING_STOP));
            }
            return bean;
        }
        return null;
    }

    public static Properties loadProperties(String text) {
        final Properties properties = new Properties();
        try (ByteArrayInputStream in = new ByteArrayInputStream(text.getBytes())) {
            properties.load(in);
        } catch (IOException ignore) {
        }
        return properties;
    }

    public MetadataElement getAsMetadata() {
        MetadataElement headerElem = new MetadataElement("Header");
        for (Map.Entry<String, String> entry : headerParser.getHeaderEntries()) {
            headerElem.setAttributeString(entry.getKey(), entry.getValue());
        }
        Set<Map.Entry<String, String>> historyEntries = headerParser.getHistoryEntries();
        if (!historyEntries.isEmpty()) {
            MetadataElement historyElem = new MetadataElement("History");
            for (Map.Entry<String, String> entry : historyEntries) {
                historyElem.setAttributeString(entry.getKey(), entry.getValue());
            }
            headerElem.addElement(historyElem);
        }
        return headerElem;
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

