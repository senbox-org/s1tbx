/*
 * $Id: AtsrProductReaderPlugInTest.java,v 1.1 2006/09/12 13:19:07 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.atsr;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.beam.framework.dataio.ProductReader;

import javax.imageio.stream.ImageInputStream;
import java.io.File;

public class AtsrProductReaderPlugInTest extends TestCase {

    private AtsrProductReaderPlugIn _plugIn = null;

    public AtsrProductReaderPlugInTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(AtsrProductReaderPlugInTest.class);
    }

    @Override
    protected void setUp() {
        _plugIn = new AtsrProductReaderPlugIn();
        assertNotNull(_plugIn);
    }

    public void testFormatNames() {
        String[] actualNames = null;
        String[] expectedNames = new String[]{AtsrConstants.ATSR_FORMAT_NAME};

        actualNames = _plugIn.getFormatNames();
        assertNotNull(actualNames);

        for (int n = 0; n < expectedNames.length; n++) {
            assertEquals(expectedNames[n], actualNames[n]);
        }
    }

    public void testGetDescrition() {
        assertEquals(AtsrConstants.DESCRIPTION, _plugIn.getDescription(null));
    }

    public void testGetInputTypes() {
        Class[] expected = new Class[]{String.class, File.class, ImageInputStream.class};
        Class[] actual = null;

        actual = _plugIn.getInputTypes();
        assertNotNull(actual);

        for (int n = 0; n < expected.length; n++) {
            assertEquals(expected[n], actual[n]);
        }
    }

    public void testCreateInstance() {
        ProductReader reader = null;

        reader = _plugIn.createReaderInstance();
        assertNotNull(reader);
        assertTrue(reader instanceof AtsrProductReader);
    }
}
