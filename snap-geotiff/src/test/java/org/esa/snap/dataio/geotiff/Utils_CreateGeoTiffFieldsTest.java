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

import com.sun.media.jai.codec.TIFFField;
import it.geosolutions.imageio.plugins.tiff.GeoTIFFTagSet;
import org.esa.snap.core.util.geotiff.GeoTIFFMetadata;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class Utils_CreateGeoTiffFieldsTest {

    private GeoTIFFMetadata metadata;

    @Before
    public void setup() {
        metadata = new GeoTIFFMetadata(1, 1, 2);
    }

    @Test
    public void testVersionOnly() {
        final List<TIFFField> list = Utils.createGeoTIFFFields(metadata);

        assertNotNull(list);
        assertEquals(1, list.size());
        final TIFFField dirField = list.get(0);

        assertEquals(GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY, dirField.getTag());
        assertEquals(TIFFField.TIFF_SHORT, dirField.getType());
        assertEquals(4, dirField.getCount());
        final char[] expected = {1, 1, 2, 0};
        assertArrayEquals(expected, dirField.getAsChars());
    }

    @Test
    public void testThreeGeoTIFFShortTags() {
        metadata.addGeoShortParam(2300, 4576);
        metadata.addGeoShortParam(2400, 12);
        metadata.addGeoShortParam(2401, 3456);

        final List<TIFFField> list = Utils.createGeoTIFFFields(metadata);

        assertNotNull(list);
        assertEquals(1, list.size());
        final TIFFField dirField = list.get(0);

        assertEquals(GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY, dirField.getTag());
        assertEquals(TIFFField.TIFF_SHORT, dirField.getType());
        assertEquals(16, dirField.getCount());
        final char[] expected = {
                1, 1, 2, 3,
                2300, 0, 1, 4576,
                2400, 0, 1, 12,
                2401, 0, 1, 3456
        };
        assertArrayEquals(expected, dirField.getAsChars());
    }

    @Test
    public void testThreeGeoTIFFTagsWithOneAscii() {
        metadata.addGeoShortParam(2300, 4576);
        metadata.addGeoAscii(2400, "String");
        metadata.addGeoShortParam(2401, 3456);

        final List<TIFFField> list = Utils.createGeoTIFFFields(metadata);

        assertNotNull(list);
        assertEquals(2, list.size());

        final TIFFField dirField = list.get(0);
        final TIFFField asciiField = list.get(1);

        assertEquals(GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY, dirField.getTag());
        assertEquals(GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS, asciiField.getTag());

        assertEquals(TIFFField.TIFF_SHORT, dirField.getType());
        assertEquals(TIFFField.TIFF_ASCII, asciiField.getType());

        assertEquals(16, dirField.getCount());
        assertEquals(1, asciiField.getCount());

        final char[] expected = {
                1, 1, 2, 3,
                2300, 0, 1, 4576,
                2400, GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS, 7, 0,
                2401, 0, 1, 3456
        };
        assertArrayEquals(expected, dirField.getAsChars());
        assertEquals("String|", asciiField.getAsString(0));
    }

    @Test
    public void testThreeGeoTIFFTagsWithOneDouble() {
        metadata.addGeoShortParam(2300, 4576);
        metadata.addGeoDoubleParam(2400, 4.5);
        metadata.addGeoShortParam(2401, 3456);

        final List<TIFFField> list = Utils.createGeoTIFFFields(metadata);

        assertNotNull(list);
        assertEquals(2, list.size());

        final TIFFField dirField = list.get(0);
        final TIFFField doubleField = list.get(1);

        assertEquals(GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY, dirField.getTag());
        assertEquals(GeoTIFFTagSet.TAG_GEO_DOUBLE_PARAMS, doubleField.getTag());

        assertEquals(TIFFField.TIFF_SHORT, dirField.getType());
        assertEquals(TIFFField.TIFF_DOUBLE, doubleField.getType());

        assertEquals(16, dirField.getCount());
        assertEquals(1, doubleField.getCount());

        final char[] expected = {
                1, 1, 2, 3,
                2300, 0, 1, 4576,
                2400, GeoTIFFTagSet.TAG_GEO_DOUBLE_PARAMS, 1, 0,
                2401, 0, 1, 3456
        };
        assertArrayEquals(expected, dirField.getAsChars());
        assertEquals(true, Arrays.equals(new double[]{4.5}, doubleField.getAsDoubles()));
    }

    @Test
    public void testThreeGeoTIFFTagsWithOneDoubleAndOneString() {
        metadata.addGeoShortParam(2300, 4576);
        metadata.addGeoDoubleParam(2400, 4.5);
        metadata.addGeoAscii(2401, "dlkjfg");

        final List<TIFFField> list = Utils.createGeoTIFFFields(metadata);

        assertNotNull(list);
        assertEquals(3, list.size());

        final TIFFField dirField = list.get(0);
        final TIFFField doubleField = list.get(1);
        final TIFFField asciiField = list.get(2);

        assertEquals(GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY, dirField.getTag());
        assertEquals(GeoTIFFTagSet.TAG_GEO_DOUBLE_PARAMS, doubleField.getTag());
        assertEquals(GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS, asciiField.getTag());

        assertEquals(TIFFField.TIFF_SHORT, dirField.getType());
        assertEquals(TIFFField.TIFF_DOUBLE, doubleField.getType());
        assertEquals(TIFFField.TIFF_ASCII, asciiField.getType());

        assertEquals(16, dirField.getCount());
        assertEquals(1, doubleField.getCount());
        assertEquals(1, asciiField.getCount());

        final char[] expected = {
                1, 1, 2, 3,
                2300, 0, 1, 4576,
                2400, GeoTIFFTagSet.TAG_GEO_DOUBLE_PARAMS, 1, 0,
                2401, GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS, 7, 0
        };
        assertArrayEquals(expected, dirField.getAsChars());
        assertEquals(true, Arrays.equals(new double[]{4.5}, doubleField.getAsDoubles()));
        assertEquals("dlkjfg|", asciiField.getAsString(0));
    }

    @Test
    public void testThreeGeoTIFFStringTags() {
        metadata.addGeoAscii(2300, "4576");
        metadata.addGeoAscii(2400, "aaaaaaaa");
        metadata.addGeoAscii(2401, "bbbb");

        final List<TIFFField> list = Utils.createGeoTIFFFields(metadata);

        assertNotNull(list);
        assertEquals(2, list.size());

        final TIFFField dirField = list.get(0);
        final TIFFField asciiField = list.get(1);

        assertEquals(GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY, dirField.getTag());
        assertEquals(GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS, asciiField.getTag());

        assertEquals(TIFFField.TIFF_SHORT, dirField.getType());
        assertEquals(TIFFField.TIFF_ASCII, asciiField.getType());

        assertEquals(16, dirField.getCount());
        assertEquals(3, asciiField.getCount());

        final char[] expected = {
                1, 1, 2, 3,
                2300, GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS, 5, 0,
                2400, GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS, 9, 5,
                2401, GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS, 5, 14
        };
        assertArrayEquals(expected, dirField.getAsChars());
        assertEquals("4576|", asciiField.getAsString(0));
        assertEquals("aaaaaaaa|", asciiField.getAsString(1));
        assertEquals("bbbb|", asciiField.getAsString(2));
    }

    @Test
    public void testVersionAndModelPixelScale() {
        metadata.setModelPixelScale(1, 2, 3);
        final List<TIFFField> list = Utils.createGeoTIFFFields(metadata);

        assertNotNull(list);
        assertEquals(2, list.size());

        final TIFFField dirField = list.get(0);
        assertEquals(GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY, dirField.getTag());
        assertEquals(TIFFField.TIFF_SHORT, dirField.getType());
        assertEquals(4, dirField.getCount());
        assertArrayEquals(new char[]{1, 1, 2, 0}, dirField.getAsChars());

        final TIFFField scaleField = list.get(1);
        assertEquals(GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE, scaleField.getTag());
        assertEquals(TIFFField.TIFF_DOUBLE, scaleField.getType());
        assertEquals(3, scaleField.getCount());
        assertEquals(true, Arrays.equals(new double[]{1, 2, 3}, scaleField.getAsDoubles()));
    }

    @Test
    public void testVersionAndModelTiePoint() {
        metadata.addModelTiePoint(1, 2, 3, 4, 5, 6);
        metadata.addModelTiePoint(2, 3, 4, 5, 6, 7);
        metadata.addModelTiePoint(3, 4, 5, 6, 7, 8);
        final List<TIFFField> list = Utils.createGeoTIFFFields(metadata);

        assertNotNull(list);
        assertEquals(2, list.size());

        final TIFFField dirField = list.get(0);
        assertEquals(GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY, dirField.getTag());
        assertEquals(TIFFField.TIFF_SHORT, dirField.getType());
        assertEquals(4, dirField.getCount());
        assertArrayEquals(new char[]{1, 1, 2, 0}, dirField.getAsChars());

        final TIFFField tiePointField = list.get(1);
        assertEquals(GeoTIFFTagSet.TAG_MODEL_TIE_POINT, tiePointField.getTag());
        assertEquals(TIFFField.TIFF_DOUBLE, tiePointField.getType());
        assertEquals(3 * 6, tiePointField.getCount());
        final double[] expected = {
                1, 2, 3, 4, 5, 6,
                2, 3, 4, 5, 6, 7,
                3, 4, 5, 6, 7, 8
        };
        assertEquals(true, Arrays.equals(expected, tiePointField.getAsDoubles()));
    }

    @Test
    public void testVersionAndModelTransformation() {
        metadata.setModelTransformation(new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16});
        final List<TIFFField> list = Utils.createGeoTIFFFields(metadata);

        assertNotNull(list);
        assertEquals(2, list.size());

        final TIFFField dirField = list.get(0);
        assertEquals(GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY, dirField.getTag());
        assertEquals(TIFFField.TIFF_SHORT, dirField.getType());
        assertEquals(4, dirField.getCount());
        assertArrayEquals(new char[]{1, 1, 2, 0}, dirField.getAsChars());

        final TIFFField transformField = list.get(1);
        assertEquals(GeoTIFFTagSet.TAG_MODEL_TRANSFORMATION, transformField.getTag());
        assertEquals(TIFFField.TIFF_DOUBLE, transformField.getType());
        assertEquals(16, transformField.getCount());
        final double[] expected = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        assertEquals(true, Arrays.equals(expected, transformField.getAsDoubles()));
    }

}
