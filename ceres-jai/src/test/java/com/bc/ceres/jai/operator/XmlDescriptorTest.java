package com.bc.ceres.jai.operator;

import junit.framework.TestCase;

import javax.media.jai.RenderedOp;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

public class XmlDescriptorTest extends TestCase {
    public void testInvalidArgs() throws URISyntaxException {
        try {
            XmlDescriptor.create(new URI(""), null, null);
        } catch (IllegalArgumentException e) {
            fail("Unexpected: " + e);
        }
        try {
            XmlDescriptor.create(null, new HashMap<String, Object>(), null);
            fail();
          } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            XmlDescriptor.create(null, null, null);
            fail();
          } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testSimple() throws URISyntaxException {
        testSimple(getResource("nested.jai.xml"));
        testSimple(getResource("flat.jai.xml"));
    }

    private void testSimple(URI uri) {
        HashMap<String, Object> configuration = new HashMap<String, Object>();
        configuration.put("source0", SourceImageFactory.createOneBandedUShortImage(2, 2, new short[]{1, 2, 3, 4}));
        RenderedOp op = XmlDescriptor.create(uri, configuration, null);
        assertNotNull(op);

        Raster data = op.getData();
        assertEquals(DataBuffer.TYPE_USHORT, data.getSampleModel().getDataType());
        assertEquals(50 * (1 + 2), data.getSample(0, 0, 0));
        assertEquals(50 * (2 + 2), data.getSample(1, 0, 0));
        assertEquals(50 * (3 + 2), data.getSample(0, 1, 0));
        assertEquals(50 * (4 + 2), data.getSample(1, 1, 0));
    }

    private URI getResource(String name) throws URISyntaxException {
        URL url = getClass().getResource(name);
        assertNotNull(url);
        return url.toURI();
    }
}