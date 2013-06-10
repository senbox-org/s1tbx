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

package org.esa.beam.framework.dataop.barithm;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class MoreFuncsTest {

    @Test
    public void testMJDSymbol() throws Exception {
        final Product product = new Product("name", "type", 10, 10);
        ProductData.UTC startTime = ProductData.UTC.parse("01-Jan-2010 00:00:00");
        ProductData.UTC endTime = ProductData.UTC.parse("01-Jan-2010 10:00:00");

        product.setStartTime(startTime);
        product.setEndTime(endTime);
        MoreFuncs.MJD mjd = new MoreFuncs.MJD(product);

        RasterDataEvalEnv rasterEnv = new RasterDataEvalEnv(0, 0, 10, 10);
        assertEquals(startTime.getMJD(), mjd.evalD(rasterEnv), 1E-6);

        rasterEnv = new RasterDataEvalEnv(9, 0, 10, 10);
        assertEquals(startTime.getMJD(), mjd.evalD(rasterEnv), 1E-6);

        rasterEnv = new RasterDataEvalEnv(0, 9, 10, 10);
        assertEquals(endTime.getMJD(), mjd.evalD(rasterEnv), 1E-6);

        rasterEnv = new RasterDataEvalEnv(9, 9, 10, 10);
        assertEquals(endTime.getMJD(), mjd.evalD(rasterEnv), 1E-6);

        rasterEnv = new RasterDataEvalEnv(-1, 4, 10, 10);
        double centralTime = (startTime.getMJD() * 5 + endTime.getMJD() * 4) / 9;
        assertEquals(centralTime, mjd.evalD(rasterEnv), 1E-6);
    }

    @Test
    public void testMJDSymbol_no_time() throws Exception {
        final Product product = new Product("name", "type", 10, 10);
        MoreFuncs.MJD mjd = new MoreFuncs.MJD(product);

        assertTrue(Double.isNaN(mjd.evalD(new RasterDataEvalEnv(-1, -1, -1, -1))));
    }
}
