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

package org.esa.beam.framework.ui.application.support;

import junit.framework.TestCase;
import com.thoughtworks.xstream.XStream;

public class DefaultApplicationDescriptorTest extends TestCase {
    private static final String XML =
            "<applicationDescriptor>\n" +
            "  <applicationId>DatMain</applicationId>\n" +
            "  <displayName>DAT</displayName>\n" +
            "  <frameIcons>/org/esa/nest/dat/images/frame-icon.png</frameIcons>\n" +
            "  <aboutImage>/org/esa/nest/dat/images/about.jpg</aboutImage>\n" +
            "  <excludedActions>\n" +
            "      <id>a1</id>\n" +
            "      <id>a2</id>\n" +
            "  </excludedActions>\n" +
            "  <excludedToolViews>\n" +
            "      <id>tv1</id>\n" +
            "      <id>tv2</id>\n" +
            "      <id>tv3</id>\n" +
            "  </excludedToolViews>\n" +
            "</applicationDescriptor>";

    public void testUnmarshalling() {

        final XStream xStream = new XStream();
        xStream.autodetectAnnotations(true);
        xStream.alias("applicationDescriptor", DefaultApplicationDescriptor.class);
        Object o = null;
        try {
           o = xStream.fromXML(XML);
        } catch (Exception e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
        assertNotNull(o);
        assertSame(DefaultApplicationDescriptor.class, o.getClass());
        DefaultApplicationDescriptor defaultApplicationDescriptor = (DefaultApplicationDescriptor) o;

        assertEquals("DatMain", defaultApplicationDescriptor.getApplicationId());
        assertEquals("DAT", defaultApplicationDescriptor.getDisplayName());
        assertEquals("/org/esa/nest/dat/images/frame-icon.png", defaultApplicationDescriptor.getFrameIconPaths());
        assertEquals("/org/esa/nest/dat/images/about.jpg", defaultApplicationDescriptor.getAboutImagePath());


        assertNotNull(defaultApplicationDescriptor.getExcludedActions());
        assertEquals(2, defaultApplicationDescriptor.getExcludedActions().length);
        assertEquals("a1", defaultApplicationDescriptor.getExcludedActions()[0]);
        assertEquals("a2", defaultApplicationDescriptor.getExcludedActions()[1]);

        assertNotNull(defaultApplicationDescriptor.getExcludedToolViews());
        assertEquals(3, defaultApplicationDescriptor.getExcludedToolViews().length);
        assertEquals("tv1", defaultApplicationDescriptor.getExcludedToolViews()[0]);
        assertEquals("tv2", defaultApplicationDescriptor.getExcludedToolViews()[1]);
        assertEquals("tv3", defaultApplicationDescriptor.getExcludedToolViews()[2]);
    }
}
