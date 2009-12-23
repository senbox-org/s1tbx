package org.esa.beam.dataio.geometry;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import org.opengis.feature.simple.SimpleFeatureType;
import org.esa.beam.util.converters.JtsGeometryConverter;

import java.io.IOException;

public class VectorDataNodeIO {
    public static final char DELIMITER_CHAR = '\t';
    public static final String NULL_TEXT = "[null]";
    public static final String FILE_EXTENSION = ".csv";

    static {
        JtsGeometryConverter.registerConverter();
    }

    public static Converter[] getConverters(SimpleFeatureType simpleFeatureType) throws IOException {
        Converter[] converters = new Converter[simpleFeatureType.getAttributeCount()];
        for (int i = 0; i < converters.length; i++) {
            Class<?> attributeType = simpleFeatureType.getType(i).getBinding();
            Converter converter = ConverterRegistry.getInstance().getConverter(attributeType);
            if (converter == null) {
                throw new IOException(String.format("No converter for type %s found.", attributeType));
            }
            converters[i] = converter;
        }
        return converters;
    }
}
