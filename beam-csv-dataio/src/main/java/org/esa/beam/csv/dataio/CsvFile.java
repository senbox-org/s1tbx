/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.csv.dataio;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import org.esa.beam.dataio.geometry.VectorDataNodeIO;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.converters.JavaTypeConverter;
import org.esa.beam.util.io.Constants;
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A CsvFile is a view on a csv file allowing a) to parse it using the {@link CsvSourceParser} interface
 * and b) to receive its values using the {@link CsvSource} interface.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvFile implements CsvSourceParser, CsvSource {

    public static final String DEFAULT_HEADER_NAME = "csv";

    private final Map<String, String> properties = new HashMap<String, String>();
    private final File csv;
    private final Map<Long, Long> positionForOffset = new HashMap<Long, Long>();

    private SimpleFeatureType simpleFeatureType;
    private FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
    private CoordinateReferenceSystem crs;

    private boolean propertiesParsed = false;
    private boolean hasFeatureId = false;
    private boolean recordsParsed = false;

    private ImageInputStream stream;
    
    private int headerByteSize;
    private int propertiesByteSize;

    private CsvFile(String csv) throws IOException {
        this(new File(csv), null);
    }

    private CsvFile(File csv, CoordinateReferenceSystem crs) throws IOException {
        this.csv = csv;
        ConverterRegistry.getInstance().setConverter(ProductData.UTC.class, new UTCConverter());
        this.crs = crs;
        stream = new FileImageInputStream(csv);
    }

    public static CsvSourceParser createCsvSourceParser(String csv) throws IOException {
        return new CsvFile(csv);
    }

    @Override
    public void parseRecords(int offset, int numRecords) throws IOException {
        Converter<?>[] converters = VectorDataNodeIO.getConverters(simpleFeatureType);

        featureCollection = new ListFeatureCollection(simpleFeatureType);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeatureType);

        skipToOffset(offset);
        String line;
        long featureCount = offset;
        while ((numRecords == -1 || featureCount < offset + numRecords) && (line = stream.readLine()) != null) {
            String[] tokens = getTokens(line);
            if (tokens == null) {
                break;
            }
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
                        BeamLogManager.getSystemLogger().warning(String.format("Problem in '%s': %s",
                                                                               csv.getPath(), e.getMessage()));
                    }
                }
            }
            SimpleFeature simpleFeature = builder.buildFeature(featureId);
            featureCollection.add(simpleFeature);
            positionForOffset.put(featureCount, stream.getStreamPosition());
        }
        recordsParsed = true;
    }

    @Override
    public CsvSource parseMetadata() throws IOException {
        parseProperties();
        parseHeader();
        return this;
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
        long posInStream = 0l;
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

    private void skipToOffset(long recordOffset) throws IOException {
        if (positionForOffset.containsKey(recordOffset)) {
            stream.seek(positionForOffset.get(recordOffset));
            return;
        }
        stream.seek(propertiesByteSize + headerByteSize);
        for (int i = 0; i < recordOffset; i++) {
            stream.readLine();
        }
        positionForOffset.put(recordOffset, stream.getStreamPosition());
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
            if (colonPos == -1) {
                attributeName = token;
                attributeTypeName = "double";
            } else if (colonPos == 0) {
                throw new IOException(String.format("Missing name specifier in attribute descriptor '%s'", token));
            } else {
                attributeName = token.substring(0, colonPos);
                attributeTypeName = token.substring(colonPos + 1).toLowerCase();
            }
            attributeTypeName = attributeTypeName.substring(0, 1).toUpperCase() + attributeTypeName.substring(1);
            Class<?> attributeType;
            try {
                attributeType = jtc.parse(attributeTypeName);
            } catch (ConversionException e) {
                throw new IOException(
                        String.format("Unknown type in attribute descriptor '%s'", token), e);
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
            final String separator =
                    properties.get("separator") != null ? properties.get("separator") : Constants.DEFAULT_SEPARATOR;
            createFeatureType(line.split(separator));
            break;
        }
    }

    private String[] getTokens(String line) {
        int pos2;
        int pos1 = 0;
        final ArrayList<String> strings = new ArrayList<String>();
        final String separator =
                properties.get("separator") != null ? properties.get("separator") : Constants.DEFAULT_SEPARATOR;
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

    private static class UTCConverter implements Converter<ProductData.UTC> {

        @Override
        public Class<? extends ProductData.UTC> getValueType() {
            return ProductData.UTC.class;
        }

        @Override
        public ProductData.UTC parse(String text) throws ConversionException {
            try {
                return ProductData.UTC.parse(text, Constants.TIME_PATTERN);
            } catch (java.text.ParseException e) {
                throw new ConversionException(e);
            }
        }

        @Override
        public String format(ProductData.UTC value) {
            final SimpleDateFormat sdf = new SimpleDateFormat(Constants.TIME_PATTERN);
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
