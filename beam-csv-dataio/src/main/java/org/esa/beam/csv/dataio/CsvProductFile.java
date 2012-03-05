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
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.feature.DefaultFeatureCollection;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A CsvProductFile is a view on a csv file allowing a) to parse it using the {@link CsvProductSourceParser} interface
 * and b) to receive its values using the {@link CsvProductSource} interface.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvProductFile implements CsvProductSourceParser, CsvProductSource {

    private final Map<String,String> properties = new HashMap<String,String>();
    private FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
    private final File csv;

    private boolean propertiesParsed = false;
    private SimpleFeatureType simpleFeatureType;
    private CoordinateReferenceSystem crs;

    public CsvProductFile(String csv) {
        this(new File(csv));
    }

    public CsvProductFile(File csv) {
        this.csv = csv;
        ConverterRegistry.getInstance().setConverter(ProductData.UTC.class, new UTCConverter());
    }

    private void parseProperties() throws ParseException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(csv));
            String line;
            while((line = reader.readLine()) != null) {
                if(!line.startsWith("#")) {
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
                if(contains(Constants.CRS_IDENTIFIERS, name)) {
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
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public void parseRecords() throws ParseException {
        Converter<?>[] converters;
        try {
            converters = VectorDataNodeIO.getConverters(simpleFeatureType);
        } catch (IOException e) {
            throw new ParseException(e);
        }

        featureCollection = new DefaultFeatureCollection("?", simpleFeatureType);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeatureType);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(csv));
            skipNonRecordLines(reader);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = getTokens(line);
                if (tokens == null) {
                    break;
                }
                final int expectedTokenCount = 1 + simpleFeatureType.getAttributeCount();
                if (tokens.length != expectedTokenCount) {
                    continue;
                }
                builder.reset();
                String featureId = null;
                for (int i = 0; i < tokens.length; i++) {
                    String token = tokens[i];
                    if (i == 0) {
                        featureId = token;
                    } else {
                        try {
                            Object value = null;
                            if (!VectorDataNodeIO.NULL_TEXT.equals(token)) {
                                value = converters[i - 1].parse(token);
                            }
                            builder.set(simpleFeatureType.getDescriptor(i - 1).getLocalName(), value);
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
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void skipNonRecordLines(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("#")) {
                break;
            }
        }
    }

    private void createFeatureType(String[] headerLine) throws IOException {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setCRS(crs != null ? crs : DefaultGeographicCRS.WGS84);
        JavaTypeConverter jtc = new CsvJavaTypeConverter();
        for (int i = 0; i < headerLine.length; i++) {
            if (i == 0) {
                builder.setName(headerLine[0]);
            } else {
                String token = headerLine[i];
                final int colonPos = token.indexOf(':');
                if (colonPos == -1) {
                    throw new IOException(String.format("Missing type specifier in attribute descriptor '%s'", token));
                } else if (colonPos == 0) {
                    throw new IOException(String.format("Missing name specifier in attribute descriptor '%s'", token));
                }
                String attributeName = token.substring(0, colonPos);
                String attributeTypeName = token.substring(colonPos + 1).toLowerCase();
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
        }
        simpleFeatureType = builder.buildFeatureType();
    }

    private void parseHeader() throws ParseException {
        if(!propertiesParsed) {
            throw new IllegalStateException("Properties need to be parsed before header.");
        }

        BufferedReader reader = null;
        try {
            String line;
            reader = new BufferedReader(new FileReader(csv));
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                final String separator = properties.get("separator") != null ? properties.get("separator") : Constants.DEFAULT_SEPARATOR ;
                createFeatureType(line.split(separator));
                break;
            }
        } catch (IOException e) {
            throw new ParseException(e);
        } finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    @Override
    public CsvProductSource parse() throws ParseException {
        parseProperties();
        parseHeader();
        return this;
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
                for (int i = 0; i < readChars; ++i) {
                    if (buffer[i] == '\n')
                        ++count;
                }
            }
        } finally {
            if(stream != null) {
                stream.close();
            }
        }
        count -= properties.size();
        final int headerLineCount = 1;
        count -= headerLineCount;
        return count;
    }

    @Override
    public FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatureCollection() {
        return featureCollection;
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        return simpleFeatureType;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    private String[] getTokens(String line) {
        int pos2;
        int pos1 = 0;
        final ArrayList<String> strings = new ArrayList<String>();
        final String separator = properties.get("separator") != null ? properties.get("separator") : Constants.DEFAULT_SEPARATOR ;
        while ((pos2 = line.indexOf(separator, pos1)) >= 0) {
            strings.add(line.substring(pos1, pos2).trim());
            pos1 = pos2 + 1;
        }
        strings.add(line.substring(pos1).trim());
        return strings.toArray(new String[strings.size()]);
    }

    private boolean contains(String[] possibleStrings, String s) {
        for (String possibleString : possibleStrings) {
            if(possibleString.toLowerCase().equals(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static class ParseException extends Exception {

        ParseException(String message) {
            super(message);
        }

        private ParseException(Throwable cause) {
            super(cause);
        }
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
            try{
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
