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

package org.esa.snap.core.dataio;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.IOException;

public class ProductIOTest extends TestCase {

    public ProductIOTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ProductIOTest.class);
    }

    public void testThatDefaultReaderAndWriterAreImplemented() {
        assertNotNull(ProductIO.getProductReader("BEAM-DIMAP"));
        assertNotNull(ProductIO.getProductWriter("BEAM-DIMAP"));
    }

    public void testReadProductArgsChecking() {
        try {
            ProductIO.readProduct((File) null);
            fail();
        } catch (IOException expected) {
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            ProductIO.readProduct("rallala");
            fail();
        } catch (IOException expected) {
        }
    }

}
