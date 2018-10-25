/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.dataio.geotiff;

import com.bc.ceres.core.Assert;
import it.geosolutions.imageio.plugins.tiff.GeoTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFDirectory;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.dataio.geotiff.internal.GeoKeyEntry;
import org.esa.snap.dataio.geotiff.internal.GeoKeyHeader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

class TiffFileInfo {

    private static final int TAG_GEO_KEY_DIRECTORY___SPOT = 34735;
    private static final int TAG_GEO_DOUBLE_PARAMS___SPOT = 34736;
    private static final int TAG_GEO_ASCII_PARAMS___SPOT = 34737;

    private final Map<Integer, TIFFField> fieldMap;

    TiffFileInfo(final TIFFDirectory dir) {
        Assert.notNull(dir);
        final TIFFField[] tiffFields = dir.getTIFFFields();
        fieldMap = new HashMap<Integer, TIFFField>(tiffFields.length);
        for (TIFFField tiffField : tiffFields) {
            fieldMap.put(tiffField.getTagNumber(), tiffField);
        }
    }

    public boolean isGeotiff() {
        return fieldMap.containsKey(GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY);
    }

    public TIFFField getField(int tagNumber) {
        return fieldMap.get(tagNumber);
    }

    public boolean containsField(int tagNumber) {
        return fieldMap.containsKey(tagNumber);
    }

    public static double[] getDoubleValues(TIFFField field) {
        final int count = field.getCount();
        final double[] doubles = new double[count];
        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = field.getAsDouble(i);
        }
        return doubles;
    }

    public static String[] getStringValues(TIFFField field) {
        final int count = field.getCount();
        final String[] strings = new String[count];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = field.getAsString(i);
        }
        return strings;
    }

    public SortedMap<Integer, GeoKeyEntry> getGeoKeyEntries() {
        final int[] dirValues = getGeoKeyDirValues();
        final double[] doubleValues = getGeoDoubleParamValues();
        final String[] asciiValues = getGeoAsciiParamValues();
        final int size = (dirValues.length / 4) - 1;
        int strIdx = 0;
        final SortedMap<Integer, GeoKeyEntry> map = new TreeMap<Integer, GeoKeyEntry>();
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
            } else if (tiffTagLocation == TAG_GEO_ASCII_PARAMS___SPOT && asciiValues.length > strIdx) {
                value = asciiValues[strIdx++];
            } else {
                value = new Integer(offsetOrValue);
            }
            map.put(keyId, new GeoKeyEntry(keyId, tiffTagLocation, count, value));
        }
        return map;
    }

    public GeoKeyHeader getGeoKeyHeader() throws ProductIOException {
        final int[] dirValues = getGeoKeyDirValues();
        return new GeoKeyHeader(dirValues[0], dirValues[1], dirValues[2], dirValues[3]);
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

    private String[] getGeoAsciiParamValues() {
        if (containsField(GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS)) {
            final TIFFField field = getField(GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS);
            final String[] values = getStringValues(field);
            final ArrayList<String> strings = new ArrayList<String>();
            for (String value : values) {
                value = value.replace("\u0000", "");
                strings.addAll(Arrays.asList(value.split("\\|")));
            }
            return strings.toArray(new String[strings.size()]);
        }
        return new String[0];
    }

}
