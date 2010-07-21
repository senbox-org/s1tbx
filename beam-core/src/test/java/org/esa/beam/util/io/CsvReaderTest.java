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

package org.esa.beam.util.io;

import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.esa.beam.util.ArrayUtils;

public class CsvReaderTest extends TestCase {

    private CsvReader reader;
    private List<String[]> expectedRecords;

    public CsvReaderTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(CsvReaderTest.class);
    }

    @Override
    protected void setUp() {
        expectedRecords = new ArrayList<String[]>(6);
        expectedRecords.add(
                new String[]{"radiance_4",
                             "Radiance_MDS(4).3",
                             "*",
                             "Float",
                             "Linear_Scale",
                             "0.0",
                             "Scaling_Factor_GADS.8.4",
                             "*",
                             "*"});
        expectedRecords.add(
                new String[]{"radiance_5",
                             "Radiance_MDS(5).3",
                             "*",
                             "Float",
                             "Linear_Scale",
                             "0.0",
                             "Scaling_Factor_GADS.8.5",
                             "*",
                             "*"});
        expectedRecords.add(
                new String[]{"radiance_6",
                             "Radiance_MDS(6).3",
                             "*",
                             "Float",
                             "Linear_Scale",
                             "0.0",
                             "Scaling_Factor_GADS.8.6",
                             "*",
                             "*"});
        expectedRecords.add(
                new String[]{"radiance_7",
                             "Radiance_MDS(7).3",
                             "*",
                             "Float",
                             "Linear_Scale",
                             "0.0",
                             "Scaling_Factor_GADS.8.7",
                             "*",
                             "*"});

        Reader reader = new StringReader("radiance_4   |Radiance_MDS(4).3 |*   |Float|Linear_Scale|0.0|Scaling_Factor_GADS.8.4 |*|*\n" +
                                         "radiance_5   |Radiance_MDS(5).3 |*   |Float|Linear_Scale|0.0|Scaling_Factor_GADS.8.5 |*|*\n" +
                                         "radiance_6   |Radiance_MDS(6).3 |*   |Float|Linear_Scale|0.0|Scaling_Factor_GADS.8.6 |*|*\n" +
                                         "radiance_7   |Radiance_MDS(7).3 |*   |Float|Linear_Scale|0.0|Scaling_Factor_GADS.8.7 |*|*\n");
        this.reader = new CsvReader(reader, new char[]{'|'});
    }

    public void testGetSeparators() {
        Reader reader = new StringReader("testreader");
        CsvReader csvReader;
        char[] seperator;

        seperator = new char[]{'|'};
        csvReader = new CsvReader(reader, seperator);
        assertEquals(seperator, csvReader.getSeparators());

        seperator = new char[]{'|', ','};
        csvReader = new CsvReader(reader, seperator);
        assertEquals(seperator, csvReader.getSeparators());
    }

    public void testReadAllRecords() {
        List<String[]> actualVector = null;
        try {
            actualVector = reader.readStringRecords();
        } catch (java.io.IOException e) {
            fail("no java.io.IOException expected");
        }

        assertEquals(actualVector.size(), expectedRecords.size());
        for (int i = 0; i < actualVector.size(); i++) {
            ArrayUtils.equalArrays(actualVector.get(i), expectedRecords.get(i));
        }
    }

    public void testReadRecord() {

        for (String[] expectedRecord : expectedRecords) {
            try {
                assertEquals(true, ArrayUtils.equalArrays(expectedRecord, reader.readRecord()));
            } catch (IOException e) {
                fail("no java.io.IOException expected");
            }
        }

        try {
            assertNull(reader.readRecord());
        } catch (java.io.IOException ex) {
            fail("no java.io.IOException expected");
        }
    }
}
