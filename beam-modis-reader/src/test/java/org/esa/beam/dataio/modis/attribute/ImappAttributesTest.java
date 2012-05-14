package org.esa.beam.dataio.modis.attribute;

import junit.framework.TestCase;
import org.esa.beam.dataio.modis.ModisGlobalAttributes;

import java.io.File;

public class ImappAttributesTest extends TestCase {

    @SuppressWarnings("ConstantConditions")
    public void testInheritance() {
        final ImappAttributes imappAttributes = new ImappAttributes(new File("."));

        assertTrue(imappAttributes instanceof ModisGlobalAttributes);
    }

    public void testGetProductType() {
        ImappAttributes imappAttributes = new ImappAttributes(new File("MOD021KM.A2006038.0722.hdf"));
        assertEquals("MOD021KM", imappAttributes.getProductType());

        imappAttributes = new ImappAttributes(new File("ATS_NR__2PNMAP20060918_095303_000000532051_00193_23794_0001.N1"));
        assertEquals("unknown", imappAttributes.getProductType());
    }

    public void testGetProductName() {
        ImappAttributes imappAttributes = new ImappAttributes(new File("MOD021KM.A2006038.0722.hdf"));
        assertEquals("MOD021KM.A2006038.0722", imappAttributes.getProductName());

        imappAttributes = new ImappAttributes(new File("MOD021KM.A20050930110428.20050930111128_v1.5.hdf"));
        assertEquals("MOD021KM.A20050930110428.20050930111128_v1.5", imappAttributes.getProductName());
    }

    public void testIsImappFormat() {
        final ImappAttributes imappAttributes = new ImappAttributes(new File("."));
        assertTrue(imappAttributes.isImappFormat());
    }

    public void testGetEosType() {
        final ImappAttributes imappAttributes = new ImappAttributes(new File("."));
        assertNull(imappAttributes.getEosType());
    }

    public void testCreateGeoCoding() {
        final ImappAttributes imappAttributes = new ImappAttributes(new File("."));
        assertNull(imappAttributes.createGeocoding());
    }
}
