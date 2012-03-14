package org.esa.beam.dataio.modis;

import junit.framework.TestCase;

import java.io.File;

public class ModisImappAttributesTest extends TestCase {

    private ModisImappAttributes.FileDescriptor fileDescriptor;

    public void testSetGetProductName() {
        final String name_1 = "modis popodis";
        final String name_2 = "firlefanz";

        fileDescriptor.setProductName(name_1);
        assertEquals(name_1, fileDescriptor.getProductName());

        fileDescriptor.setProductName(name_2);
        assertEquals(name_2, fileDescriptor.getProductName());
    }

    public void testSetGetProductType() {
        final String type_1 = "the Type";
        final String type_2 = "the other Type";

        fileDescriptor.setProductType(type_1);
        assertEquals(type_1, fileDescriptor.getProductType());

        fileDescriptor.setProductType(type_2);
        assertEquals(type_2, fileDescriptor.getProductType());
    }

    public void testParseFileNameAndType() {
        final File mydFile = new File("MYD09GA.A2010031.h00v08.005.2010033144508.hdf");

        final ModisImappAttributes.FileDescriptor descriptor = ModisImappAttributes.parseFileNameAndType(mydFile);
        assertNotNull(descriptor);
        assertEquals("MYD09GA.A2010031.h00v08.005.2010033144508", descriptor.getProductName());
        assertEquals("MYD09GA", descriptor.getProductType());
    }

    public void testParseFileNameAndType_unknownType() {
        final File mydFile = new File("ATS_NR__2PNPDK20060329_103452_000065272046_00223_21319_0188.N1");

        final ModisImappAttributes.FileDescriptor descriptor = ModisImappAttributes.parseFileNameAndType(mydFile);
        assertNotNull(descriptor);
        assertEquals("ATS_NR__2PNPDK20060329_103452_000065272046_00223_21319_0188", descriptor.getProductName());
        assertEquals("unknown", descriptor.getProductType());
    }

    ////////////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void setUp() {
        fileDescriptor = new ModisImappAttributes.FileDescriptor();
    }
}
