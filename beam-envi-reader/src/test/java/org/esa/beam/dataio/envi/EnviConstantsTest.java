package org.esa.beam.dataio.envi;

import junit.framework.TestCase;

public class EnviConstantsTest extends TestCase {

    public void testTheConstants() {
        assertEquals("samples", EnviConstants.HEADER_KEY_SAMPLES);
        assertEquals("lines", EnviConstants.HEADER_KEY_LINES);
        assertEquals("bands", EnviConstants.HEADER_KEY_BANDS);
        assertEquals("header offset", EnviConstants.HEADER_KEY_HEADER_OFFSET);
        assertEquals("file type", EnviConstants.HEADER_KEY_FILE_TYPE);
        assertEquals("data type", EnviConstants.HEADER_KEY_DATA_TYPE);
        assertEquals("interleave", EnviConstants.HEADER_KEY_INTERLEAVE);
        assertEquals("sensor type", EnviConstants.HEADER_KEY_SENSOR_TYPE);
        assertEquals("byte order", EnviConstants.HEADER_KEY_BYTE_ORDER);
        assertEquals("map info", EnviConstants.HEADER_KEY_MAP_INFO);
        assertEquals("projection info", EnviConstants.HEADER_KEY_PROJECTION_INFO);
        assertEquals("wavelength units", EnviConstants.HEADER_KEY_WAVELENGTH_UNITS);
        assertEquals("band names", EnviConstants.HEADER_KEY_BAND_NAMES);
        assertEquals("description", EnviConstants.HEADER_KEY_DESCRIPTION);

        assertEquals("ENVI", EnviConstants.FIRST_LINE);
        assertEquals("ENVI", EnviConstants.FORMAT_NAME);
        assertEquals("ENVI Data Products", EnviConstants.DESCRIPTION);
        assertEquals(".zip", EnviConstants.ZIP_EXTENSION);

        assertEquals(1, EnviConstants.TYPE_ID_BYTE);
        assertEquals(2, EnviConstants.TYPE_ID_INT16);
        assertEquals(3, EnviConstants.TYPE_ID_INT32);
        assertEquals(4, EnviConstants.TYPE_ID_FLOAT32);
        assertEquals(5, EnviConstants.TYPE_ID_FLOAT64);
        assertEquals(12, EnviConstants.TYPE_ID_UINT16);
        assertEquals(13, EnviConstants.TYPE_ID_UINT32);
    }
}
