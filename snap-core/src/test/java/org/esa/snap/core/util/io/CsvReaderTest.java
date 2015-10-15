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

package org.esa.snap.core.util.io;

import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.*;

public class CsvReaderTest {

    public static final String CSV_DATA =
            "radiance_4   |Radiance_MDS(4).3 |*   |Float|Linear_Scale|0.0|Scaling_Factor_GADS.8.4 |*|*\n" +
                    "radiance_5   |Radiance_MDS(5).3 |*   |Float|Linear_Scale|0.0|Scaling_Factor_GADS.8.5 |*|*\n" +
                    "radiance_6   |Radiance_MDS(6).3 |*   |Float|Linear_Scale|0.0|Scaling_Factor_GADS.8.6 |*|\n" +
                    "radiance_7   |Radiance_MDS(7).3 |*   |Float|Linear_Scale|0.0|Scaling_Factor_GADS.8.7 ||*\n";

    private static final String[][] EXPECTED_RECORDS = new String[][]{
            new String[]{
                    "radiance_4",
                    "Radiance_MDS(4).3",
                    "*",
                    "Float",
                    "Linear_Scale",
                    "0.0",
                    "Scaling_Factor_GADS.8.4",
                    "*",
                    "*"
            },
            {
                    "radiance_5",
                    "Radiance_MDS(5).3",
                    "*",
                    "Float",
                    "Linear_Scale",
                    "0.0",
                    "Scaling_Factor_GADS.8.5",
                    "*",
                    "*"
            },
            {
                    "radiance_6",
                    "Radiance_MDS(6).3",
                    "*",
                    "Float",
                    "Linear_Scale",
                    "0.0",
                    "Scaling_Factor_GADS.8.6",
                    "*",
                    ""
            },
            {
                    "radiance_7",
                    "Radiance_MDS(7).3",
                    "*",
                    "Float",
                    "Linear_Scale",
                    "0.0",
                    "Scaling_Factor_GADS.8.7",
                    "",
                    "*"
            }
    };

    @Test
    public void testGetSeparators() {
        Reader reader = new StringReader("mem");

        assertArrayEquals(new char[]{'|'},
                          new CsvReader(reader, new char[]{'|'}).getSeparators());

        assertArrayEquals(new char[]{'|', ','},
                          new CsvReader(reader, new char[]{'|', ','}).getSeparators());
    }

    @Test
    public void testReadAllRecords() throws IOException {
        CsvReader reader = createReader(CSV_DATA, '|');
        List<String[]> records = reader.readStringRecords();
        assertEquals(EXPECTED_RECORDS.length, records.size());
        for (int i = 0; i < records.size(); i++) {
            assertArrayEquals(EXPECTED_RECORDS[i], records.get(i));
        }
    }

    @Test
    public void testReadRecord() throws IOException {
        CsvReader reader = createReader(CSV_DATA, '|');
        for (String[] expectedRecord : EXPECTED_RECORDS) {
            assertArrayEquals(expectedRecord, reader.readRecord());
        }
        assertNull(reader.readRecord());
    }

    @Test
    public void testMultipleSeparators() throws Exception {
        CsvReader reader = createReader("a$b|c\nd$$\n|$i", '|', '$');
        List<String[]> records = reader.readStringRecords();
        assertEquals(3, records.size());
        assertArrayEquals(new String[]{"a", "b", "c"}, records.get(0));
        assertArrayEquals(new String[]{"d", "", ""}, records.get(1));
        assertArrayEquals(new String[]{"", "", "i"}, records.get(2));
    }

    @Test
    public void testSemicolonSeparator() throws Exception {
        CsvReader reader = createReader("a; b;c\nd;;\n;; i \n", ';');
        List<String[]> records = reader.readStringRecords();
        assertEquals(3, records.size());
        assertArrayEquals(new String[]{"a", "b", "c"}, records.get(0));
        assertArrayEquals(new String[]{"d", "", ""}, records.get(1));
        assertArrayEquals(new String[]{"", "", "i"}, records.get(2));
    }

    @Test
    public void testTabSeparator() throws Exception {
        CsvReader reader = createReader("a\tb\tc\nd\t\t\n\t\ti", '\t');
        List<String[]> records = reader.readStringRecords();
        assertEquals(3, records.size());
        assertArrayEquals(new String[]{"a", "b", "c"}, records.get(0));
        assertArrayEquals(new String[]{"d", "", ""}, records.get(1));
        assertArrayEquals(new String[]{"", "", "i"}, records.get(2));
    }

    @Test
    public void testSpaceSeparator() throws Exception {
        CsvReader reader = createReader(" a b  c\nd  \n  i", ' ');
        List<String[]> records = reader.readStringRecords();
        assertEquals(3, records.size());
        assertArrayEquals(new String[]{"a", "b", "c"}, records.get(0));
        assertArrayEquals(new String[]{"d"}, records.get(1));
        assertArrayEquals(new String[]{"i"}, records.get(2));
    }

    @Test
    public void testWhitespaceSeparators() throws Exception {
        String csv = "\n// Holla!\n \t  \n a\tb  c\n\nd  \n\t i \n\n\n";
        CsvReader reader = createReader(csv, false, null, '\t', ' ');
        List<String[]> records = reader.readStringRecords();
        assertEquals(9, records.size());
        assertArrayEquals(new String[]{}, records.get(0));
        assertArrayEquals(new String[]{"//", "Holla!"}, records.get(1));
        assertArrayEquals(new String[]{}, records.get(2));
        assertArrayEquals(new String[]{"a", "b", "c"}, records.get(3));
        assertArrayEquals(new String[]{}, records.get(4));
        assertArrayEquals(new String[]{"d"}, records.get(5));
        assertArrayEquals(new String[]{"i"}, records.get(6));
        assertArrayEquals(new String[]{}, records.get(7));
        assertArrayEquals(new String[]{}, records.get(8));
    }

    @Test
    public void testWhitespaceSeparatorIgnoringComments() throws Exception {
        String csv = "\n// Holla!\n \t  \n a\tb  c\n\nd  \n\t i \n\n\n";
        CsvReader reader = createReader(csv, false, "//", '\t', ' ');
        List<String[]> records = reader.readStringRecords();
        assertEquals(8, records.size());
        assertArrayEquals(new String[]{}, records.get(0));
        assertArrayEquals(new String[]{}, records.get(1));
        assertArrayEquals(new String[]{"a", "b", "c"}, records.get(2));
        assertArrayEquals(new String[]{}, records.get(3));
        assertArrayEquals(new String[]{"d"}, records.get(4));
        assertArrayEquals(new String[]{"i"}, records.get(5));
        assertArrayEquals(new String[]{}, records.get(6));
        assertArrayEquals(new String[]{}, records.get(7));
    }

    @Test
    public void testWhitespaceSeparatorsIgnoringEmptyLines() throws Exception {
        String csv = "\n// Holla!\n \t  \n a\tb  c\n\nd  \n\t i \n\n\n";
        CsvReader reader = createReader(csv, true, null, '\t', ' ');
        List<String[]> records = reader.readStringRecords();
        assertEquals(4, records.size());
        assertArrayEquals(new String[]{"//", "Holla!"}, records.get(0));
        assertArrayEquals(new String[]{"a", "b", "c"}, records.get(1));
        assertArrayEquals(new String[]{"d"}, records.get(2));
        assertArrayEquals(new String[]{"i"}, records.get(3));
    }

    @Test
    public void testWhitespaceSeparatorsIgnoringEmptyLinesAndComments() throws Exception {
        String csv = "\n// Holla!\n \t  \n a\tb  c\n\nd  \n\t i \n\n\n";
        CsvReader reader = createReader(csv, true, "//", '\t', ' ');
        List<String[]> records = reader.readStringRecords();
        assertEquals(3, records.size());
        assertArrayEquals(new String[]{"a", "b", "c"}, records.get(0));
        assertArrayEquals(new String[]{"d"}, records.get(1));
        assertArrayEquals(new String[]{"i"}, records.get(2));
    }

    private static CsvReader createReader(String csv, char... separators) {
        Reader reader = new StringReader(csv);
        return new CsvReader(reader, separators);
    }

    private static CsvReader createReader(String csv,boolean ignoreEmptyLines, String commentPrefix, char... separators) {
        Reader reader = new StringReader(csv);
        return new CsvReader(reader, separators, ignoreEmptyLines, commentPrefix);
    }

}
