/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.csv.dataio;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.sun.media.imageio.stream.FileChannelImageInputStream;
import org.esa.snap.core.dataio.geometry.VectorDataNodeIO;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.converters.JavaTypeConverter;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A CsvFile is a view on a csv file allowing a) to parse it using the {@link CsvSourceParser} interface
 * and b) to receive its values using the {@link CsvSource} interface.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvFile implements CsvSourceParser, CsvSource {

    public static final String DEFAULT_HEADER_NAME = "csv";

    private final Map<String, String> properties = new HashMap<>();
    private final File csv;
    private final SortedMap<Long, Long> bytePositionForOffset = new TreeMap<>();

    private SimpleFeatureType simpleFeatureType;
    private ListFeatureCollection featureCollection;
    private CoordinateReferenceSystem crs;

    private boolean propertiesParsed = false;
    private boolean hasFeatureId = false;
    private boolean recordsParsed = false;

    private ImageInputStream stream;

    private int headerByteSize;
    private int propertiesByteSize;
    private Converter<?>[] converters;

    private CsvFile(String csv) throws IOException {
        this(new File(csv), null);
    }

    private CsvFile(File csv, CoordinateReferenceSystem crs) throws IOException {
        this.csv = csv;
        ConverterRegistry.getInstance().setConverter(ProductData.UTC.class, new UTCConverter());
        this.crs = crs;
        RandomAccessFile randomAccessFile = new RandomAccessFile(csv, "r");
        stream = new FileChannelImageInputStream(randomAccessFile.getChannel());
    }

    public static CsvSourceParser createCsvSourceParser(String csv) throws IOException {
        return new CsvFile(csv);
    }

    @Override
    public void checkReadingFirstRecord() throws IOException {
        skipToLine(0);
        final String line = stream.readLine();
        final String[] tokens = getTokens(line);

        for (int i = 0; i < converters.length; i++) {
            Converter<?> converter = converters[i];
            try {
                converter.parse(tokens[i + (hasFeatureId ? 1 : 0)]);
            } catch (ConversionException e) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public Object[] parseRecords(final int offset, final int numRecords, final String colName) throws IOException {
        AttributeDescriptor attributeDescriptor = simpleFeatureType.getDescriptor(colName);
        int expectedTokenCount = simpleFeatureType.getAttributeCount();
        expectedTokenCount += hasFeatureId ? 1 : 0;
        int colIndex = simpleFeatureType.getAttributeDescriptors().indexOf(attributeDescriptor);
        int tokenIndex = colIndex + (hasFeatureId ? 1 : 0);

        List<Object> values = new ArrayList<>(numRecords);
        skipToLine(offset);
        String line;
        long featureCount = offset;
        while ((numRecords == -1 || featureCount < offset + numRecords) && (line = stream.readLine()) != null) {
            String[] tokens = getTokens(line);

            if (tokens.length != expectedTokenCount) {
                continue;
            }
            featureCount++;

            String token = tokens[tokenIndex];
            try {
                Object value = null;
                if (!VectorDataNodeIO.NULL_TEXT.equals(token)) {
                    value = converters[colIndex].parse(token);
                }
                values.add(value);
            } catch (ConversionException e) {
                SystemUtils.LOG.warning(String.format("Problem in '%s': %s",
                                                      csv.getPath(), e.getMessage()));
            }
            bytePositionForOffset.put(featureCount, stream.getStreamPosition());
        }
        return values.toArray();
    }

    @Override
    public void parseRecords(int offset, int numRecords) throws IOException {
        featureCollection = new ListFeatureCollection(simpleFeatureType);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeatureType);

        skipToLine(offset);
        String line;
        long featureCount = offset;
        while ((numRecords == -1 || featureCount < offset + numRecords) && (line = stream.readLine()) != null) {
            String[] tokens = getTokens(line);
            int expectedTokenCount = simpleFeatureType.getAttributeCount();
            expectedTokenCount += hasFeatureId ? 1 : 0;
            if (tokens.length != expectedTokenCount) {
                continue;
            }
            builder.reset();
            String featureId = "" + featureCount++;
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                if (i == 0 && hasFeatureId) {
                    featureId = token;
                } else {
                    try {
                        Object value = null;
                        int currentIndex = i;
                        currentIndex -= hasFeatureId ? 1 : 0;
                        if (!VectorDataNodeIO.NULL_TEXT.equals(token)) {
                            value = converters[currentIndex].parse(token);
                        }
                        builder.set(simpleFeatureType.getDescriptor(currentIndex).getLocalName(), value);
                    } catch (ConversionException e) {
                        SystemUtils.LOG.warning(String.format("Problem in '%s': %s",
                                                              csv.getPath(), e.getMessage()));
                    }
                }
            }
            SimpleFeature simpleFeature = builder.buildFeature(featureId);
            featureCollection.add(simpleFeature);
            bytePositionForOffset.put(featureCount, stream.getStreamPosition());
        }
        recordsParsed = true;
    }

    @Override
    public CsvSource parseMetadata() throws IOException {
        parseProperties();
        parseHeader();
        initConverters();
        return this;
    }

    private void initConverters() throws IOException {
        converters = VectorDataNodeIO.getConverters(simpleFeatureType);
        final String timePattern = properties.get(Constants.PROPERTY_NAME_TIME_PATTERN);
        if (StringUtils.isNotNullAndNotEmpty(timePattern)) {
            List<AttributeType> attributeTypes = getFeatureType().getTypes();
            for (int i = 0; i < attributeTypes.size(); i++) {
                Class<?> type = attributeTypes.get(i).getBinding();
                if (type == ProductData.UTC.class) {
                    converters[i] = new UTCConverter(timePattern);
                }
            }
        }
    }

    @Override
    public void close() {
        try {
            stream.close();
        } catch (IOException ignore) {
        }
    }

    @Override
    public int getRecordCount() throws IOException {
        int count = 1;
        byte[] buffer = new byte[100 * 1024];
        int readChars;
        long currentPosInStream = stream.getStreamPosition();
        stream.seek(0);
        while ((readChars = stream.read(buffer)) != -1) {
            for (int i = 0; i < readChars - 1; ++i) {
                if (buffer[i] == '\n') {
                    ++count;
                }
            }
        }
        count -= properties.size();
        final int headerLineCount = 1;
        count -= headerLineCount;
        stream.seek(currentPosInStream);
        return count;
    }

    @Override
    public SimpleFeature[] getSimpleFeatures() {
        if (!recordsParsed) {
            throw new IllegalStateException("The records have not been parsed yet.");
        }
        final Object[] objects = featureCollection.toArray(new Object[featureCollection.size()]);
        final SimpleFeature[] simpleFeatures = new SimpleFeature[objects.length];
        for (int i = 0; i < simpleFeatures.length; i++) {
            simpleFeatures[i] = (SimpleFeature) objects[i];

        }
        return simpleFeatures;
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        return simpleFeatureType;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    private void parseProperties() throws IOException {
        String line;
        stream.seek(0);
        propertiesByteSize = 0;
        long posInStream = 0;
        while ((line = stream.readLine()) != null) {
            if (!line.startsWith("#")) {
                stream.seek(posInStream);
                break;
            }
            propertiesByteSize += (stream.getStreamPosition() - posInStream);
            posInStream = stream.getStreamPosition();

            line = line.substring(1);
            int pos = line.indexOf('=');
            if (pos == -1) {
                throw new IOException("Missing '=' in '" + line + "'");
            }
            String name = line.substring(0, pos).trim();
            if (name.isEmpty()) {
                throw new IOException("Empty property name in '" + line + "'");
            }
            String value = line.substring(pos + 1).trim();
            try {
                if (contains(Constants.CRS_IDENTIFIERS, name) && crs != null) {
                    crs = CRS.parseWKT(value);
                }
            } catch (FactoryException e) {
                throw new IOException(e);
            }
            properties.put(name, value);
        }
        propertiesParsed = true;
    }

    private void skipToLine(long lineOffset) throws IOException {
        if (bytePositionForOffset.containsKey(lineOffset)) {
            stream.seek(bytePositionForOffset.get(lineOffset));
            return;
        }
        Map.Entry<Long, Long> entry = getBestOffset(lineOffset);
        stream.seek(entry.getValue());
        long linesToSkip = lineOffset - entry.getKey();
        for (int i = 0; i < linesToSkip; i++) {
            stream.readLine();
        }
        bytePositionForOffset.put(lineOffset, stream.getStreamPosition());
    }

    private Map.Entry<Long, Long> getBestOffset(long lineOffset) {
        Map.Entry<Long, Long> result = new AbstractMap.SimpleEntry<>(0L, (long) propertiesByteSize + headerByteSize);
        for (Map.Entry<Long, Long> entry : bytePositionForOffset.entrySet()) {
            if (entry.getKey() > lineOffset) {
                return result;
            } else {
                result = entry;
            }
        }
        return result;
    }

    private void createFeatureType(String[] headerLine) throws IOException {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setCRS(crs != null ? crs : DefaultGeographicCRS.WGS84);
        JavaTypeConverter jtc = new CsvJavaTypeConverter();
        builder.setName(DEFAULT_HEADER_NAME);
        for (String token : headerLine) {
            if (token.toLowerCase().equals("featureId".toLowerCase())) {
                hasFeatureId = true;
                continue;
            }
            String attributeTypeName;
            String attributeName;
            final int colonPos = token.indexOf(':');
            if (colonPos == 0) {
                throw new IOException(String.format("Missing name specifier in attribute descriptor '%s'", token));
            }
            Class<?> attributeType;
            if (colonPos == -1) {
                attributeName = token;
                attributeType = Double.class;
            } else {
                attributeName = token.substring(0, colonPos);
                attributeTypeName = token.substring(colonPos + 1).toLowerCase();
                if (attributeTypeName.equals("int")) {
                    attributeType = Integer.class;
                } else {
                    attributeTypeName = attributeTypeName.substring(0, 1).toUpperCase() + attributeTypeName.substring(1);
                    try {
                        attributeType = jtc.parse(attributeTypeName);
                    } catch (ConversionException e) {
                        throw new IOException(
                                String.format("Unknown type in attribute descriptor '%s'", token), e);
                    }
                }
            }
            builder.add(attributeName, attributeType);
        }
        simpleFeatureType = builder.buildFeatureType();
    }

    private void parseHeader() throws IOException {
        if (!propertiesParsed) {
            throw new IllegalStateException("Properties need to be parsed before header.");
        }
        stream.seek(propertiesByteSize);
        String line;
        long posInStream = stream.getStreamPosition();
        while ((line = stream.readLine()) != null) {
            if (line.startsWith("#")) {
                propertiesByteSize += (stream.getStreamPosition() - posInStream);
                posInStream = stream.getStreamPosition();
                continue;
            }
            headerByteSize += (stream.getStreamPosition() - posInStream);
            final String separator = getProperty(Constants.PROPERTY_NAME_SEPARATOR, Constants.DEFAULT_SEPARATOR);
            createFeatureType(line.split(separator));
            break;
        }
    }

    private String[] getTokens(String line) {
        int pos2;
        int pos1 = 0;
        final ArrayList<String> strings = new ArrayList<>();
        final String separator = getProperty(Constants.PROPERTY_NAME_SEPARATOR, Constants.DEFAULT_SEPARATOR);
        while ((pos2 = line.indexOf(separator, pos1)) >= 0) {
            strings.add(line.substring(pos1, pos2).trim());
            pos1 = pos2 + 1;
        }
        strings.add(line.substring(pos1).trim());
        return strings.toArray(new String[strings.size()]);
    }

    private boolean contains(String[] possibleStrings, String s) {
        for (String possibleString : possibleStrings) {
            if (possibleString.toLowerCase().equals(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String getProperty(String propertyName, String defaultValue) {
        return properties.get(propertyName) != null ? properties.get(propertyName) : defaultValue;
    }

    private static class UTCConverter implements Converter<ProductData.UTC> {

        private final String timePattern;

        public UTCConverter() {
            this(Constants.TIME_PATTERN);
        }

        private UTCConverter(String timePattern) {
            this.timePattern = timePattern;
        }

        @Override
        public Class<? extends ProductData.UTC> getValueType() {
            return ProductData.UTC.class;
        }

        @Override
        public ProductData.UTC parse(String text) throws ConversionException {
            try {
                return ProductData.UTC.parse(text, timePattern);
            } catch (java.text.ParseException e) {
                throw new ConversionException(e);
            }
        }

        @Override
        public String format(ProductData.UTC value) {
            final SimpleDateFormat sdf = new SimpleDateFormat(timePattern);
            return sdf.format(value.getAsDate());
        }
    }

    private class CsvJavaTypeConverter extends JavaTypeConverter {

        @Override
        public Class parse(String text) throws ConversionException {
            Class result;
            try {
                result = super.parse(text);
            } catch (ConversionException e) {
                try {
                    if (contains(Constants.TIME_NAMES, text.toLowerCase())) {
                        result = getClass().getClassLoader().loadClass(ProductData.UTC.class.getName());
                    } else if ("ubyte".toLowerCase().equals(text.toLowerCase())) {
                        result = getClass().getClassLoader().loadClass(Byte.class.getName());
                    } else if ("ushort".toLowerCase().equals(text.toLowerCase())) {
                        result = getClass().getClassLoader().loadClass(Short.class.getName());
                    } else if ("uint".toLowerCase().equals(text.toLowerCase())) {
                        result = getClass().getClassLoader().loadClass(Integer.class.getName());
                    } else {
                        throw new ConversionException(e);
                    }
                } catch (ClassNotFoundException e1) {
                    throw new ConversionException(e1);
                }
            }
            return result;
        }
    }
}
