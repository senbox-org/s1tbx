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

import junit.framework.TestCase;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class LogVolDescriptorTest extends TestCase {

    public void testIt() throws IOException,ParseException {
        File dir = TestDataDir.get();
        File file = new File(dir, "decode_qual_intended/0001/0001_LOG.TXT");
        FileReader reader = new FileReader(file);
        try {
            LogVolDescriptor descriptor = new LogVolDescriptor(reader);
            assertEquals("V2KRNS10__20060721E", descriptor.getProductId());
            assertNotNull(descriptor.getGeoCoding());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
            assertEquals(dateFormat.parse("20060720223132"), descriptor.getStartDate());
            assertEquals(dateFormat.parse("20060730235628"), descriptor.getEndDate());
            Rectangle bounds = descriptor.getImageBounds();
            assertNotNull( bounds);
            assertEquals(0, bounds.x);
            assertEquals(0, bounds.y);
            assertEquals(8177, bounds.width);
            assertEquals(5601, bounds.height);
        } finally {
            reader.close();
        }
    }
}
