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
package org.esa.snap.dataio.envisat;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.snap.core.util.io.CsvReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.List;

public class EnvisatConstantsTest extends TestCase {

    public EnvisatConstantsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(EnvisatConstantsTest.class);
    }

    /**
     * Tests whether the <code>MERIS_FR_L1B_PRODUCT_TYPE_NAME</code> field <code>MAGIC_STRING</code> conforms with the
     * database - or not
     */
    public void testMERIS_FR_L1B_TYPE() throws IOException {
        testProductTypeExistsInFirstLine("/products/MER_FR__1P.dd", EnvisatConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME);
    }

    /**
     * Tests whether the <code>MERIS_RR_L1B_PRODUCT_TYPE_NAME</code> field <code>MAGIC_STRING</code> conforms with the
     * database - or not
     */
    public void testMERIS_RR_L1B_TYPE() throws IOException {
        testProductTypeExistsInFirstLine("/products/MER_RR__1P.dd", EnvisatConstants.MERIS_RR_L1B_PRODUCT_TYPE_NAME);
    }

    /**
     * Tests whether the <code>MERIS_FR_L2_PRODUCT_TYPE_NAME</code> field <code>MAGIC_STRING</code> conforms with the
     * database - or not
     */
    public void testMERIS_FR_L2_TYPE() throws IOException {
        testProductTypeExistsInFirstLine("/products/MER_FR__2P.dd", EnvisatConstants.MERIS_FR_L2_PRODUCT_TYPE_NAME);
    }

    /**
     * Tests whether the <code>MERIS_RR_L2_PRODUCT_TYPE_NAME</code> field <code>MAGIC_STRING</code> conforms with the
     * database - or not
     */
    public void testMERIS_RR_L2_TYPE() throws IOException {
        testProductTypeExistsInFirstLine("/products/MER_RR__2P.dd", EnvisatConstants.MERIS_RR_L2_PRODUCT_TYPE_NAME);
    }

    /**
     * Tests whether the <code>AATSR_L1B_TOA_PRODUCT_TYPE_NAME</code> field <code>MAGIC_STRING</code> conforms with the
     * database - or not
     */
    public void testAATSR_FR_L1B_TYPE() throws IOException {
        testProductTypeExistsInFirstLine("/products/ATS_TOA_1P.dd", EnvisatConstants.AATSR_L1B_TOA_PRODUCT_TYPE_NAME);
    }

    /**
     * Tests whether the <code>AATSR_L2_NR_PRODUCT_TYPE_NAME</code> field <code>MAGIC_STRING</code> conforms with the
     * database - or not
     */
    public void testAATSR_FR_L2_TYPE() throws IOException {
        testProductTypeExistsInFirstLine("/products/ATS_NR__2P.dd", EnvisatConstants.AATSR_L2_NR_PRODUCT_TYPE_NAME);
    }


    public void testMERIS_L1_MDS_NAMES() throws IOException {
        existsInFile(EnvisatConstants.MERIS_L1B_BAND_NAMES, "/bands/MER_RR__1P.dd");
        existsInFile(EnvisatConstants.MERIS_L1B_BAND_NAMES, "/bands/MER_FR__1P.dd");
    }

    public void testMERIS_L2_MDS_NAMES() throws IOException {
        existsInFile(EnvisatConstants.MERIS_L2_BAND_NAMES, "/bands/MER_RR__2P.dd");
        existsInFile(EnvisatConstants.MERIS_L2_BAND_NAMES, "/bands/MER_FR__2P.dd");
    }

    public void testMERIS_ADS_NAMES() throws IOException {
        existsInFile(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES, "/bands/MER_RR__1P.dd");
        existsInFile(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES, "/bands/MER_FR__1P.dd");
        existsInFile(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES, "/bands/MER_RR__2P.dd");
        existsInFile(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES, "/bands/MER_FR__2P.dd");
    }

    public void testAATSR_L1_MDS_NAMES() throws IOException {
        existsInFile(EnvisatConstants.AATSR_L1B_BAND_NAMES, "/bands/ATS_TOA_1P.dd");
    }

    public void testAATSR_ADS_NAMES() throws IOException {
        existsInFile(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES, "/bands/ATS_TOA_1P.dd");
        existsInFile(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES, "/bands/ATS_NR__2P.dd");
    }

    public void testAATSR_L2_MDS_NAMES() throws IOException {
        existsInFile(EnvisatConstants.AATSR_L2_BAND_NAMES, "/bands/ATS_NR__2P.dd");
    }

    private static LineNumberReader createLineReader(String resourcePath) throws IOException {
        return new LineNumberReader(new InputStreamReader(DDDB.getDatabaseResource(resourcePath).openStream()));
    }

    private static void existsInFile(String[] expectedValues, String resourcePath) throws IOException {
        CsvReader csvReader = new CsvReader(createLineReader(resourcePath), new char[]{'|'}, true, "#");
        List<String[]> recordSet = csvReader.readStringRecords();
        try {
            for (String expectedValue : expectedValues) {
                assertTrue("expected '" + expectedValue + "' in " + resourcePath,
                           existsInRecordSet(expectedValue, recordSet, 0));
            }
        } finally {
            csvReader.close();
        }
    }

    private static boolean existsInRecordSet(String expectedValue, List<String[]> recordSet, int column) {
        for (String[] record : recordSet) {
            if (expectedValue.equals(record[column])) {
                return true;
            }
        }
        return false;
    }

    private static void testProductTypeExistsInFirstLine(String resourcePath, String productType) throws IOException {
        LineNumberReader reader = createLineReader(resourcePath);
        String line = reader.readLine();
        reader.close();
        assertNotNull(line);
        assertTrue(line.contains(productType));
    }
}
