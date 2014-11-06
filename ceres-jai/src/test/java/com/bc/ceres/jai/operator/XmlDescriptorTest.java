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