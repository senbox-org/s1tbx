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

package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.util.RasterDigest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.NetcdfFile;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class CfTimePartTest {

    private Product product;
    private CfTimePart cfTimePart;
    private TestProfileReadContext ctx;

    @Before
    public void setUp() throws Exception {
        product = new Product("name", "type", 10, 10);
        cfTimePart = new CfTimePart();
    }

    @Test
    public void testDecode_testfile_1() throws Exception {
        ctx = new TestProfileReadContext("../../../test.nc");

        assertNull(product.getStartTime());
        assertNull(product.getEndTime());

        cfTimePart.decode(ctx, product);

        long expectedStartTime = ProductData.UTC.parse("2002-12-24 11:12:13", "yyyy-MM-dd HH:mm:ss").getAsDate().getTime();
        long expectedEndTime = ProductData.UTC.parse("2002-12-24 11:12:14", "yyyy-MM-dd HH:mm:ss").getAsDate().getTime();

        assertEquals(expectedStartTime, product.getStartTime().getAsDate().getTime());
        assertEquals(expectedEndTime, product.getEndTime().getAsDate().getTime());
    }

    @Test
    public void testDecode_testfile_2() throws Exception {
        ctx = new TestProfileReadContext("../../../test_2.nc");

        assertNull(product.getStartTime());
        assertNull(product.getEndTime());

        cfTimePart.decode(ctx, product);

        long expectedStartTime = ProductData.UTC.parse("2002-12-31 11:12:13", "yyyy-MM-dd HH:mm:ss").getAsDate().getTime();
        long expectedEndTime = ProductData.UTC.parse("2002-12-31 22:12:14", "yyyy-MM-dd HH:mm:ss").getAsDate().getTime();

        assertEquals(expectedStartTime, product.getStartTime().getAsDate().getTime());
        assertEquals(expectedEndTime, product.getEndTime().getAsDate().getTime());
    }

    @After
    public void tearDown() throws Exception {
        if (ctx != null) {
            ctx.netcdfFile.close();
        }
    }

    private static class TestProfileReadContext implements ProfileReadContext {

        private NetcdfFile netcdfFile;
        private String file;

        private TestProfileReadContext(String file) {
            this.file = file;
        }

        @Override
        public NetcdfFile getNetcdfFile() {
            try {
                netcdfFile = NetcdfFile.openInMemory(getClass().getResource(file).toURI());
                return netcdfFile;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void setRasterDigest(RasterDigest rasterDigest) {
        }

        @Override
        public RasterDigest getRasterDigest() {
            return null;
        }

        @Override
        public void setProperty(String name, Object value) {
        }

        @Override
        public Object getProperty(String name) {
            return null;
        }
    }
}
