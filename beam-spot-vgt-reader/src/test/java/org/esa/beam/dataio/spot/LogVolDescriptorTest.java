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

package org.esa.beam.dataio.spot;

import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;

import static org.junit.Assert.*;

@RunWith(TestDirRunner.class)
public class LogVolDescriptorTest {
    @Test
    public void testIt() throws IOException, ParseException {
        File dir = TestDataDir.get();
        File file = new File(dir, "decode_qual_intended/0001/0001_LOG.TXT");
        FileReader reader = new FileReader(file);
        try {
            LogVolDescriptor descriptor = new LogVolDescriptor(reader);
            assertEquals("V2KRNS10__20060721E", descriptor.getProductId());
            assertNotNull(descriptor.getGeoCoding());
            assertEquals("20-JUL-2006 22:31:32.000000", ProductData.UTC.create(descriptor.getStartDate(), 0).toString());
            assertEquals("30-JUL-2006 23:56:28.000000", ProductData.UTC.create(descriptor.getEndDate(), 0).toString());
            Rectangle bounds = descriptor.getImageBounds();
            assertNotNull(bounds);
            assertEquals(0, bounds.x);
            assertEquals(0, bounds.y);
            assertEquals(8177, bounds.width);
            assertEquals(5601, bounds.height);
        } finally {
            reader.close();
        }
    }
}
