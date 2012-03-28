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

package org.esa.beam.util.io;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import org.esa.beam.dataio.geometry.VectorDataNodeIO;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.converters.JavaTypeConverter;
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
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

    public static final String DEFAULT_HEADER_NAME = "Csv";
    private final Map<String, String> properties = new HashMap<String, String>();
    private FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
    private final File csv;

    private boolean propertiesParsed = false;
    private SimpleFeatureType simpleFeatureType;
    private CoordinateReferenceSystem crs;
    private boolean hasFeatureId = false;
    private LineCountReader reader;
    private boolean recordsParsed = false;

    private CsvFile(String csv) {
        this(new File(csv));
    }

    private CsvFile(File csv) {
        this(csv, null);
    }

    private CsvFile(File csv, CoordinateReferenceSystem crs) {
        this.csv = csv;
        ConverterRegistry.getInstance().setConverter(ProductData.UTC.class, new UTCConverter());
        reader = null;
        this.crs = crs;
    }

    public static CsvSourceParser createCsvSourceParser(String csv) {
        return new CsvFile(csv);
    }

    public static CsvSourceParser createCsvSourceParser(File csv) {
        return new CsvFile(csv);
    }

    public static CsvSourceParser createCsvSourceParser(File csv, CoordinateReferenceSystem crs) {
        return new CsvFile(csv, crs);
    }
    
    @Override
    public void parseRecords(int offset, int numRecords) throws ParseException {
        Converter<?>[] converters;
        try {
            converters = VectorDataNodeIO.getConverters(simpleFeatureType);
        } catch (IOException e) {
            throw new ParseException(e);
        }

        featureCollection = new ListFeatureCollection(simpleFeatureType);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeatureType);

        try {
            if (reader == null) {
                reader = new LineCountReader(new FileReader(csv));
                skipNonRecordLines(reader);
            } else if (reader.getRecordLine() > offset) {
                reader.close();
                reader = new LineCountReader(new FileReader(csv));
                skipNonRecordLines(reader);
            }
            skipToOffset(offset, reader);
            String line;
            int featureCount = offset;
            while ((numRecords == -1 || featureCount < offset + numRecords) && (line = reader.readRecordLine()) != null) {
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
            }
        } catch (Exception e) {
            throw new ParseException(e);
        }
        recordsParsed = true;
    }

    @Override
    public CsvSource parseMetadata() throws ParseException {
        parseProperties();
        parseHeader();
        return this;
    }

    @Override
    public void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignore) {
            }
        }
    }

    @Override
    public int getRecordCount() throws IOException {
        int count = 1;
        InputStream stream = null;
        try {
            stream = new BufferedInputStream(new FileInputStream(csv));
            byte[] buffer = new byte[100 * 1024];
            int readChars;
            while ((readChars = stream.read(buffer)) != -1) {
                for (int i = 0; i < readChars - 1; ++i) {
                    if (buffer[i] == '\n') {
                        ++count;
                    }
                }
            }
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
        count -= properties.size();
        final int headerLineCount = 1;
        count -= headerLineCount;
        return count;
    }

    @Override
    public SimpleFeature[] getSimpleFeatures() {
        if(!recordsParsed) {
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

    private void parseProperties() throws ParseException {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(csv));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.startsWith("#")) {
                    break;
                }
                line = line.substring(1);
                int pos = line.indexOf('=');
                if (pos == -1) {
                    throw new ParseException("Missing '=' in '" + line + "'");
                }
                String name = line.substring(0, pos).trim();
                if (name.isEmpty()) {
                    throw new ParseException("Empty property name in '" + line + "'");
                }
                String value = line.substring(pos + 1).trim();
                if (contains(Constants.CRS_IDENTIFIERS, name) && crs != null) {
                    crs = CRS.parseWKT(value);
                }
                properties.put(name, value);
            }
        } catch (IOException e) {
            throw new ParseException(e);
        } catch (FactoryException e) {
            throw new ParseException(e);
        } finally {
            propertiesParsed = true;
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void skipToOffset(int sourceOffsetX, LineCountReader reader) throws IOException {
        int count = reader.getRecordLine();
        while (count < sourceOffsetX && reader.readRecordLine() != null) {
            count++;
        }
    }

    private void skipNonRecordLines(LineCountReader reader) throws IOException {
        String line;
        while ((line = reader.readNonRecordLine()) != null) {
            if (!line.startsWith("#")) {
                break;
            }
        }
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

    private void parseHeader() throws ParseException {
        if (!propertiesParsed) {
            throw new IllegalStateException("Properties need to be parsed before header.");
        }

        BufferedReader bufferedReader = null;
        try {
            String line;
            bufferedReader = new BufferedReader(new FileReader(csv));
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                final String separator = properties.get("separator") != null ? properties.get("separator") : Constants.DEFAULT_SEPARATOR;
                createFeatureType(line.split(separator));
                break;
            }
        } catch (IOException e) {
            throw new ParseException(e);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ignore) {
                }
            }
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

    private class LineCountReader extends BufferedReader {
        private int recordLine = 0;

        private LineCountReader(Reader in) {
            super(in);
        }

        @Override
        public String readLine() throws IOException {
            throw new IllegalStateException("Use 'readNonRecordLine' or 'readRecordLine'");
        }

        public String readNonRecordLine() throws IOException {
            return super.readLine();
        }

        public String readRecordLine() throws IOException {
            recordLine++;
            return super.readLine();
        }

        public int getRecordLine() {
            return recordLine;
        }
    }

}
