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

package org.esa.beam.dataio.envisat;


import org.esa.beam.framework.datamodel.ProductData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ProductFileTest {

    ProductFile productFile;

    @Before
    public void setUp() throws Exception {
        File file = new File(getClass().getResource(
                "ATS_TOA_1PRMAP20050504_080932_000000482037_00020_16607_0001.N1").toURI());
        String productType = ProductFile.getProductType(file);
        assertEquals("ATS_TOA_1P", productType);

        productFile = ProductFile.open(file);
        assertNotNull(productFile);
    }

    @After
    public void tearDown() throws Exception {
        productFile.close();
    }

    @Test
    public void testRecordTime() throws IOException {

        final String[] validDatasetNames = productFile.getValidDatasetNames(EnvisatConstants.DS_TYPE_MEASUREMENT);
        assertEquals(18, validDatasetNames.length);


        ProductData.UTC utc1 = productFile.getRecordTime(validDatasetNames[0], "dsr_time", 0);
        assertEquals("04-MAY-2005 08:09:32.862224", utc1.format());

        ProductData.UTC utc2 = productFile.getRecordTime(validDatasetNames[0], "dsr_time", 1);
        assertEquals("04-MAY-2005 08:09:33.012223", utc2.format());

        ProductData.UTC utc3 = productFile.getRecordTime(validDatasetNames[0], "dsr_time", 2);
        assertEquals("04-MAY-2005 08:09:33.162223", utc3.format());

        assertEquals(1.7361E-6, utc2.getMJD()-utc1.getMJD(), 1e-10);
        assertEquals(1.7361E-6, utc3.getMJD()-utc2.getMJD(), 1e-10);
    }

    @Test
    public void testAllRecordTimes() throws IOException {

        final String[] validDatasetNames = productFile.getValidDatasetNames(EnvisatConstants.DS_TYPE_MEASUREMENT);
        assertEquals(18, validDatasetNames.length);

        int numRecs = productFile.getSceneRasterHeight();
        assertEquals(320, numRecs);

        ProductData.UTC[] recordTimes = productFile.getAllRecordTimes();
        assertEquals(numRecs, recordTimes.length);

        assertEquals("04-MAY-2005 08:09:32.862224", recordTimes[0].format());
        assertEquals("04-MAY-2005 08:09:33.012223", recordTimes[1].format());
        assertEquals("04-MAY-2005 08:09:33.162223", recordTimes[2].format());

        assertEquals("04-MAY-2005 08:10:20.412223", recordTimes[numRecs-3].format());
        assertEquals("04-MAY-2005 08:10:20.562223", recordTimes[numRecs-2].format());
        assertEquals("04-MAY-2005 08:10:20.712223", recordTimes[numRecs-1].format());
    }

}