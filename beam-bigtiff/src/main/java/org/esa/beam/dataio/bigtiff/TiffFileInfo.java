package org.esa.beam.dataio.bigtiff;


import it.geosolutions.imageio.plugins.tiff.GeoTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFIFD;
import org.esa.beam.dataio.bigtiff.internal.GeoKeyEntry;

import java.util.*;

public class TiffFileInfo {

    private static final int TAG_GEO_KEY_DIRECTORY___SPOT = 34735;
    private static final int TAG_GEO_DOUBLE_PARAMS___SPOT = 34736;
    private static final int TAG_GEO_ASCII_PARAMS___SPOT = 34737;

    private final Map<Integer, TIFFField> fieldMap;

    TiffFileInfo(TIFFIFD rootIFD) {
        final TIFFField[] tiffFields = rootIFD.getTIFFFields();
        fieldMap = new HashMap<>(tiffFields.length);
        for (final TIFFField field : tiffFields) {
            fieldMap.put(field.getTagNumber(), field);
        }
    }

    TIFFField getField(int tagNumber) {
        return fieldMap.get(tagNumber);
    }

    boolean containsField(int tagNumber) {
        return fieldMap.containsKey(tagNumber);
    }

    boolean isGeotiff() {
        return fieldMap.containsKey(GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY);
    }

    public SortedMap<Integer, GeoKeyEntry> getGeoKeyEntries() {
        final int[] dirValues = getGeoKeyDirValues();
        final double[] doubleValues = getGeoDoubleParamValues();
        final String[] asciiValues = getGeoAsciiParamValues();
        final int size = (dirValues.length / 4) - 1;
        int strIdx = 0;
        final SortedMap<Integer, GeoKeyEntry> map = new TreeMap<>();
        for (int i = 0; i < size; i++) {
            final int offset = (i + 1) * 4;
            final int keyId = dirValues[offset];
            final int tiffTagLocation = dirValues[offset + 1];
            final int count = dirValues[offset + 2];
            final int offsetOrValue = dirValues[offset + 3];
            final Object value;

            if (tiffTagLocation == TAG_GEO_DOUBLE_PARAMS___SPOT) {
                final double[] doubles = new double[count];
                System.arraycopy(doubleValues, offsetOrValue, doubles, 0, count);
                value = doubles;
            } else if (tiffTagLocation == TAG_GEO_ASCII_PARAMS___SPOT) {
                value = asciiValues[strIdx++];
            } else {
                value = new Integer(offsetOrValue);
            }
            map.put(keyId, new GeoKeyEntry(keyId, tiffTagLocation, count, value));
        }
        return map;
    }

    private int[] getGeoKeyDirValues() {
        if (!containsField(TAG_GEO_KEY_DIRECTORY___SPOT)) {
            throw new IllegalStateException("no GEO_KEY_DIRECTORY");
        }
        final TIFFField field = getField(TAG_GEO_KEY_DIRECTORY___SPOT);
        final int count = field.getCount();
        final int[] ints = new int[count];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = field.getAsInt(i);
        }
        return ints;
    }

    private double[] getGeoDoubleParamValues() {
        if (containsField(GeoTIFFTagSet.TAG_GEO_DOUBLE_PARAMS)) {
            return getDoubleValues(getField(GeoTIFFTagSet.TAG_GEO_DOUBLE_PARAMS));
        }
        return new double[0];
    }

    private static double[] getDoubleValues(TIFFField field) {
        final int count = field.getCount();
        final double[] doubles = new double[count];
        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = field.getAsDouble(i);
        }
        return doubles;
    }

    private String[] getGeoAsciiParamValues() {
        if (containsField(GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS)) {
            final TIFFField field = getField(GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS);
            final String[] values = getStringValues(field);
            final ArrayList<String> strings = new ArrayList<>();
            for (String value : values) {
                value = value.replace("\u0000", "");
                strings.addAll(Arrays.asList(value.split("\\|")));
            }
            return strings.toArray(new String[strings.size()]);
        }
        return new String[0];
    }

    private static String[] getStringValues(TIFFField field) {
        final int count = field.getCount();
        final String[] strings = new String[count];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = field.getAsString(i);
        }
        return strings;
    }
}
