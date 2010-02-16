package org.esa.beam.dataio.geometry;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import org.opengis.feature.simple.SimpleFeatureType;
import org.esa.beam.util.converters.JtsGeometryConverter;

import java.io.IOException;

public class VectorDataNodeIO {
    public static final char DELIMITER_CHAR = '\t';
    public static final String ESCAPE_STRING = "\\t";
    public static final String NULL_TEXT = "[null]";
    public static final String FILENAME_EXTENSION = ".csv";
    static final String PROPERTY_NAME_DEFAULT_CSS = "defaultCSS";

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
    
    public static String encodeTabString(String input) {
        StringBuilder sb = new StringBuilder(input.length() + 10);
        boolean escapeMode = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == DELIMITER_CHAR) {
                sb.append(ESCAPE_STRING);
                escapeMode = false;
            } else { 
                if (c == '\\') {
                    escapeMode = true;
                } else {
                    if (c == 't' && escapeMode) {
                        sb.append('\\');
                    }
                    escapeMode = false;
                }
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    public static String decodeTabString(String input) {
        StringBuilder sb = new StringBuilder(input.length() + 10);
        int numEscapes = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\') {
                numEscapes++;
                sb.append(c);
            } else {
                if (c == 't' && numEscapes == 1) {
                    sb.deleteCharAt(sb.length()-1);
                    sb.append(DELIMITER_CHAR);
                    numEscapes = 0;
                } else if (c == 't' && numEscapes > 1) {
                    sb.deleteCharAt(sb.length()-1);
                    sb.append(c);
                    numEscapes = 0;
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}
