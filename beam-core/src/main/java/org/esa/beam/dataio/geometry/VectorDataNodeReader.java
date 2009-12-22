package org.esa.beam.dataio.geometry;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.util.io.CsvReader;
import org.esa.beam.util.io.FileUtils;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

public class VectorDataNodeReader {

    public VectorDataNode read(File file) throws IOException {
        FileReader reader = new FileReader(file);
        try {
            FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = readFeatures(reader);
            return new VectorDataNode(FileUtils.getFilenameWithoutExtension(file), featureCollection);
        } finally {
            reader.close();
        }
    }

    public FeatureCollection<SimpleFeatureType, SimpleFeature> readFeatures(Reader reader) throws IOException {
        CsvReader csvReader = new CsvReader(reader, new char[]{VectorDataNodeIO.DELIMITER_CHAR});
        SimpleFeatureType type = readFeatureType(csvReader);
        return readFeatures(csvReader, type);
    }


    private FeatureCollection<SimpleFeatureType, SimpleFeature> readFeatures(CsvReader csvReader, SimpleFeatureType simpleFeatureType) throws IOException {
        Converter<?>[] converters = VectorDataNodeIO.getConverters(simpleFeatureType);

        DefaultFeatureCollection fc = new DefaultFeatureCollection("?", simpleFeatureType);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeatureType);
        while (true) {
            String[] tokens = csvReader.readRecord();
            if (tokens == null) {
                break;
            }
            if (tokens.length != 1 + simpleFeatureType.getAttributeCount()) {
                throw new IOException("Shit.");  // todo - msg
            }
            builder.reset();
            String fid = null;
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                if (i == 0) {
                    fid = token;
                } else {
                    try {
                        Object value = null;
                        if (!VectorDataNodeIO.NULL_TEXT.equals(token)) {
                            value = converters[i - 1].parse(token);
                        }
                        builder.set(simpleFeatureType.getDescriptor(i - 1).getLocalName(), value);
                    } catch (ConversionException e) {
                        throw new IOException("Shit.", e);  // todo - msg
                    }
                }
            }
            SimpleFeature simpleFeature = builder.buildFeature(fid);
            fc.add(simpleFeature);
        }
        return fc;
    }

    private SimpleFeatureType readFeatureType(CsvReader csvReader) throws IOException {
        String[] tokens = csvReader.readRecord();
        if (tokens == null || tokens.length <= 1) {
            throw new IOException("Missing feature type definition in first line.");
        }
        return createFeatureType(tokens);
    }

    private SimpleFeatureType createFeatureType(String[] tokens) throws IOException {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        JavaTypeConverter jtc = new JavaTypeConverter();
        for (int i = 0; i < tokens.length; i++) {
            if (i == 0) {
                builder.setName(tokens[0]);
            } else {
                String token = tokens[i];
                final int colonPos = token.indexOf(':');
                if (colonPos == -1) {
                    throw new IOException(String.format("Missing type specifier in attribute descriptor '%s'", token));
                } else if (colonPos == 0) {
                    throw new IOException(String.format("Missing name specifier in attribute descriptor '%s'", token));
                }
                String attributeName = token.substring(0, colonPos);
                String attributeTypeName = token.substring(colonPos + 1);
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
        return builder.buildFeatureType();
    }

}
