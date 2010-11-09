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

package org.esa.beam.dataio.netcdf;

import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfNetCdfReaderPlugIn;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Calendar;

import static org.junit.Assert.*;

/**
 * @author Ralf Quast
 * @version $Revision: 2182 $ $Date: 2008-06-12 11:09:11 +0200 (Do, 12 Jun 2008) $
 */
public class Nc4ReaderTest {

    @Test
    public void testGlobalAttributes() throws IOException {

        final URL url = Nc4ReaderTest.class.getResource("test.nc");
        assertNotNull(url);
        assertEquals("file", url.getProtocol());

        final String path = URLDecoder.decode(url.getPath(), "UTF-8");
        assertTrue(path.endsWith("test.nc"));

        final File file = new File(path);
        assertEquals(file.getName(), "test.nc");
        assertTrue(file.exists());
        assertTrue(file.canRead());

        final ProductReader reader = new CfNetCdfReaderPlugIn().createReaderInstance();
        final Product product = reader.readProductNodes(file.getPath(), null);
        assertNotNull(product);

        testStartTime(product);
        testEndTime(product);
    }

    private void testStartTime(final Product product) {
        final ProductData.UTC utc = product.getStartTime();
        assertNotNull(utc);

        final Calendar startTime = utc.getAsCalendar();
        assertEquals(2002, startTime.get(Calendar.YEAR));
        assertEquals(11, startTime.get(Calendar.MONTH));
        assertEquals(24, startTime.get(Calendar.DATE));
        assertEquals(11, startTime.get(Calendar.HOUR_OF_DAY));
        assertEquals(12, startTime.get(Calendar.MINUTE));
        assertEquals(13, startTime.get(Calendar.SECOND));
    }

    private void testEndTime(final Product product) {
        final ProductData.UTC utc = product.getEndTime();
        assertNotNull(utc);

        final Calendar endTime = utc.getAsCalendar();
        assertEquals(2002, endTime.get(Calendar.YEAR));
        assertEquals(11, endTime.get(Calendar.MONTH));
        assertEquals(24, endTime.get(Calendar.DATE));
        assertEquals(11, endTime.get(Calendar.HOUR_OF_DAY));
        assertEquals(12, endTime.get(Calendar.MINUTE));
        assertEquals(14, endTime.get(Calendar.SECOND));
    }
}
