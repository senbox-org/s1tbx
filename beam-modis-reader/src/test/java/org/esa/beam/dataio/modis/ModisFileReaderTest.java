package org.esa.beam.dataio.modis;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;

public class ModisFileReaderTest extends TestCase {

    public void testGetTypeString_typeNull() {
        final Product product = new Product("Name", "PROD_TYPE",5, 5 );

        assertEquals("PROD_TYPE", ModisFileReader.getTypeString(null, product));
    }

    public void testGetTypeString_typeSupplied() {
        final Product product = new Product("Name", "PROD_TYPE",5, 5 );

        assertEquals("TYPE_SRING", ModisFileReader.getTypeString("TYPE_SRING", product));
    }
}
