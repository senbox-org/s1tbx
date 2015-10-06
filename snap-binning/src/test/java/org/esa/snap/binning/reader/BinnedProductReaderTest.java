/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning.reader;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;
import ucar.nc2.NetcdfFile;

import java.net.URL;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class BinnedProductReaderTest {

    @Test
    public void testExtractStartTime() throws Exception {
        final URL resource = getClass().getResource("test.nc");
        final NetcdfFile netcdfFile = NetcdfFile.openInMemory(resource.toURI());
        ProductData.UTC startTime = BinnedProductReader.extractStartTime(netcdfFile);
        assertEquals(ProductData.UTC.parse("20030101:0000", "yyyyMMdd:HHmm").getAsDate().getTime(), startTime.getAsDate().getTime());
    }

    @Test
    public void testExtractEndTime() throws Exception {
        final URL resource = getClass().getResource("test.nc");
        final NetcdfFile netcdfFile = NetcdfFile.openInMemory(resource.toURI());
        ProductData.UTC startTime = BinnedProductReader.extractEndTime(netcdfFile);
        assertEquals(ProductData.UTC.parse("20030101:0000", "yyyyMMdd:HHmm").getAsDate().getTime(), startTime.getAsDate().getTime());
    }

    @Test
    public void testExtractStartTime_NoTimeInfo() throws Exception {
        final URL resource = getClass().getResource("test_without_time_info.nc");
        final NetcdfFile netcdfFile = NetcdfFile.openInMemory(resource.toURI());
        ProductData.UTC startTime = BinnedProductReader.extractStartTime(netcdfFile);
        assertNull(startTime);
    }

    @Test
    public void testExtractEndTime_NoTimeInfo() throws Exception {
        final URL resource = getClass().getResource("test_without_time_info.nc");
        final NetcdfFile netcdfFile = NetcdfFile.openInMemory(resource.toURI());
        ProductData.UTC endTime = BinnedProductReader.extractEndTime(netcdfFile);
        assertNull(endTime);
    }

    @Test
    public void testGetWavelengthFromBandName() {
        assertEquals(670, BinnedProductReader.getWavelengthFromBandName("atot_670"));
        assertEquals(443, BinnedProductReader.getWavelengthFromBandName("aph_443"));

        assertEquals(0, BinnedProductReader.getWavelengthFromBandName("chlor_a"));
        assertEquals(0, BinnedProductReader.getWavelengthFromBandName("latitude"));
    }

    @Test
    public void testIsSubSampled() {
         assertTrue(BinnedProductReader.isSubSampled(2, 1));
         assertTrue(BinnedProductReader.isSubSampled(1, 4));

         assertFalse(BinnedProductReader.isSubSampled(1, 1));
    }
}
