package org.esa.beam.dataio.landsat.geotiff;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import static org.junit.Assert.*;


public class LandsatMetadataTest {

    @Test
    public void testfoo() throws IOException {
        InputStream stream = LandsatMetadataTest.class.getResourceAsStream("test_MTL.txt");
        InputStreamReader reader = new InputStreamReader(stream);
        MetadataElement element = new LandsatMetadata(reader).getMetaDataElementRoot();
        assertNotNull(element);
        assertEquals("L1_METADATA_FILE", element.getName());
        MetadataElement[] childElements = element.getElements();
        assertEquals(8, childElements.length);
        MetadataElement firstChild = childElements[0];
        assertEquals("METADATA_FILE_INFO", firstChild.getName());
        assertEquals(0, firstChild.getElements().length);
        MetadataAttribute[] attributes = firstChild.getAttributes();
        assertEquals(9, attributes.length);
        MetadataAttribute originAttr = attributes[0];
        assertEquals("ORIGIN", originAttr.getName());
        assertEquals(ProductData.TYPESTRING_ASCII, originAttr.getData().getTypeString());
        assertEquals("Image courtesy of the U.S. Geological Survey", originAttr.getData().getElemString());
    }
}
