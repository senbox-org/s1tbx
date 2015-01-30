package org.esa.beam.dataio.bigtiff.internal;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

@SuppressWarnings("EmptyCatchBlock")
public class GeoKeyEntryTest {

    @Test
    public void testConstructAndGet_Integer(){
        final GeoKeyEntry entry = new GeoKeyEntry(3072, 9, 1, 89);

        assertEquals(89, entry.getIntValue().intValue());
        assertEquals(3072, entry.getKeyId());
        assertEquals("ProjectedCSTypeGeoKey", entry.getName());

        assertTrue(entry.hasIntValue());
        assertFalse(entry.hasStringValue());
        assertFalse(entry.hasDoubleValues());
    }

    @Test
    public void testConstructAndGet_String(){
        final GeoKeyEntry entry = new GeoKeyEntry(3073, 10, 1, "theTestValue");

        assertEquals("theTestValue", entry.getStringValue());
        assertEquals(3073, entry.getKeyId());
        assertEquals("PCSCitationGeoKey", entry.getName());

        assertFalse(entry.hasIntValue());
        assertTrue(entry.hasStringValue());
        assertFalse(entry.hasDoubleValues());
    }

    @Test
    public void testConstructAndGet_DoubleArray(){
        final GeoKeyEntry entry = new GeoKeyEntry(3074, 11, 3, new double[]{1.0, 2.0, 3.0});

        final double[] values = entry.getDoubleValues();
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, values, 1e-8);
        assertEquals(3074, entry.getKeyId());
        assertEquals("ProjectionGeoKey", entry.getName());

        assertFalse(entry.hasIntValue());
        assertFalse(entry.hasStringValue());
        assertTrue(entry.hasDoubleValues());
    }

    @Test
    public void testConstruct_unsupportedType() {
        try {
            new GeoKeyEntry(3074, 12, 2, new File("."));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }
}
